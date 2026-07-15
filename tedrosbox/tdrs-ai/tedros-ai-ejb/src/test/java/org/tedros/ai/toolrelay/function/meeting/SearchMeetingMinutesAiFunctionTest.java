package org.tedros.ai.toolrelay.function.meeting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.ai.toolrelay.function.TServerFunctionConstants;
import org.tedros.ai.toolrelay.function.meeting.model.MeetingMinutesSearchModel;
import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

/**
 * Unit tests for search_meeting_minutes AI tool (RAG service stubbed).
 */
public class SearchMeetingMinutesAiFunctionTest {

	private static final TAccessToken TOKEN = new TAccessToken("tk-meeting");

	private static TAiToolContext ctx() {
		TUser user = new TUser("Davis");
		user.setId(42L);
		return new TAiToolContext(user, TOKEN);
	}

	static class StubSearch extends SearchMeetingMinutesAiFunction {
		List<Map<String, Object>> hits;
		RuntimeException toThrow;
		String queryUsed;
		Long ownerUsed;

		@Override
		protected List<Map<String, Object>> searchMeetings(String query, Long ownerId) {
			this.queryUsed = query;
			this.ownerUsed = ownerId;
			if (toThrow != null) {
				throw toThrow;
			}
			return hits;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object result) {
		assertTrue(result instanceof Map);
		return (Map<String, Object>) result;
	}

	private static MeetingMinutesSearchModel model(String query) {
		MeetingMinutesSearchModel m = new MeetingMinutesSearchModel();
		m.setQuery(query);
		return m;
	}

	@Test
	public void nameRegistersInCatalogWithoutCollision() {
		TServerFunctionCatalog catalog = new TServerFunctionCatalog(
				List.<TServerAiFunction>of(new SearchMeetingMinutesAiFunction()));
		assertTrue(catalog.contains(SearchMeetingMinutesAiFunction.NAME));
		assertEquals(1, catalog.toolSpecifications().size());
	}

	@Test
	public void blankQueryIsRejected() {
		StubSearch fn = new StubSearch();
		Map<String, Object> r1 = asMap(fn.execute(null, ctx()));
		assertEquals(TServerFunctionConstants.ERROR, r1.get(TServerFunctionConstants.STATUS));
		assertEquals("meeting_minutes_search_error", r1.get(TServerFunctionConstants.ACTION));

		Map<String, Object> r2 = asMap(fn.execute(model("  "), ctx()));
		assertEquals("meeting_minutes_search_error", r2.get(TServerFunctionConstants.ACTION));
	}

	@Test
	public void emptyHitsReturnNotFoundWithInstruction() {
		StubSearch fn = new StubSearch();
		fn.hits = List.of();
		Map<String, Object> r = asMap(fn.execute(model("deployments"), ctx()));
		assertEquals(TServerFunctionConstants.SUCCESS, r.get(TServerFunctionConstants.STATUS));
		assertEquals("meeting_minutes_not_found", r.get(TServerFunctionConstants.ACTION));
		assertEquals("deployments", fn.queryUsed);
		assertEquals(Long.valueOf(42L), fn.ownerUsed);
		assertTrue(((List<?>) r.get("matches")).isEmpty());
	}

	@Test
	public void hitsReturnFoundPayloadForOwner() {
		StubSearch fn = new StubSearch();
		fn.hits = List.of(Map.of("score", 0.9, "text", "discutimos deploy"));
		Map<String, Object> r = asMap(fn.execute(model("deploy"), ctx()));
		assertEquals("meeting_minutes_found", r.get(TServerFunctionConstants.ACTION));
		assertEquals(1, ((List<?>) r.get("matches")).size());
		assertTrue(String.valueOf(r.get(TServerFunctionConstants.SYSTEM_INSTRUCTION))
				.contains("Answer exclusively from the provided meeting context"));
	}

	@Test
	public void nullContextPassesNullOwner() {
		StubSearch fn = new StubSearch();
		fn.hits = List.of();
		fn.execute(model("x"), null);
		assertEquals(null, fn.ownerUsed);
	}

	@Test
	public void searchExceptionBecomesErrorMap() {
		StubSearch fn = new StubSearch();
		fn.toThrow = new RuntimeException("pgvector down");
		Map<String, Object> r = asMap(fn.execute(model("x"), ctx()));
		assertEquals(TServerFunctionConstants.ERROR, r.get(TServerFunctionConstants.STATUS));
		assertEquals("meeting_minutes_search_error", r.get(TServerFunctionConstants.ACTION));
		assertEquals("pgvector down", r.get(TServerFunctionConstants.ERROR_MESSAGE));
	}
}
