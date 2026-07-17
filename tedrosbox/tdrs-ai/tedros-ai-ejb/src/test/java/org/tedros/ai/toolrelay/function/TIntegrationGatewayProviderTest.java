package org.tedros.ai.toolrelay.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.tedros.core.security.model.TUser;
import org.tedros.integration.gitlab.gateway.GitLabGateway;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;

/**
 * Testes do provider de gateways Redmine/GitLab: cache do snapshot de sistema
 * (URLs) e das entradas por usuario (keys + gateways) com TTL, erros amigaveis
 * distinguindo servidor nao configurado de usuario sem key, resiliencia a
 * falha de lookup e reuso/rebuild dos gateways quando url/key mudam.
 */
public class TIntegrationGatewayProviderTest {

	/** Provider com loads e clock controlados pelo teste (sem lookup JNDI). */
	static class FakeProvider extends TIntegrationGatewayProvider {
		final AtomicInteger systemLoads = new AtomicInteger();
		final AtomicInteger userLoads = new AtomicInteger();
		final AtomicLong clock = new AtomicLong(1_000_000L);

		volatile String redmineUrl = "http://redmine.local";
		volatile String gitlabUrl = "http://gitlab.local";
		final Map<Long, String> redmineKeys = new ConcurrentHashMap<>();
		final Map<Long, String> gitlabKeys = new ConcurrentHashMap<>();
		volatile boolean failSystem;
		volatile boolean failUser;

		@Override
		protected SystemSnapshot loadSystem() throws Exception {
			systemLoads.incrementAndGet();
			if (failSystem)
				throw new IllegalStateException("lookup failed");
			return new SystemSnapshot(redmineUrl, gitlabUrl);
		}

		@Override
		protected UserEntry loadUser(Long userId) throws Exception {
			userLoads.incrementAndGet();
			if (failUser)
				throw new IllegalStateException("lookup failed");
			return new UserEntry(redmineKeys.get(userId), gitlabKeys.get(userId), clock.get());
		}

		@Override
		protected long now() {
			return clock.get();
		}

		void advance(long millis) {
			clock.addAndGet(millis);
		}

		void expireCaches() {
			advance(TIntegrationGatewayProvider.CACHE_TTL_MILLIS + 1);
		}
	}

	private static FakeProvider fullyConfigured() {
		FakeProvider p = new FakeProvider();
		p.redmineKeys.put(1L, "rk1");
		p.gitlabKeys.put(1L, "gk1");
		p.redmineKeys.put(2L, "rk2");
		p.gitlabKeys.put(2L, "gk2");
		return p;
	}

	private static TAiToolContext ctx(long userId) {
		TUser user = new TUser();
		user.setId(userId);
		return new TAiToolContext(user, null);
	}

	// ------------------------------------------------------------- cache/TTL

	@Test
	public void systemSnapshotIsCachedWithinTtl() {
		FakeProvider p = fullyConfigured();
		p.redmineGateway(ctx(1L));
		p.redmineGateway(ctx(1L));
		p.gitlabGateway(ctx(1L));
		assertEquals(1, p.systemLoads.get());
	}

	@Test
	public void userEntryIsCachedWithinTtl() {
		FakeProvider p = fullyConfigured();
		p.redmineGateway(ctx(1L));
		p.gitlabGateway(ctx(1L));
		p.redmineGateway(ctx(1L));
		assertEquals(1, p.userLoads.get());
	}

	@Test
	public void expiredCachesAreReloaded() {
		FakeProvider p = fullyConfigured();
		p.redmineGateway(ctx(1L));
		p.expireCaches();
		p.redmineGateway(ctx(1L));
		assertEquals(2, p.systemLoads.get());
		assertEquals(2, p.userLoads.get());
	}

	// ------------------------------------------------------- key por usuario

	@Test
	public void distinctUsersGetDistinctGateways() {
		FakeProvider p = fullyConfigured();
		RedmineApiGateway r1 = p.redmineGateway(ctx(1L));
		RedmineApiGateway r2 = p.redmineGateway(ctx(2L));
		GitLabGateway g1 = p.gitlabGateway(ctx(1L));
		GitLabGateway g2 = p.gitlabGateway(ctx(2L));
		assertNotNull(r1);
		assertNotNull(g1);
		assertNotSame(r1, r2);
		assertNotSame(g1, g2);
		assertEquals(2, p.userLoads.get());
	}

