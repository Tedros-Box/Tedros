package org.tedros.ai.toolrelay.function.gitlab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.tedros.ai.toolrelay.function.TIntegrationGatewayProvider;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.ai.toolrelay.function.TServerFunctionConstants;
import org.tedros.integration.gitlab.ai.model.TGitLabBranch;
import org.tedros.integration.gitlab.ai.model.TGitLabCommit;
import org.tedros.integration.gitlab.ai.model.TGitLabMergeRequest;
import org.tedros.integration.gitlab.ai.model.TGitLabProjectId;
import org.tedros.integration.gitlab.ai.model.TGitLabProjectName;
import org.tedros.integration.gitlab.api.model.BranchModel;
import org.tedros.integration.gitlab.api.model.CommitModel;
import org.tedros.integration.gitlab.gateway.GitLabGateway;

/**
 * Testes das tools GitLab migradas: nomes identicos aos do FE, resultado de
 * sucesso com as mesmas chaves, erro do gateway e config ausente — gateway
 * mockado (construtor privado, sem chamadas externas).
 */
public class GitLabFunctionsTest {

	private GitLabGateway gateway;

	@Before
	public void setUp() {
		gateway = mock(GitLabGateway.class);
	}

	class FakeProvider extends TIntegrationGatewayProvider {
		final boolean configured;

		FakeProvider(boolean configured) {
			this.configured = configured;
		}

		@Override
		public GitLabGateway gitlabGateway() {
			if (!configured)
				throw new IllegalStateException(GITLAB_NOT_CONFIGURED);
			return gateway;
		}
	}

	private <T extends TServerAiFunction> T withGateway(T fn) {
		inject(fn, new FakeProvider(true));
		return fn;
	}

