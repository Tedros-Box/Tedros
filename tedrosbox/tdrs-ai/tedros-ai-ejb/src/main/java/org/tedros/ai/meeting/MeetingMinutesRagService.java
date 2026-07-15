package org.tedros.ai.meeting;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.tedros.ai.cdi.bo.GenericEntityBO;
import org.tedros.ai.toolrelay.TAiProvider;
import org.tedros.ai.toolrelay.TAiRelayConfig;
import org.tedros.ai.toolrelay.TAiRelayConfigSnapshot;
import org.tedros.core.support.TPropertieSupport;
import org.tedros.it.tools.entity.MeetingMinutes;
import org.tedros.server.cdi.bo.ITGenericBO;
import org.tedros.server.ejb.service.TEjbService;
import org.tedros.server.service.TServiceLocator;
import org.tedros.util.TLoggerUtil;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

@LocalBean
@Stateless(name = "MeetingMinutesRagService")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MeetingMinutesRagService extends TEjbService<MeetingMinutes> {

	private static final Logger LOGGER = TLoggerUtil.getLogger(MeetingMinutesRagService.class);
	/** Schema-qualified: LangChain4j supports {@code schema.table} (see computeCleanTableName). */
	private static final String TABLE = "tedros_core.meeting_minutes_embeddings";
	/** Shared pgvector dimension for OpenAI and Gemini embedding backends. */
	private static final int DIMENSION = 1536;
	private static final String OPENAI_EMBEDDING_MODEL = "text-embedding-3-small";
	private static final String GEMINI_EMBEDDING_MODEL = "gemini-embedding-001";
	private static final String PROP_OPENAI_KEY = "sys.openai.key";
	private static final String PROP_GEMINI_KEY = "sys.gemini.key";
	private static final String PROP_EMBEDDING_PROVIDER = "sys.ai.embedding.provider";

	@Inject
	TAiRelayConfig relayConfig;
	
	@Inject
	private GenericEntityBO<MeetingMinutes> bo;

	private static final Pattern JDBC_PG = Pattern
			.compile("^jdbc:postgresql://([^:/?]+)(?::(\\d+))?/([^?]+)(?:\\?.*)?$");

	@Asynchronous
	public void ingestAsync(String meetingId, String text, Map<String, String> metadata) {
		try {
			ingest(meetingId, text, metadata);
		} catch (RuntimeException e) {
			// Avoid OpenEJB system-exception / InvalidateReferenceException on async failures.
			LOGGER.error("RAG ingestAsync failed for {}: {}", meetingId, e.getMessage(), e);
		}
	}

	public void ingest(String meetingId, String text, Map<String, String> metadata) {
		int attempts = 0;
		RuntimeException last = null;
		while (attempts < 3) {
			attempts++;
			try {
				purge(meetingId);
				EmbeddingModel embeddingModel = embeddingModel(false);
				EmbeddingStore<TextSegment> store = embeddingStore(embeddingModel);
				Metadata meta = Metadata.from(metadata != null ? metadata : Map.of("meeting_id", meetingId));
				Document document = Document.from(text, meta);
				EmbeddingStoreIngestor.builder()
						.documentSplitter(DocumentSplitters.recursive(800, 120))
						.embeddingModel(embeddingModel)
						.embeddingStore(store)
						.build()
						.ingest(document);
				return;
			} catch (RuntimeException e) {
				last = e;
				LOGGER.warn("RAG ingest attempt {} failed: {}", attempts, e.getMessage());
				if (isNonRetryableEmbeddingError(e)) {
					break;
				}
				retryPause(attempts);
			}
		}
		if (last != null) {
			throw last;
		}
	}

	public void purge(String meetingId) {
		try {
			EmbeddingModel embeddingModel = embeddingModel(false);
			EmbeddingStore<TextSegment> store = embeddingStore(embeddingModel);
			store.removeAll(MetadataFilterBuilder.metadataKey("meeting_id").isEqualTo(meetingId));
		} catch (Exception e) {
			LOGGER.warn("RAG purge failed for {}: {}", meetingId, e.getMessage());
		}
	}

	public List<Map<String, Object>> search(String query, Long ownerUserId, int maxResults, double minScore) {
		EmbeddingModel embeddingModel = embeddingModel(true);
		EmbeddingStore<TextSegment> store = embeddingStore(embeddingModel);
		Embedding embedding = embeddingModel.embed(query).content();
		var builder = EmbeddingSearchRequest.builder()
				.queryEmbedding(embedding)
				.maxResults(Math.max(1, maxResults))
				.minScore(minScore);
		if (ownerUserId != null) {
			builder.filter(MetadataFilterBuilder.metadataKey("owner_user_id").isEqualTo(String.valueOf(ownerUserId)));
		}
		EmbeddingSearchResult<TextSegment> result = store.search(builder.build());
		List<Map<String, Object>> out = new ArrayList<>();
		result.matches().forEach(m -> {
			Map<String, Object> row = new HashMap<>();
			row.put("score", m.score());
			row.put("text", m.embedded().text());
			row.put("metadata", m.embedded().metadata() != null ? m.embedded().metadata().toMap() : Map.of());
			out.add(row);
		});
		return out;
	}

	protected void retryPause(int attempt) {
		try {
			Thread.sleep(500L * attempt);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * @param forQuery {@code true} for search queries (Gemini {@code RETRIEVAL_QUERY});
	 *                 {@code false} for document ingest (Gemini {@code RETRIEVAL_DOCUMENT}).
	 *                 OpenAI ignores this flag.
	 */
	protected EmbeddingModel embeddingModel(boolean forQuery) {
		TAiRelayConfigSnapshot cfg = relayConfig.snapshot();
		if (cfg == null || !cfg.isAiEnabled()) {
			throw new IllegalStateException("AI relay is not configured for embeddings");
		}
		EmbeddingBackend backend = resolveEmbeddingBackend(cfg);
		LOGGER.debug("RAG embeddings via {} model {}", backend.provider, backend.modelName());
		return switch (backend.provider) {
			case OPENAI -> OpenAiEmbeddingModel.builder()
					.apiKey(backend.apiKey)
					.modelName(OPENAI_EMBEDDING_MODEL)
					.maxRetries(3)
					.build();
			case GEMINI -> GoogleAiEmbeddingModel.builder()
					.apiKey(backend.apiKey)
					.modelName(GEMINI_EMBEDDING_MODEL)
					.outputDimensionality(DIMENSION)
					.taskType(forQuery ? GoogleAiEmbeddingModel.TaskType.RETRIEVAL_QUERY
							: GoogleAiEmbeddingModel.TaskType.RETRIEVAL_DOCUMENT)
					.maxRetries(3)
					.build();
			case GROK -> throw new IllegalStateException(
					"Grok/xAI has no embeddings API; resolveEmbeddingBackend should never return GROK");
		};
	}

	/**
	 * Chooses an embedding backend that actually exposes an embeddings API.
	 * <ul>
	 * <li>OPENAI chat → OpenAI embeddings ({@code text-embedding-3-small})</li>
	 * <li>GEMINI chat → Gemini embeddings ({@code gemini-embedding-001})</li>
	 * <li>GROK chat → OpenAI or Gemini key fallback (xAI has no public embeddings API)</li>
	 * <li>Optional override: {@code sys.ai.embedding.provider=OPENAI|GEMINI}</li>
	 * </ul>
	 */
	protected EmbeddingBackend resolveEmbeddingBackend(TAiRelayConfigSnapshot cfg) {
		TAiProvider override = parseEmbeddingProviderOverride(lookupSystemProperty(PROP_EMBEDDING_PROVIDER));
		if (override != null) {
			return backendForProvider(override, cfg, true);
		}
		TAiProvider chatProvider = cfg.getProvider();
		if (chatProvider == null) {
			throw new IllegalStateException("AI relay provider is not configured for embeddings");
		}
		return switch (chatProvider) {
			case OPENAI, GEMINI -> backendForProvider(chatProvider, cfg, false);
			case GROK -> resolveGrokEmbeddingFallback(cfg);
		};
	}

	private EmbeddingBackend resolveGrokEmbeddingFallback(TAiRelayConfigSnapshot cfg) {
		String openaiKey = firstNonBlank(lookupSystemProperty(PROP_OPENAI_KEY));
		if (openaiKey != null) {
			LOGGER.info("Chat provider is GROK; using OpenAI embeddings (sys.openai.key) for RAG");
			return new EmbeddingBackend(TAiProvider.OPENAI, openaiKey);
		}
		String geminiKey = firstNonBlank(lookupSystemProperty(PROP_GEMINI_KEY));
		if (geminiKey != null) {
			LOGGER.info("Chat provider is GROK; using Gemini embeddings (sys.gemini.key) for RAG");
			return new EmbeddingBackend(TAiProvider.GEMINI, geminiKey);
		}
		throw new IllegalStateException(
				"Grok/xAI does not provide an embeddings API (model text-embedding-3-small is OpenAI-only). "
						+ "Configure sys.openai.key or sys.gemini.key for meeting-minutes RAG, "
						+ "or set sys.ai.embedding.provider=OPENAI|GEMINI with the matching key.");
	}

	private EmbeddingBackend backendForProvider(TAiProvider provider, TAiRelayConfigSnapshot cfg,
			boolean fromOverride) {
		return switch (provider) {
			case OPENAI -> {
				String key = fromOverride ? firstNonBlank(lookupSystemProperty(PROP_OPENAI_KEY), cfg.getApiKey())
						: cfg.getApiKey();
				if (key == null || key.isBlank()) {
					throw new IllegalStateException("OpenAI API key missing for embeddings (sys.openai.key)");
				}
				yield new EmbeddingBackend(TAiProvider.OPENAI, key);
			}
			case GEMINI -> {
				String key = fromOverride ? firstNonBlank(lookupSystemProperty(PROP_GEMINI_KEY), cfg.getApiKey())
						: cfg.getApiKey();
				if (key == null || key.isBlank()) {
					throw new IllegalStateException("Gemini API key missing for embeddings (sys.gemini.key)");
				}
				yield new EmbeddingBackend(TAiProvider.GEMINI, key);
			}
			case GROK -> throw new IllegalStateException(
					"sys.ai.embedding.provider cannot be GROK; use OPENAI or GEMINI");
		};
	}

	private static TAiProvider parseEmbeddingProviderOverride(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			TAiProvider p = TAiProvider.valueOf(value.trim().toUpperCase());
			if (p == TAiProvider.GROK) {
				throw new IllegalStateException("sys.ai.embedding.provider cannot be GROK; use OPENAI or GEMINI");
			}
			return p;
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException(
					"Invalid sys.ai.embedding.provider '" + value + "'; expected OPENAI or GEMINI");
		}
	}

	/** Hook for unit tests (avoids JNDI). */
	protected String lookupSystemProperty(String name) {
		try {
			TServiceLocator serv = TServiceLocator.getInstance();
			try {
				TPropertieSupport support = serv.lookupWithRetry(TPropertieSupport.JNDI_NAME);
				String v = support.getValue(name);
				return v != null ? v.trim() : null;
			} finally {
				serv.close();
			}
		} catch (Exception e) {
			LOGGER.debug("Could not read property {}: {}", name, e.getMessage());
			return null;
		}
	}

	private static boolean isNonRetryableEmbeddingError(Throwable e) {
		Throwable cur = e;
		while (cur != null) {
			String name = cur.getClass().getSimpleName();
			if (cur instanceof IllegalStateException || "ModelNotFoundException".equals(name)
					|| "InvalidRequestException".equals(name) || "AuthenticationException".equals(name)) {
				return true;
			}
			String msg = cur.getMessage();
			if (msg != null && (msg.contains("does not exist") || msg.contains("does not provide an embeddings API")
					|| msg.contains("not configured for embeddings"))) {
				return true;
			}
			cur = cur.getCause();
		}
		return false;
	}

	protected static final class EmbeddingBackend {
		final TAiProvider provider;
		final String apiKey;

		EmbeddingBackend(TAiProvider provider, String apiKey) {
			this.provider = provider;
			this.apiKey = apiKey;
		}

		String modelName() {
			return provider == TAiProvider.GEMINI ? GEMINI_EMBEDDING_MODEL : OPENAI_EMBEDDING_MODEL;
		}
	}

	/**
	 * Uses a direct PostgreSQL connection (PGSimpleDataSource via host/port builder).
	 * The TomEE/Tomcat pooled {@code tedrosDataSource} wraps connections and
	 * {@code PGvector.addVectorType} fails with "Not a wrapper of org.postgresql.PGConnection".
	 */
	protected EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
		PostgresEndpoint ep = resolvePostgresEndpoint();
		int dim = embeddingModel.dimension() > 0 ? embeddingModel.dimension() : DIMENSION;
		return PgVectorEmbeddingStore.builder()
				.host(ep.host)
				.port(ep.port)
				.database(ep.database)
				.user(ep.user)
				.password(ep.password)
				.table(TABLE)
				.dimension(dim)
				.createTable(true)
				.skipCreateVectorExtension(true)
				.build();
	}

	protected PostgresEndpoint resolvePostgresEndpoint() {
		String url = firstNonBlank(System.getProperty("tedros.db.url"), System.getenv("TEDROS_DB_URL"));
		String user = firstNonBlank(System.getProperty("tedros.db.user"), System.getenv("TEDROS_DB_USER"), "tdrs");
		String password = firstNonBlank(System.getProperty("tedros.db.password"), System.getenv("TEDROS_DB_PASSWORD"),
				"xpto");
		if (url == null || url.isBlank()) {
			throw new IllegalStateException(
					"PostgreSQL URL not configured (tedros.db.url / TEDROS_DB_URL) for pgvector RAG");
		}
		if (!url.startsWith("jdbc:postgresql:")) {
			throw new IllegalStateException("pgvector RAG requires PostgreSQL JDBC URL, got: " + url);
		}
		Matcher m = JDBC_PG.matcher(url);
		if (!m.matches()) {
			// Fallback for unusual forms: parse as URI after stripping jdbc:
			try {
				URI uri = URI.create(url.substring("jdbc:".length()));
				String host = uri.getHost();
				int port = uri.getPort() > 0 ? uri.getPort() : 5432;
				String path = uri.getPath();
				String database = path != null && path.startsWith("/") ? path.substring(1) : path;
				if (host == null || database == null || database.isBlank()) {
					throw new IllegalStateException("Cannot parse PostgreSQL JDBC URL: " + url);
				}
				return new PostgresEndpoint(host, port, database, user, password);
			} catch (RuntimeException e) {
				throw new IllegalStateException("Cannot parse PostgreSQL JDBC URL: " + url, e);
			}
		}
		String host = m.group(1);
		int port = m.group(2) != null ? Integer.parseInt(m.group(2)) : 5432;
		String database = m.group(3);
		return new PostgresEndpoint(host, port, database, user, password);
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String v : values) {
			if (v != null && !v.isBlank()) {
				return v;
			}
		}
		return null;
	}

	protected static final class PostgresEndpoint {
		final String host;
		final int port;
		final String database;
		final String user;
		final String password;

		PostgresEndpoint(String host, int port, String database, String user, String password) {
			this.host = host;
			this.port = port;
			this.database = database;
			this.user = user;
			this.password = password;
		}
	}

	@Override
	public ITGenericBO<MeetingMinutes> getBussinesObject() {
		return bo;
	}
}
