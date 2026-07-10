package org.tedros.ai.toolrelay;

import java.util.concurrent.locks.ReentrantLock;

import org.tedros.core.support.TPropertieSupport;
import org.tedros.server.service.TServiceLocator;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Resolve e cacheia a configuracao do relay a partir das properties do
 * sistema (via {@link TPropertieSupport}, lookup JNDI server-side).
 * <p>
 * Regra de resolucao (secao 7.5 do guia): {@code sys.ai.enabled} define se a
 * IA esta ativa; {@code sys.ai.provider} define o provedor; as chaves
 * {@code sys.<provider>.key/model/prompt} definem token, modelo e prompt.
 * <p>
 * Os valores resolvidos ficam em cache por {@link #CACHE_TTL_MILLIS} para
 * evitar lookups remotos a cada turno.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TAiRelayConfig {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiRelayConfig.class);

	private static final long CACHE_TTL_MILLIS = 3 * 60 * 1000L;

	private static final int DEFAULT_CONVERSATION_TTL_MIN = 30;
	private static final int DEFAULT_PENDING_TURN_TTL_MIN = 5;
	private static final int DEFAULT_MAX_CONVERSATIONS = 2000;

	private volatile TAiRelayConfigSnapshot cached;
	private volatile long cachedAt;
	private final ReentrantLock refreshLock = new ReentrantLock();

	/**
	 * Retorna o snapshot atual da configuracao, atualizando o cache se o TTL
	 * expirou. Nunca retorna null; em caso de falha na leitura das properties
	 * com cache vazio, retorna um snapshot desabilitado.
	 */
	public TAiRelayConfigSnapshot snapshot() {
		TAiRelayConfigSnapshot snap = cached;
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
				LOGGER.error("Error loading AI relay configuration", e);
				// mantem o ultimo snapshot valido se existir
				snap = (cached != null) ? cached
						: new TAiRelayConfigSnapshot(false, null, null, null, null,
								DEFAULT_CONVERSATION_TTL_MIN, DEFAULT_PENDING_TURN_TTL_MIN,
								DEFAULT_MAX_CONVERSATIONS, false);
			}
			cached = snap;
			cachedAt = System.currentTimeMillis();
			return snap;
		} finally {
			refreshLock.unlock();
		}
	}

	private TAiRelayConfigSnapshot load() throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			TPropertieSupport support = serv.lookupWithRetry(TPropertieSupport.JNDI_NAME);

			boolean aiEnabled = Boolean.parseBoolean(trim(support.getValue("sys.ai.enabled")));
			if (!aiEnabled)
				return new TAiRelayConfigSnapshot(false, null, null, null, null,
						DEFAULT_CONVERSATION_TTL_MIN, DEFAULT_PENDING_TURN_TTL_MIN,
						DEFAULT_MAX_CONVERSATIONS, false);

			TAiProvider provider = parseProvider(trim(support.getValue("sys.ai.provider")));
			String prefix = "sys." + provider.name().toLowerCase() + ".";
			String apiKey = trim(support.getValue(prefix + "key"));
			String model = trim(support.getValue(prefix + "model"));
			String prompt = trim(support.getValue(prefix + "prompt"));

			int convTtl = parseInt(trim(support.getValue("sys.ai.toolrelay.conversation.ttl.min")),
					DEFAULT_CONVERSATION_TTL_MIN);
			int pendingTtl = parseInt(trim(support.getValue("sys.ai.toolrelay.pendingturn.ttl.min")),
					DEFAULT_PENDING_TURN_TTL_MIN);
			int maxConv = parseInt(trim(support.getValue("sys.ai.toolrelay.max.conversations")),
					DEFAULT_MAX_CONVERSATIONS);
			boolean debug = Boolean.parseBoolean(trim(support.getValue("sys.ai.toolrelay.debug")));

			return new TAiRelayConfigSnapshot(true, provider, apiKey, model, prompt,
					convTtl, pendingTtl, maxConv, debug);
		} finally {
			serv.close();
		}
	}

	private TAiProvider parseProvider(String value) {
		if (value == null)
			throw new IllegalStateException("Property sys.ai.provider is not defined");
		return TAiProvider.valueOf(value.toUpperCase());
	}

	private static String trim(String v) {
		return v != null ? v.trim() : null;
	}

	private static int parseInt(String v, int def) {
		try {
			return v != null ? Integer.parseInt(v) : def;
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
