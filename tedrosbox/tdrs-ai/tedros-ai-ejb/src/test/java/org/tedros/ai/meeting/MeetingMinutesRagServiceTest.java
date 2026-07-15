package org.tedros.ai.meeting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;
import org.tedros.ai.toolrelay.TAiProvider;
import org.tedros.ai.toolrelay.TAiRelayConfig;
import org.tedros.ai.toolrelay.TAiRelayConfigSnapshot;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;

/**
 * Unit tests for RAG ingest/purge/search with in-memory embedding store.
 */
public class MeetingMinutesRagServiceTest {

	static class HashEmbeddingModel implements EmbeddingModel {
		@Override
		public Response<Embedding> embed(String text) {
			return Response.from(vectorFor(text));
		}

		@Override
		public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
			List<Embedding> list = new ArrayList<>();
			for (TextSegment s : textSegments) {
				list.add(vectorFor(s.text()));
			}
			return Response.from(list);
		}

		@Override
		public int dimension() {
			return 8;
		}

		private static Embedding vectorFor(String text) {
			float[] v = new float[8];
			int h = text == null ? 0 : text.hashCode();
			for (int i = 0; i < v.length; i++) {
				v[i] = ((h >> (i * 3)) & 0xFF) / 255f;
			}
			return Embedding.from(v);
		}
	}

	/** Minimal store sufficient for purge/search/ingest used by the service. */
	static class MemStore implements EmbeddingStore<TextSegment> {
		final Map<String, TextSegment> byId = new HashMap<>();
		int removeAllCalls;

		@Override
		public String add(Embedding embedding) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void add(String id, Embedding embedding) {
			byId.put(id, TextSegment.from(""));
		}

		@Override
		public String add(Embedding embedding, TextSegment embedded) {
			String id = "id-" + byId.size();
			byId.put(id, embedded);
			return id;
		}

		@Override
		public List<String> addAll(List<Embedding> embeddings) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
			List<String> ids = new ArrayList<>();
			for (int i = 0; i < embeddings.size(); i++) {
				ids.add(add(embeddings.get(i), embedded.get(i)));
			}
			return ids;
		}

		@Override
		public void removeAll(Filter filter) {
			removeAllCalls++;
			byId.clear();
		}

		@Override
		public void removeAll(java.util.Collection<String> ids) {
			ids.forEach(byId::remove);
		}

		@Override
		public void removeAll() {
			byId.clear();
		}

		@Override
		public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
			List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
			for (Map.Entry<String, TextSegment> e : byId.entrySet()) {
				matches.add(new EmbeddingMatch<>(0.9, e.getKey(), request.queryEmbedding(), e.getValue()));
			}
			return new EmbeddingSearchResult<>(matches);
		}
	}

	static class TestableService extends MeetingMinutesRagService {
		final EmbeddingModel model = new HashEmbeddingModel();
		final MemStore store = new MemStore();
		int pauseCalls;
		boolean failIngest;

		@Override
		protected EmbeddingModel embeddingModel(boolean forQuery) {
			if (failIngest) {
				throw new IllegalStateException("AI relay is not configured for embeddings");
			}
			return model;
		}

		@Override
		protected EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel) {
			return store;
		}

		@Override
		protected void retryPause(int attempt) {
			pauseCalls++;
		}
	}

	private TestableService service;

	@Before
	public void setUp() {
		service = new TestableService();
	}

	@Test
	public void ingestPurgesThenStoresSegments() {
		Map<String, String> meta = new HashMap<>();
		meta.put("meeting_id", "m-1");
		meta.put("owner_user_id", "7");
		service.ingest("m-1", "Discussão sobre deploy e rollback da release.", meta);
		assertTrue(service.store.removeAllCalls >= 1);
		assertTrue(service.store.byId.size() >= 1);
	}

	@Test
	public void ingestFailsFastOnNonRetryableConfigError() {
		service.failIngest = true;
		try {
			service.ingest("m-1", "text", Map.of("meeting_id", "m-1"));
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("not configured"));
			assertEquals(0, service.pauseCalls);
		}
	}

	@Test
	public void ingestRetriesTransientErrorsThenRethrows() {
		AtomicInteger attempts = new AtomicInteger();
		MeetingMinutesRagService s = new MeetingMinutesRagService() {
			@Override
			public void purge(String meetingId) {
				// skip store setup; count only ingest attempts
			}

			@Override
			protected EmbeddingModel embeddingModel(boolean forQuery) {
				attempts.incrementAndGet();
				throw new RuntimeException("temporary network glitch");
			}

			@Override
			protected void retryPause(int attempt) {
				// no sleep in unit test
			}
		};
		try {
			s.ingest("m-1", "text", Map.of("meeting_id", "m-1"));
			fail("expected RuntimeException");
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("temporary network glitch"));
			assertEquals(3, attempts.get());
		}
	}

	@Test
	public void purgeDoesNotPropagateStoreErrors() {
		MeetingMinutesRagService s = new MeetingMinutesRagService() {
			@Override
			protected EmbeddingModel embeddingModel(boolean forQuery) {
				throw new RuntimeException("boom");
			}
		};
		s.purge("m-1");
	}

	@Test
	public void embeddingModelSupportsGeminiAndOpenAi() {
		assertTrue(embeddingFor(TAiProvider.GEMINI, Map.of(), false) instanceof GoogleAiEmbeddingModel);
		assertTrue(embeddingFor(TAiProvider.GEMINI, Map.of(), true) instanceof GoogleAiEmbeddingModel);
		assertTrue(embeddingFor(TAiProvider.OPENAI, Map.of(), false) instanceof OpenAiEmbeddingModel);
	}

	@Test
	public void grokFallsBackToOpenAiEmbeddingsWhenOpenAiKeyPresent() {
		EmbeddingModel model = embeddingFor(TAiProvider.GROK, Map.of("sys.openai.key", "openai-key"), false);
		assertTrue(model instanceof OpenAiEmbeddingModel);
	}

	@Test
	public void grokFallsBackToGeminiEmbeddingsWhenOnlyGeminiKeyPresent() {
		EmbeddingModel model = embeddingFor(TAiProvider.GROK, Map.of("sys.gemini.key", "gemini-key"), false);
		assertTrue(model instanceof GoogleAiEmbeddingModel);
	}

	@Test
	public void grokWithoutFallbackKeyFailsFastWithClearMessage() {
		try {
			embeddingFor(TAiProvider.GROK, Map.of(), false);
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("does not provide an embeddings API"));
			assertTrue(e.getMessage().contains("sys.openai.key"));
		}
	}

	@Test
	public void embeddingProviderOverrideCannotBeGrok() {
		try {
			embeddingFor(TAiProvider.OPENAI, Map.of("sys.ai.embedding.provider", "GROK"), false);
			fail("expected IllegalStateException");
		} catch (IllegalStateException e) {
			assertTrue(e.getMessage().contains("cannot be GROK"));
		}
	}

	private static EmbeddingModel embeddingFor(TAiProvider chatProvider, Map<String, String> props,
			boolean forQuery) {
		MeetingMinutesRagService s = new MeetingMinutesRagService() {
			@Override
			protected String lookupSystemProperty(String name) {
				return props.get(name);
			}
		};
		s.relayConfig = new TAiRelayConfig() {
			@Override
			public TAiRelayConfigSnapshot snapshot() {
				return new TAiRelayConfigSnapshot(true, chatProvider, "chat-key", "chat-model", null, 30, 5, 2000,
						false);
			}
		};
		return s.embeddingModel(forQuery);
	}

	@Test
	public void searchMapsMatches() {
		service.store.add(Embedding.from(new float[] { 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f }),
				TextSegment.from("conteúdo da ata",
						dev.langchain4j.data.document.Metadata.from(Map.of("meeting_id", "m-9"))));
		List<Map<String, Object>> rows = service.search("ata", 7L, 5, 0.1);
		assertEquals(1, rows.size());
		assertEquals("conteúdo da ata", rows.get(0).get("text"));
		assertEquals(0.9, rows.get(0).get("score"));
	}

	@Test
	public void resolvePostgresEndpointParsesJdbcUrl() {
		String prevUrl = System.getProperty("tedros.db.url");
		String prevUser = System.getProperty("tedros.db.user");
		String prevPass = System.getProperty("tedros.db.password");
		try {
			System.setProperty("tedros.db.url", "jdbc:postgresql://postgres:5432/tedros");
			System.setProperty("tedros.db.user", "tdrs");
			System.setProperty("tedros.db.password", "xpto");
			MeetingMinutesRagService.PostgresEndpoint ep = new MeetingMinutesRagService().resolvePostgresEndpoint();
			assertEquals("postgres", ep.host);
			assertEquals(5432, ep.port);
			assertEquals("tedros", ep.database);
			assertEquals("tdrs", ep.user);
			assertEquals("xpto", ep.password);
		} finally {
			restoreProp("tedros.db.url", prevUrl);
			restoreProp("tedros.db.user", prevUser);
			restoreProp("tedros.db.password", prevPass);
		}
	}

	private static void restoreProp(String key, String value) {
		if (value == null) {
			System.clearProperty(key);
		} else {
			System.setProperty(key, value);
		}
	}
}
