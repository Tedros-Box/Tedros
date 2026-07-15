package org.tedros.ai.ejb.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.tedros.ai.meeting.MeetingMinutesRagService;
import org.tedros.core.security.model.TUser;
import org.tedros.server.ejb.controller.ITSecurityController;
import org.tedros.server.entity.ITUser;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;

/**
 * Unit tests for MeetingMinutesRagController with injected fakes (no EJB container).
 */
public class MeetingMinutesRagControllerTest {

	private static final TAccessToken TOKEN = new TAccessToken("tk-rag");

	static class FakeSecurity implements ITSecurityController {
		ITUser user;

		@Override
		public ITUser getUser(TAccessToken token) {
			return user;
		}

		@Override
		public boolean isAccessGranted(TAccessToken clent) {
			return true;
		}

		@Override
		public boolean isPolicieAllowed(TAccessToken token, String securityId, String... action) {
			return true;
		}
	}

	static class FakeRag extends MeetingMinutesRagService {
		final AtomicReference<String> ingestId = new AtomicReference<>();
		final AtomicReference<Map<String, String>> ingestMeta = new AtomicReference<>();
		final AtomicReference<String> purgeId = new AtomicReference<>();
		List<Map<String, Object>> searchResult = List.of();
		RuntimeException toThrow;

		@Override
		public void ingestAsync(String meetingId, String text, Map<String, String> metadata) {
			if (toThrow != null) {
				throw toThrow;
			}
			ingestId.set(meetingId);
			ingestMeta.set(metadata);
		}

		@Override
		public void purge(String meetingId) {
			if (toThrow != null) {
				throw toThrow;
			}
			purgeId.set(meetingId);
		}

		@Override
		public List<Map<String, Object>> search(String query, Long ownerUserId, int maxResults, double minScore) {
			if (toThrow != null) {
				throw toThrow;
			}
			return searchResult;
		}
	}

	private MeetingMinutesRagController controller(FakeRag rag, FakeSecurity security) {
		MeetingMinutesRagController c = new MeetingMinutesRagController();
		c.ragService = rag;
		c.securityController = security;
		return c;
	}

	@Test
	public void ingestAddsOwnerUserIdFromToken() {
		FakeRag rag = new FakeRag();
		FakeSecurity sec = new FakeSecurity();
		TUser user = new TUser("Davis");
		user.setId(99L);
		sec.user = user;
		MeetingMinutesRagController c = controller(rag, sec);

		Map<String, String> meta = new HashMap<>();
		TResult<Void> r = c.ingest(TOKEN, "m-1", "texto", meta);
		assertEquals(TState.SUCCESS, r.getState());
		assertEquals("m-1", rag.ingestId.get());
		assertEquals("99", meta.get("owner_user_id"));
	}

	@Test
	public void ingestWithoutSecurityLeavesMetadataUntouched() {
		FakeRag rag = new FakeRag();
		MeetingMinutesRagController c = controller(rag, null);
		Map<String, String> meta = new HashMap<>();
		c.ingest(TOKEN, "m-1", "texto", meta);
		assertNull(meta.get("owner_user_id"));
	}

	@Test
	public void ingestFailureReturnsError() {
		FakeRag rag = new FakeRag();
		rag.toThrow = new RuntimeException("ingest failed");
		TResult<Void> r = controller(rag, new FakeSecurity()).ingest(TOKEN, "m-1", "t", new HashMap<>());
		assertEquals(TState.ERROR, r.getState());
		assertEquals("ingest failed", r.getMessage());
	}

	@Test
	public void purgeDelegates() {
		FakeRag rag = new FakeRag();
		TResult<Void> r = controller(rag, new FakeSecurity()).purge(TOKEN, "m-2");
		assertEquals(TState.SUCCESS, r.getState());
		assertEquals("m-2", rag.purgeId.get());
	}

	@Test
	public void searchReturnsRows() {
		FakeRag rag = new FakeRag();
		rag.searchResult = List.of(Map.of("text", "hit"));
		FakeSecurity sec = new FakeSecurity();
		TUser user = new TUser("Davis");
		user.setId(1L);
		sec.user = user;
		TResult<List<Map<String, Object>>> r = controller(rag, sec).search(TOKEN, "q", 5, 0.5);
		assertEquals(TState.SUCCESS, r.getState());
		assertEquals(1, r.getValue().size());
		assertTrue(r.getValue().get(0).get("text").equals("hit"));
	}
}
