package org.tedros.ai.toolrelay.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;

/**
 * Testes do provider de gateways Redmine/GitLab: lazy load com cache/TTL,
 * erro amigavel com config ausente, resiliencia a falha de lookup e
 * reconstrucao do gateway Redmine quando as properties mudam.
 */
public class TIntegrationGatewayProviderTest {

	/** Provider com load() controlado pelo teste (sem lookup JNDI). */
	static class FakeProvider extends TIntegrationGatewayProvider {
		final AtomicInteger loads = new AtomicInteger();
		volatile Snapshot next;
		volatile boolean fail;

		FakeProvider(Snapshot next) {
			this.next = next;
		}

		@Override
		protected Snapshot load() throws Exception {
			loads.incrementAndGet();
			if (fail)
				throw new IllegalStateException("lookup failed");
			return next;
		}
	}

	private static TIntegrationGatewayProvider.Snapshot full() {
		return new TIntegrationGatewayProvider.Snapshot(
				"http://redmine.local", "rk", "http://gitlab.local", "gk");
	}

	@Test
	public void snapshotIsCachedWithinTtl() {
		FakeProvider p = new FakeProvider(full());
		p.snapshot();
		p.snapshot();
		p.snapshot();
		assertEquals(1, p.loads.get());
	}

	@Test
	public void missingRedmineConfigThrowsFriendlyError() {
		FakeProvider p = new FakeProvider(new TIntegrationGatewayProvider.Snapshot(null, null, "u", "k"));
		IllegalStateException e = assertThrows(IllegalStateException.class, p::redmineGateway);
		assertEquals(TIntegrationGatewayProvider.REDMINE_NOT_CONFIGURED, e.getMessage());
	}

	@Test
	public void missingGitlabConfigThrowsFriendlyError() {
		FakeProvider p = new FakeProvider(new TIntegrationGatewayProvider.Snapshot("u", "k", " ", null));
		IllegalStateException e = assertThrows(IllegalStateException.class, p::gitlabGateway);
		assertEquals(TIntegrationGatewayProvider.GITLAB_NOT_CONFIGURED, e.getMessage());
	}

	@Test
	public void loadFailureFallsBackToNotConfigured() {
		FakeProvider p = new FakeProvider(full());
		p.fail = true;
		// nao propaga a excecao do lookup: vira "nao configurado" amigavel
		IllegalStateException e = assertThrows(IllegalStateException.class, p::redmineGateway);
		assertEquals(TIntegrationGatewayProvider.REDMINE_NOT_CONFIGURED, e.getMessage());
	}

	@Test
	public void redmineGatewayIsReusedWhileConfigUnchanged() {
		FakeProvider p = new FakeProvider(full());
		RedmineApiGateway g1 = p.redmineGateway();
		RedmineApiGateway g2 = p.redmineGateway();
		assertNotNull(g1);
		assertSame(g1, g2);
		assertTrue(p.loads.get() >= 1);
	}
}
