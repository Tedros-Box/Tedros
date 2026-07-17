package org.tedros.ai.toolrelay.function.redmine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TIntegrationGatewayProvider;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.ai.toolrelay.function.TServerFunctionConstants;
import org.tedros.integration.redmine.ai.model.FilterCondition;
import org.tedros.integration.redmine.ai.model.RedmineAssignedToFilter;
import org.tedros.integration.redmine.ai.model.RedmineIssueFilter;
import org.tedros.integration.redmine.ai.model.RedmineIssueIdToFind;
import org.tedros.integration.redmine.ai.model.RedmineUserFilter;
import org.tedros.integration.redmine.api.model.TIssueEvidenceInfo;
import org.tedros.integration.redmine.api.model.TIssueStatus;
import org.tedros.integration.redmine.api.model.TRedmineUser;
import org.tedros.integration.redmine.api.model.TTimeEntry;
import org.tedros.core.security.model.TUser;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;

/**
 * Testes das tools Redmine migradas: nomes identicos aos do FE, resultado de
 * sucesso com as mesmas chaves, caminho no-data, erro do gateway e config
 * ausente — tudo com gateway fake (sem chamadas externas).
 */
public class RedmineFunctionsTest {

	// ---------------------------------------------------------------- fakes

	static class FakeRedmineGateway extends RedmineApiGateway {
		TIssueEvidenceInfo issue;
		List<TIssueEvidenceInfo> issues = List.of();
		List<TIssueStatus> statuses = List.of();
		List<TTimeEntry> entries = List.of();
		List<TRedmineUser> users = List.of();
		RuntimeException toThrow;

		FakeRedmineGateway() {
			super("http://redmine.local", "key");
		}

		private void maybeThrow() {
			if (toThrow != null)
				throw toThrow;
		}

		@Override
		public TIssueEvidenceInfo getTIssueEvidenceInfo(Integer issueId) {
			maybeThrow();
			return issue;
		}

		@Override
		public List<TIssueEvidenceInfo> getIssuesByFilters(Map<String, FilterCondition> filters) {
			maybeThrow();
			return issues;
		}

		@Override
		public List<TIssueStatus> listIssueStatuses() {
			maybeThrow();
			return statuses;
		}

		@Override
		public List<TTimeEntry> getTimeEntriesForIssue(Integer issueId) {
			maybeThrow();
			return entries;
		}

		@Override
		public List<TRedmineUser> findUser(String name) {
			maybeThrow();
			return users;
		}
	}

	static class FakeProvider extends TIntegrationGatewayProvider {
		final RedmineApiGateway gateway;
		final boolean configured;

		FakeProvider(RedmineApiGateway gateway, boolean configured) {
			this.gateway = gateway;
			this.configured = configured;
		}

		@Override
		public RedmineApiGateway redmineGateway(TAiToolContext ctx) {
			if (!configured)
				throw new IllegalStateException(REDMINE_NOT_CONFIGURED);
			return gateway;
		}
	}

	/** Contexto de teste com usuario autenticado (id preenchido). */
	private static TAiToolContext ctx() {
		TUser user = new TUser();
		user.setId(1L);
		return new TAiToolContext(user, null);
	}

	private final FakeRedmineGateway gateway = new FakeRedmineGateway();

	private <T extends TServerAiFunction> T withGateway(T fn) {
		inject(fn, new FakeProvider(gateway, true));
		return fn;
	}

	private <T extends TServerAiFunction> T withoutConfig(T fn) {
		inject(fn, new FakeProvider(null, false));
		return fn;
	}

