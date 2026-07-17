package org.tedros.ai.toolrelay.function;

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
 * entrega os gateways do {@code tedros-integration} as tools de backend, nos
 * moldes do {@link org.tedros.ai.toolrelay.TAiRelayConfig}.
 * <p>
 * Diferente do padrao do FE (bloco {@code static} que lanca excecao e
 * derrubaria o deploy), a resolucao aqui e adiada ate a primeira tool que
 * precisar do gateway: config ausente vira {@link IllegalStateException} com
 * mensagem amigavel, que a tool converte em resultado de erro para o LLM.
 * <p>
 * Nota: {@link GitLabGateway} e um singleton do modulo compartilhado — a
 * primeira inicializacao fixa url/key ate o restart do servidor.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TIntegrationGatewayProvider {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TIntegrationGatewayProvider.class);

	static final long CACHE_TTL_MILLIS = 3 * 60 * 1000L;

	public static final String REDMINE_NOT_CONFIGURED =
			"Redmine integration is not configured on the server (properties 'redmine.url' and 'redmine.api.key').";
	public static final String GITLAB_NOT_CONFIGURED =
			"GitLab integration is not configured on the server (properties 'gitlab.url' and 'gitlab.api.key').";

	/** Valores resolvidos das properties; campos nulos = nao configurado. */
	static class Snapshot {
		final String redmineUrl;
		final String redmineKey;
		final String gitlabUrl;
		final String gitlabKey;

		Snapshot(String redmineUrl, String redmineKey, String gitlabUrl, String gitlabKey) {
			this.redmineUrl = redmineUrl;
			this.redmineKey = redmineKey;
			this.gitlabUrl = gitlabUrl;
			this.gitlabKey = gitlabKey;
		}

		boolean hasRedmine() {
			return notBlank(redmineUrl) && notBlank(redmineKey);
		}

		boolean hasGitlab() {
			return notBlank(gitlabUrl) && notBlank(gitlabKey);
		}

		private static boolean notBlank(String v) {
			return v != null && !v.isBlank();
		}
	}

	private volatile Snapshot cached;
	private volatile long cachedAt;
	private final ReentrantLock refreshLock = new ReentrantLock();

	// gateway Redmine reconstruido quando url/key mudam apos refresh do cache
	private volatile RedmineApiGateway redmineGateway;
	private volatile String redmineGatewayUrl;
	private volatile String redmineGatewayKey;

	/**
	 * Gateway Redmine configurado, ou {@link IllegalStateException} com
	 * mensagem amigavel se as properties nao estiverem definidas.
	 */
	public RedmineApiGateway redmineGateway() {
		Snapshot snap = snapshot();
		if (!snap.hasRedmine())
			throw new IllegalStateException(REDMINE_NOT_CONFIGURED);
		RedmineApiGateway gw = redmineGateway;
		if (gw != null && snap.redmineUrl.equals(redmineGatewayUrl)
				&& snap.redmineKey.equals(redmineGatewayKey))
			return gw;
		refreshLock.lock();
		try {
			if (redmineGateway == null || !snap.redmineUrl.equals(redmineGatewayUrl)
					|| !snap.redmineKey.equals(redmineGatewayKey)) {
				redmineGateway = new RedmineApiGateway(snap.redmineUrl, snap.redmineKey);
				redmineGatewayUrl = snap.redmineUrl;
				redmineGatewayKey = snap.redmineKey;
			}
			return redmineGateway;
		} finally {
			refreshLock.unlock();
		}
	}

	/**
	 * Gateway GitLab configurado, ou {@link IllegalStateException} com
	 * mensagem amigavel se as properties nao estiverem definidas.
	 */
	public GitLabGateway gitlabGateway() {
		Snapshot snap = snapshot();
		if (!snap.hasGitlab())
			throw new IllegalStateException(GITLAB_NOT_CONFIGURED);
		return GitLabGateway.getInstance(snap.gitlabUrl, snap.gitlabKey);
	}

	Snapshot snapshot() {
		Snapshot snap = cached;
		if (snap != null && (System.currentTimeMillis() - cachedAt) < CACHE_TTL_MILLIS)
			return snap;

		refreshLock.lock();
		try {
			snap = cached;
			if (snap != null && (System.currentTimeMillis() - cachedAt) < CACHE_TTL_MILLIS)
				return snap;
			try {
				snap = load();
			} catch (Exception e) {
				LOGGER.error("Error loading Redmine/GitLab integration properties", e);
				// mantem o ultimo snapshot valido; sem ele, nada configurado
				snap = (cached != null) ? cached : new Snapshot(null, null, null, null);
			}
			cached = snap;
			cachedAt = System.currentTimeMillis();
			return snap;
		} finally {
			refreshLock.unlock();
		}
	}

	/** Protegido para permitir override nos testes de unidade. */
	protected Snapshot load() throws Exception {
		
		try(TServiceLocator serv = TServiceLocator.getInstance()) {
			TDomainPropertieSupport support = serv.lookupWithRetry(TDomainPropertieSupport.JNDI_NAME);
			return new Snapshot(
					trim(support.getSystemPropertyValue(ItSupportPropertie.REDMINE_URL.getValue())),
					trim(support.getSystemPropertyValue(ItSupportPropertie.REDMINE_KEY.getValue())),
					trim(support.getSystemPropertyValue(ItSupportPropertie.GITLAB_URL.getValue())),
					trim(support.getSystemPropertyValue(ItSupportPropertie.GITLAB_KEY.getValue())));
		} 
	}

	private static String trim(String v) {
		return v != null ? v.trim() : null;
	}
}
