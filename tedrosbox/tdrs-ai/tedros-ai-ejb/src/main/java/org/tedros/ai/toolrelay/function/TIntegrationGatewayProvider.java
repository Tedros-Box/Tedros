package org.tedros.ai.toolrelay.function;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.tedros.core.support.TDomainPropertieSupport;
import org.tedros.integration.gitlab.gateway.GitLabGateway;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;
import org.tedros.it.tools.domain.ItSupportPropertie;
import org.tedros.server.util.TLoggerUtil;
import org.tedros.server.util.TServiceLocator;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Resolve e cacheia (lazy, TTL) as properties de integracao Redmine/GitLab e
 * entrega os gateways do {@code tedros-integration} as tools de backend.
 * <p>
 * O cache tem duas camadas: as URLs ({@code redmine.url}/{@code gitlab.url},
 * {@code TType.SYSTEM}) ficam em um snapshot global; as API keys
 * ({@code redmine.api.key}/{@code gitlab.api.key}, {@code TType.USER}) e os
 * gateways construidos com elas ficam em entradas por usuario
 * ({@code ConcurrentHashMap<userId, UserEntry>}), ambos com TTL de 3 min.
 * Cada usuario recebe seu proprio {@link RedmineApiGateway} e
 * {@link GitLabGateway} (via {@link GitLabGateway#create(String, String)},
 * nunca o singleton {@code getInstance}).
 * <p>
 * Diferente do padrao do FE (bloco {@code static} que lanca excecao e
 * derrubaria o deploy), a resolucao aqui e adiada ate a primeira tool que
 * precisar do gateway: config ausente vira {@link IllegalStateException} com
 * mensagem amigavel, que a tool converte em resultado de erro para o LLM —
 * distinguindo servidor nao configurado (URL, problema de admin) de usuario
 * sem key (o agente orienta o usuario a configurar a property em
 * Preferencias/User Properties).
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TIntegrationGatewayProvider {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TIntegrationGatewayProvider.class);

	static final long CACHE_TTL_MILLIS = 3 * 60 * 1000L;

	/** Entradas de usuario ociosas ha mais que isto sao removidas na varredura. */
	static final long USER_ENTRY_EVICT_MILLIS = CACHE_TTL_MILLIS * 4;

	public static final String REDMINE_NOT_CONFIGURED =
			"Redmine integration is not configured on the server (system property 'redmine.url').";
	public static final String GITLAB_NOT_CONFIGURED =
			"GitLab integration is not configured on the server (system property 'gitlab.url').";
	public static final String REDMINE_USER_KEY_NOT_CONFIGURED =
			"The user has not configured their Redmine API key (user property 'redmine.api.key'). "
			+ "Inform the user that they must first configure this property in the system preferences "
			+ "(User Properties) before using Redmine tools.";
	public static final String GITLAB_USER_KEY_NOT_CONFIGURED =
			"The user has not configured their GitLab API key (user property 'gitlab.api.key'). "
			+ "Inform the user that they must first configure this property in the system preferences "
			+ "(User Properties) before using GitLab tools.";

	/** URLs de sistema (TType.SYSTEM); campos nulos = nao configurado. */
	static class SystemSnapshot {
		final String redmineUrl;
		final String gitlabUrl;

		SystemSnapshot(String redmineUrl, String gitlabUrl) {
			this.redmineUrl = redmineUrl;
			this.gitlabUrl = gitlabUrl;
		}

		boolean hasRedmineUrl() {
			return notBlank(redmineUrl);
		}

		boolean hasGitlabUrl() {
			return notBlank(gitlabUrl);
		}
	}

	/** Keys (TType.USER) e gateways construidos de um usuario. */
	static class UserEntry {
		final String redmineKey;
		final String gitlabKey;
		final long loadedAt;

		// gateways lazy, reconstruidos quando a url vigente muda; key nova
		// implica entrada nova (sem gateway herdado), forcando o rebuild
		volatile RedmineApiGateway redmineGateway;
		volatile String redmineGatewayUrl;
		volatile GitLabGateway gitlabGateway;
		volatile String gitlabGatewayUrl;

		UserEntry(String redmineKey, String gitlabKey) {
			this(redmineKey, gitlabKey, System.currentTimeMillis());
		}

		UserEntry(String redmineKey, String gitlabKey, long loadedAt) {
			this.redmineKey = redmineKey;
			this.gitlabKey = gitlabKey;
			this.loadedAt = loadedAt;
		}

		boolean hasRedmineKey() {
			return notBlank(redmineKey);
		}

		boolean hasGitlabKey() {
			return notBlank(gitlabKey);
		}

		boolean expired(long now) {
			return (now - loadedAt) >= CACHE_TTL_MILLIS;
		}
	}

	private volatile SystemSnapshot system;
	private volatile long systemLoadedAt;
	private final ReentrantLock refreshLock = new ReentrantLock();

	private final ConcurrentHashMap<Long, UserEntry> userEntries = new ConcurrentHashMap<>();

	/**
	 * Gateway Redmine do usuario da conversa, ou {@link IllegalStateException}
	 * com mensagem amigavel se a URL de sistema ou a key do usuario nao
	 * estiverem definidas.
	 */
	public RedmineApiGateway redmineGateway(TAiToolContext ctx) {
		SystemSnapshot sys = systemSnapshot();
		if (!sys.hasRedmineUrl())
			throw new IllegalStateException(REDMINE_NOT_CONFIGURED);
		UserEntry entry = userEntry(ctx.getUser().getId());
		if (!entry.hasRedmineKey())
			throw new IllegalStateException(REDMINE_USER_KEY_NOT_CONFIGURED);
		RedmineApiGateway gw = entry.redmineGateway;
		if (gw != null && sys.redmineUrl.equals(entry.redmineGatewayUrl))
			return gw;
		synchronized (entry) {
			if (entry.redmineGateway == null || !sys.redmineUrl.equals(entry.redmineGatewayUrl)) {
				entry.redmineGateway = new RedmineApiGateway(sys.redmineUrl, entry.redmineKey);
				entry.redmineGatewayUrl = sys.redmineUrl;
			}
			return entry.redmineGateway;
		}
	}

	/**
	 * Gateway GitLab do usuario da conversa, ou {@link IllegalStateException}
	 * com mensagem amigavel se a URL de sistema ou a key do usuario nao
	 * estiverem definidas.
	 */
	public GitLabGateway gitlabGateway(TAiToolContext ctx) {
		SystemSnapshot sys = systemSnapshot();
		if (!sys.hasGitlabUrl())
			throw new IllegalStateException(GITLAB_NOT_CONFIGURED);
		UserEntry entry = userEntry(ctx.getUser().getId());
		if (!entry.hasGitlabKey())
			throw new IllegalStateException(GITLAB_USER_KEY_NOT_CONFIGURED);
		GitLabGateway gw = entry.gitlabGateway;
		if (gw != null && sys.gitlabUrl.equals(entry.gitlabGatewayUrl))
			return gw;
		synchronized (entry) {
			if (entry.gitlabGateway == null || !sys.gitlabUrl.equals(entry.gitlabGatewayUrl)) {
				entry.gitlabGateway = GitLabGateway.create(sys.gitlabUrl, entry.gitlabKey);
				entry.gitlabGatewayUrl = sys.gitlabUrl;
			}
			return entry.gitlabGateway;
		}
	}

	SystemSnapshot systemSnapshot() {
		SystemSnapshot snap = system;
		if (snap != null && (now() - systemLoadedAt) < CACHE_TTL_MILLIS)
			return snap;

		refreshLock.lock();
		try {
			snap = system;
			if (snap != null && (now() - systemLoadedAt) < CACHE_TTL_MILLIS)
				return snap;
			try {
				snap = loadSystem();
			} catch (Exception e) {
				LOGGER.error("Error loading Redmine/GitLab integration system properties", e);
				// mantem o ultimo snapshot valido; sem ele, nada configurado
				snap = (system != null) ? system : new SystemSnapshot(null, null);
			}
			system = snap;
			systemLoadedAt = now();
			return snap;
		} finally {
			refreshLock.unlock();
		}
	}

	UserEntry userEntry(Long userId) {
		long now = now();
		UserEntry entry = userEntries.get(userId);
		if (entry != null && !entry.expired(now))
			return entry;

		purgeIdleEntries(now, userId);

		// compute serializa o refresh por usuario sem contencao global
		return userEntries.compute(userId, (id, old) -> {
			long t = now();
			if (old != null && !old.expired(t))
				return old;
			UserEntry fresh;
			try {
				fresh = loadUser(id);
			} catch (Exception e) {
				LOGGER.error("Error loading Redmine/GitLab API keys for user " + id, e);
				// mantem a ultima entrada valida (renovando o TTL); sem ela,
				// trata como usuario sem key configurada
				fresh = (old != null)
						? new UserEntry(old.redmineKey, old.gitlabKey, t)
						: new UserEntry(null, null, t);
			}
			if (old != null) {
				// keys inalteradas: herda os gateways ja construidos
				if (old.redmineKey != null && old.redmineKey.equals(fresh.redmineKey)) {
					fresh.redmineGateway = old.redmineGateway;
					fresh.redmineGatewayUrl = old.redmineGatewayUrl;
				}
				if (old.gitlabKey != null && old.gitlabKey.equals(fresh.gitlabKey)) {
					fresh.gitlabGateway = old.gitlabGateway;
					fresh.gitlabGatewayUrl = old.gitlabGatewayUrl;
				}
			}
			return fresh;
		});
	}

	/**
	 * Varredura oportunista: remove entradas de usuarios ociosos ha mais de
	 * {@link #USER_ENTRY_EVICT_MILLIS} (mantem as apenas expiradas, que ainda
	 * servem de fallback se o proximo load falhar), exceto a do usuario sendo
	 * carregado.
	 */
	private void purgeIdleEntries(long now, Long loadingUserId) {
		userEntries.entrySet().removeIf(e -> !e.getKey().equals(loadingUserId)
				&& (now - e.getValue().loadedAt) >= USER_ENTRY_EVICT_MILLIS);
	}

	/** Protegido para permitir override nos testes de unidade. */
	protected SystemSnapshot loadSystem() throws Exception {

		try (TServiceLocator serv = TServiceLocator.getInstance()) {
			TDomainPropertieSupport support = serv.lookupWithRetry(TDomainPropertieSupport.JNDI_NAME);
			return new SystemSnapshot(
					trim(support.getSystemPropertyValue(ItSupportPropertie.REDMINE_URL.getValue())),
					trim(support.getSystemPropertyValue(ItSupportPropertie.GITLAB_URL.getValue())));
		}
	}

	/** Protegido para permitir override nos testes de unidade. */
	protected UserEntry loadUser(Long userId) throws Exception {

		try (TServiceLocator serv = TServiceLocator.getInstance()) {
			TDomainPropertieSupport support = serv.lookupWithRetry(TDomainPropertieSupport.JNDI_NAME);
			return new UserEntry(
					trim(support.getUserPropertyValue(ItSupportPropertie.REDMINE_KEY.getValue(), userId)),
					trim(support.getUserPropertyValue(ItSupportPropertie.GITLAB_KEY.getValue(), userId)));
		}
	}

	/** Protegido para permitir clock controlado nos testes de unidade. */
	protected long now() {
		return System.currentTimeMillis();
	}

	private static boolean notBlank(String v) {
		return v != null && !v.isBlank();
	}

	private static String trim(String v) {
		return v != null ? v.trim() : null;
	}
}