	private static void inject(TServerAiFunction fn, TIntegrationGatewayProvider provider) {
		try {
			var field = fn.getClass().getDeclaredField("gateways");
			field.setAccessible(true);
			field.set(fn, provider);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object result) {
		assertTrue(result instanceof Map);
		return (Map<String, Object>) result;
	}

	// ---------------------------------------------------------------- tests

	@Test
	public void namesMatchFrontendAndRegisterWithoutCollision() {
		List<TServerAiFunction> fns = List.of(
				new GetRedmineIssueAiFunction(),
				new RedmineIssueSearchAiFunction(),
				new RedmineFilterIssueByUserAiFunction(),
				new RedmineListIssueStatusAiFunction(),
				new RedmineListIssueTimeEntryAiFunction(),
				new RedmineSearchUserAiFunction());

		TServerFunctionCatalog catalog = new TServerFunctionCatalog(fns);
		assertEquals(6, catalog.toolSpecifications().size());
		assertTrue(catalog.contains("get_redmine_issue"));
		assertTrue(catalog.contains("filter_redmine_issues"));
		assertTrue(catalog.contains("filter_redmine_issues_by_assigned_to_id"));
		assertTrue(catalog.contains("list_all_redmine_statuses"));
		assertTrue(catalog.contains("get_redmine_issue_time_entries"));
		assertTrue(catalog.contains("filter_redmine_user_by_name"));
	}

	@Test
	public void getIssueSuccess() {
		gateway.issue = new TIssueEvidenceInfo();
		var fn = withGateway(new GetRedmineIssueAiFunction());

		RedmineIssueIdToFind arg = new RedmineIssueIdToFind();
		arg.setIssueId(42);
		Map<String, Object> result = asMap(fn.execute(arg, ctx()));

		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals("redmine_issue_retrieved", result.get(TServerFunctionConstants.ACTION));
		assertNotNull(result.get(TServerFunctionConstants.SYSTEM_INSTRUCTION));
		assertEquals(gateway.issue, result.get("issue"));
	}

	@Test
	public void getIssueNoDataFoundMirrorsFrontendResponseWrapper() {
		gateway.issue = null;
		var fn = withGateway(new GetRedmineIssueAiFunction());

		RedmineIssueIdToFind arg = new RedmineIssueIdToFind();
		arg.setIssueId(42);
		Map<String, Object> result = asMap(fn.execute(arg, ctx()));

		// {"data":{"message":"No data found. ","object":null}} — como no FE
		Map<String, Object> data = asMap(result.get("data"));
		assertEquals(TServerFunctionConstants.NO_DATA_FOUND_MESSAGE, data.get("message"));
		assertNull(data.get("object"));
	}

	@Test
	public void getIssueErrorResult() {
		gateway.toThrow = new RuntimeException("boom");
		var fn = withGateway(new GetRedmineIssueAiFunction());

		RedmineIssueIdToFind arg = new RedmineIssueIdToFind();
		arg.setIssueId(1);
		Map<String, Object> result = asMap(fn.execute(arg, ctx()));

		assertEquals(TServerFunctionConstants.ERROR, result.get(TServerFunctionConstants.STATUS));
		assertEquals("redmine_issue_retrieved", result.get(TServerFunctionConstants.ACTION));
		assertEquals("boom", result.get(TServerFunctionConstants.ERROR_MESSAGE));
	}

	@Test
	public void missingConfigBecomesFriendlyErrorResult() {
		var fn = withoutConfig(new RedmineListIssueStatusAiFunction());

		Map<String, Object> result = asMap(fn.execute(new Object(), ctx()));

		assertEquals(TServerFunctionConstants.ERROR, result.get(TServerFunctionConstants.STATUS));
		assertEquals(TIntegrationGatewayProvider.REDMINE_NOT_CONFIGURED,
				result.get(TServerFunctionConstants.ERROR_MESSAGE));
	}

	@Test
	public void filterIssuesSuccess() {
		var fn = withGateway(new RedmineIssueSearchAiFunction());

		Map<String, Object> result = asMap(fn.execute(new RedmineIssueFilter(), ctx()));

		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals("redmine_issues_filtered", result.get(TServerFunctionConstants.ACTION));
		assertEquals(gateway.issues, result.get("issues"));
	}

	@Test
	public void filterByAssignedToSuccess() {
		var fn = withGateway(new RedmineFilterIssueByUserAiFunction());

		RedmineAssignedToFilter arg = new RedmineAssignedToFilter();
		arg.setAssigned_to_id("509");
		Map<String, Object> result = asMap(fn.execute(arg, ctx()));

		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals("redmine_issues_filtered_by_assigned_to_id",
				result.get(TServerFunctionConstants.ACTION));
	}

	@Test
	public void listStatusesSuccess() {
		var fn = withGateway(new RedmineListIssueStatusAiFunction());

		Map<String, Object> result = asMap(fn.execute(new Object(), ctx()));

		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals("redmine_issue_statuses_listed", result.get(TServerFunctionConstants.ACTION));
		assertEquals(gateway.statuses, result.get("issue_statuses"));
	}

	@Test
	public void listTimeEntriesSuccess() {
		var fn = withGateway(new RedmineListIssueTimeEntryAiFunction());

		RedmineIssueIdToFind arg = new RedmineIssueIdToFind();
		arg.setIssueId(7);
		Map<String, Object> result = asMap(fn.execute(arg, ctx()));

		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals("redmine_issue_time_entries_retrieved",
				result.get(TServerFunctionConstants.ACTION));
		assertEquals(gateway.entries, result.get("time_entries"));
	}

	@Test
	public void searchUserSuccessAndError() {
		var fn = withGateway(new RedmineSearchUserAiFunction());

		RedmineUserFilter arg = new RedmineUserFilter();
		arg.setUserName("Davis");
		Map<String, Object> ok = asMap(fn.execute(arg, ctx()));
		assertEquals(TServerFunctionConstants.SUCCESS, ok.get(TServerFunctionConstants.STATUS));
		assertEquals("redmine_users_retrieved", ok.get(TServerFunctionConstants.ACTION));

		gateway.toThrow = new RuntimeException("api down");
		Map<String, Object> err = asMap(fn.execute(arg, ctx()));
		assertEquals(TServerFunctionConstants.ERROR, err.get(TServerFunctionConstants.STATUS));
		assertEquals("redmine_users_error", err.get(TServerFunctionConstants.ACTION));
		assertEquals("api down", err.get(TServerFunctionConstants.ERROR_MESSAGE));
	}
}