	private <T extends TServerAiFunction> T withoutConfig(T fn) {
		inject(fn, new FakeProvider(false));
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

	private static void assertSuccess(Map<String, Object> result, String action, String payloadKey) {
		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals(action, result.get(TServerFunctionConstants.ACTION));
		assertTrue(result.containsKey(TServerFunctionConstants.SYSTEM_INSTRUCTION));
		assertTrue(result.containsKey(payloadKey));
	}

	private static void assertError(Map<String, Object> result, String action, String message) {
		assertEquals(TServerFunctionConstants.ERROR, result.get(TServerFunctionConstants.STATUS));
		assertEquals(action, result.get(TServerFunctionConstants.ACTION));
		assertEquals(message, result.get(TServerFunctionConstants.ERROR_MESSAGE));
	}

	private static TGitLabProjectId projectId(long id) {
		TGitLabProjectId v = new TGitLabProjectId();
		v.setProjectId(id);
		return v;
	}

	// ---------------------------------------------------------------- tests

	@Test
	public void namesMatchFrontendAndRegisterWithoutCollision() {
		List<TServerAiFunction> fns = List.of(
				new ListAllGitLabProjectFunction(),
				new SearchGitLabProjectFunction(),
				new GetGitLabRepositoryBranchFunction(),
				new SearchGitLabRepositoryBranchesFunction(),
				new GetGitLabRepositoryCommitFunction(),
				new SearchGitLabRepositoryCommitsFunction(),
				new GetGitLabRepositoryCommitDiffFunction(),
				new GetGitLabMergeRequestRawDiffsFunction(),
				new SearchGitLabOpenedMergeRequestFunction());

		TServerFunctionCatalog catalog = new TServerFunctionCatalog(fns);
		assertEquals(9, catalog.toolSpecifications().size());
		for (String name : List.of(
				"list_gitlab_projects", "get_gitlab_projects_by_name",
				"get_gitlab_repository_branch", "list_gitlab_repository_branches",
				"get_gitlab_repository_commit", "list_gitlab_repository_commits",
				"get_gitlab_repository_commit_diff", "get_merge_request_raw_diffs",
				"list_gitlab_opened_merge_request"))
			assertTrue("missing tool " + name, catalog.contains(name));
	}

	@Test
	public void listProjectsSuccessAndError() {
		when(gateway.getAllProjects()).thenReturn(List.of());
		var fn = withGateway(new ListAllGitLabProjectFunction());
		assertSuccess(asMap(fn.execute(new Object(), null)), "gitlab_projects_listed", "projects");

		when(gateway.getAllProjects()).thenThrow(new RuntimeException("api down"));
		assertError(asMap(fn.execute(new Object(), null)), "gitlab_projects_list_error", "api down");
	}

	@Test
	public void searchProjectsByNameSuccess() {
		when(gateway.searchProjectsByName(anyString())).thenReturn(List.of());
		var fn = withGateway(new SearchGitLabProjectFunction());
		TGitLabProjectName arg = new TGitLabProjectName();
		arg.setName("tedros");
		assertSuccess(asMap(fn.execute(arg, null)), "gitlab_projects_searched", "projects");
	}

	@Test
	public void getBranchSuccess() {
		when(gateway.getSingleRepositoryBranches(anyLong(), anyString()))
				.thenReturn(new BranchModel("main", null, false, false, false, false, false, false, null));
		var fn = withGateway(new GetGitLabRepositoryBranchFunction());
		TGitLabBranch arg = new TGitLabBranch();
		arg.setProjectId(1L);
		arg.setBranchName("main");
		assertSuccess(asMap(fn.execute(arg, null)), "gitlab_repository_branch_retrieved", "branch");
	}

	@Test
	public void listBranchesSuccess() {
		when(gateway.getRepositoryBranches(anyLong())).thenReturn(List.of());
		var fn = withGateway(new SearchGitLabRepositoryBranchesFunction());
		assertSuccess(asMap(fn.execute(projectId(1L), null)),
				"gitlab_repository_branches_retrieved", "branches");
	}

	@Test
	public void getCommitSuccess() {
		when(gateway.getSingleRepositoryCommit(anyLong(), anyString()))
				.thenReturn(new CommitModel("abc123", null, null, null, null, null, null, null,
						null, null, null, null, null, null, null));
		var fn = withGateway(new GetGitLabRepositoryCommitFunction());
		TGitLabCommit arg = new TGitLabCommit();
		arg.setProjectId(1L);
		arg.setCommitSha("abc123");
		assertSuccess(asMap(fn.execute(arg, null)), "gitlab_repository_commit_retrieved", "commit");
	}

	@Test
	public void listCommitsSuccess() {
		when(gateway.getRepositoryCommits(anyLong())).thenReturn(List.of());
		var fn = withGateway(new SearchGitLabRepositoryCommitsFunction());
		assertSuccess(asMap(fn.execute(projectId(1L), null)),
				"gitlab_repository_commits_retrieved", "commits");
	}

	@Test
	public void getCommitDiffSuccess() {
		when(gateway.getRepositoryCommitDiff(anyLong(), anyString())).thenReturn(List.of());
		var fn = withGateway(new GetGitLabRepositoryCommitDiffFunction());
		TGitLabCommit arg = new TGitLabCommit();
		arg.setProjectId(1L);
		arg.setCommitSha("abc123");
		assertSuccess(asMap(fn.execute(arg, null)),
				"gitlab_repository_commit_diff_retrieved", "commit_diff");
	}

	@Test
	public void mergeRequestRawDiffsSuccessAndError() throws Exception {
		when(gateway.getMergeRequestRawDiffs(anyLong(), anyLong())).thenReturn("diff --git a b");
		var fn = withGateway(new GetGitLabMergeRequestRawDiffsFunction());
		TGitLabMergeRequest arg = new TGitLabMergeRequest();
		arg.setProjectId(1L);
		arg.setMergeRequestIid(9L);
		Map<String, Object> ok = asMap(fn.execute(arg, null));
		assertSuccess(ok, "gitlab_merge_request_raw_diffs_retrieved", "raw_diffs");
		assertEquals("diff --git a b", ok.get("raw_diffs"));

		when(gateway.getMergeRequestRawDiffs(anyLong(), anyLong()))
				.thenThrow(new java.io.IOException("timeout"));
		assertError(asMap(fn.execute(arg, null)), "gitlab_merge_request_raw_diffs_error", "timeout");
	}

	@Test
	public void openedMergeRequestsSuccess() {
		when(gateway.getOpenedMergeRequests(anyLong())).thenReturn(List.of());
		var fn = withGateway(new SearchGitLabOpenedMergeRequestFunction());
		assertSuccess(asMap(fn.execute(projectId(1L), null)),
				"gitlab_opened_merge_requests_retrieved", "opened_merge_requests");
	}

	@Test
	public void missingConfigBecomesFriendlyErrorResult() {
		var fn = withoutConfig(new ListAllGitLabProjectFunction());
		Map<String, Object> result = asMap(fn.execute(new Object(), null));
		assertEquals(TServerFunctionConstants.ERROR, result.get(TServerFunctionConstants.STATUS));
		assertEquals(TIntegrationGatewayProvider.GITLAB_NOT_CONFIGURED,
				result.get(TServerFunctionConstants.ERROR_MESSAGE));
	}
}