	// ------------------------------------------------------- erros amigaveis

	@Test
	public void missingRedmineUrlThrowsServerNotConfigured() {
		FakeProvider p = fullyConfigured();
		p.redmineUrl = null;
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> p.redmineGateway(ctx(1L)));
		assertEquals(TIntegrationGatewayProvider.REDMINE_NOT_CONFIGURED, e.getMessage());
	}

	@Test
	public void missingGitlabUrlThrowsServerNotConfigured() {
		FakeProvider p = fullyConfigured();
		p.gitlabUrl = " ";
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> p.gitlabGateway(ctx(1L)));
		assertEquals(TIntegrationGatewayProvider.GITLAB_NOT_CONFIGURED, e.getMessage());
	}

	@Test
	public void missingUserRedmineKeyThrowsUserKeyNotConfigured() {
		FakeProvider p = fullyConfigured();
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> p.redmineGateway(ctx(99L)));
		assertEquals(TIntegrationGatewayProvider.REDMINE_USER_KEY_NOT_CONFIGURED, e.getMessage());
	}

	@Test
	public void missingUserGitlabKeyThrowsUserKeyNotConfigured() {
		FakeProvider p = fullyConfigured();
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> p.gitlabGateway(ctx(99L)));
		assertEquals(TIntegrationGatewayProvider.GITLAB_USER_KEY_NOT_CONFIGURED, e.getMessage());
	}

	// ------------------------------------------------------------ resiliencia

	@Test
	public void systemLoadFailureFallsBackToNotConfigured() {
		FakeProvider p = fullyConfigured();
		p.failSystem = true;
		// nao propaga a excecao do lookup: vira "nao configurado" amigavel
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> p.redmineGateway(ctx(1L)));
		assertEquals(TIntegrationGatewayProvider.REDMINE_NOT_CONFIGURED, e.getMessage());
	}

	@Test
	public void userLoadFailureFallsBackToKeyNotConfigured() {
		FakeProvider p = fullyConfigured();
		p.failUser = true;
		IllegalStateException e = assertThrows(IllegalStateException.class,
				() -> p.redmineGateway(ctx(1L)));
		assertEquals(TIntegrationGatewayProvider.REDMINE_USER_KEY_NOT_CONFIGURED, e.getMessage());
	}

	@Test
	public void loadFailureKeepsLastValidSnapshotAndEntry() {
		FakeProvider p = fullyConfigured();
		RedmineApiGateway before = p.redmineGateway(ctx(1L));
		p.expireCaches();
		p.failSystem = true;
		p.failUser = true;
		// mantem o ultimo snapshot/entry validos: o gateway continua o mesmo
		assertSame(before, p.redmineGateway(ctx(1L)));
	}

	// ------------------------------------------------------- reuso e rebuild

	@Test
	public void gatewaysAreReusedWhileUrlAndKeyUnchanged() {
		FakeProvider p = fullyConfigured();
		RedmineApiGateway r1 = p.redmineGateway(ctx(1L));
		GitLabGateway g1 = p.gitlabGateway(ctx(1L));
		p.expireCaches();
		assertSame(r1, p.redmineGateway(ctx(1L)));
		assertSame(g1, p.gitlabGateway(ctx(1L)));
	}

	@Test
	public void gatewaysAreRebuiltWhenUrlChanges() {
		FakeProvider p = fullyConfigured();
		RedmineApiGateway r1 = p.redmineGateway(ctx(1L));
		GitLabGateway g1 = p.gitlabGateway(ctx(1L));
		p.expireCaches();
		p.redmineUrl = "http://redmine.other";
		p.gitlabUrl = "http://gitlab.other";
		assertNotSame(r1, p.redmineGateway(ctx(1L)));
		assertNotSame(g1, p.gitlabGateway(ctx(1L)));
	}

	@Test
	public void gatewaysAreRebuiltWhenUserKeyChanges() {
		FakeProvider p = fullyConfigured();
		RedmineApiGateway r1 = p.redmineGateway(ctx(1L));
		GitLabGateway g1 = p.gitlabGateway(ctx(1L));
		p.expireCaches();
		p.redmineKeys.put(1L, "rk1-new");
		p.gitlabKeys.put(1L, "gk1-new");
		assertNotSame(r1, p.redmineGateway(ctx(1L)));
		assertNotSame(g1, p.gitlabGateway(ctx(1L)));
	}
}
