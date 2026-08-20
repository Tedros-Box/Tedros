package org.tedros.ai.toolrelay.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

/**
 * Criterio de aceite da migracao Fase 1.1: as 27 tools migradas do FE
 * (12 consultas BE + 15 Redmine/GitLab) + a tool nativa
 * {@code get_server_datetime} registram no catalogo sem colisao de nome,
 * com os nomes IDENTICOS aos do FE.
 */
public class TServerFunctionCatalogMigrationTest {

	private static final List<String> MIGRATED_TOOL_NAMES = List.of(
			// Redmine (6)
			"get_redmine_issue", "filter_redmine_issues",
			"filter_redmine_issues_by_assigned_to_id", "list_all_redmine_statuses",
			"get_redmine_issue_time_entries", "filter_redmine_user_by_name",
			// GitLab (9)
			"list_gitlab_projects", "get_gitlab_projects_by_name",
			"get_gitlab_repository_branch", "list_gitlab_repository_branches",
			"get_gitlab_repository_commit", "list_gitlab_repository_commits",
			"get_gitlab_repository_commit_diff", "get_merge_request_raw_diffs",
			"list_gitlab_opened_merge_request",
			// Consultas BE itsupport (3)
			"get_employee_activities", "search_gmuds", "search_service_catalog",
			// Person (6)
			"list_all_employees", "search_employees", "search_person",
			"list_categories_person", "list_statuses_person", "list_person_types",
			// Extensions/Services/Stock/Samples (4)
			"search_for_documents", "list_services", "search_products", "list_products_price");

	private static List<TServerAiFunction> allFunctions() {
		return List.of(
				new ServerDateTimeFunction(),
				new org.tedros.ai.toolrelay.function.redmine.GetRedmineIssueAiFunction(),
				new org.tedros.ai.toolrelay.function.redmine.RedmineIssueSearchAiFunction(),
				new org.tedros.ai.toolrelay.function.redmine.RedmineFilterIssueByUserAiFunction(),
				new org.tedros.ai.toolrelay.function.redmine.RedmineListIssueStatusAiFunction(),
				new org.tedros.ai.toolrelay.function.redmine.RedmineListIssueTimeEntryAiFunction(),
				new org.tedros.ai.toolrelay.function.redmine.RedmineSearchUserAiFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.ListAllGitLabProjectFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.SearchGitLabProjectFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.GetGitLabRepositoryBranchFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.SearchGitLabRepositoryBranchesFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.GetGitLabRepositoryCommitFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.SearchGitLabRepositoryCommitsFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.GetGitLabRepositoryCommitDiffFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.GetGitLabMergeRequestRawDiffsFunction(),
				new org.tedros.ai.toolrelay.function.gitlab.SearchGitLabOpenedMergeRequestFunction(),
				new org.tedros.ai.toolrelay.function.itsupport.GetEmployeeActivitiesAIFunction(),
				new org.tedros.ai.toolrelay.function.itsupport.SearchGmudAIFunction(),
				new org.tedros.ai.toolrelay.function.itsupport.SearchServiceCatalogAiFunction(),
				new org.tedros.ai.toolrelay.function.person.ListEmployeesFunction(),
				new org.tedros.ai.toolrelay.function.person.SearchEmployeeFunction(),
				new org.tedros.ai.toolrelay.function.person.SearchPersonFunction(),
				new org.tedros.ai.toolrelay.function.person.ListPersonCategoryFunction(),
				new org.tedros.ai.toolrelay.function.person.ListPersonStatusFunction(),
				new org.tedros.ai.toolrelay.function.person.ListPersonTypeFunction(),
				new org.tedros.ai.toolrelay.function.extension.SearchForDocumentsFunction(),
				new org.tedros.ai.toolrelay.function.services.ListServicesFunction(),
				new org.tedros.ai.toolrelay.function.stock.FindProductFunction(),
				new org.tedros.ai.toolrelay.function.samples.ListProductPriceAiFunction());
	}

	@Test
	public void all27MigratedToolsPlusDateTimeRegisterWithoutNameCollision() {
		List<TServerAiFunction> fns = allFunctions();
		assertEquals(29, fns.size());

		TServerFunctionCatalog catalog = new TServerFunctionCatalog(fns);
		// 29 specs = nenhuma colisao de nome sobrescreveu outra tool
		assertEquals(29, catalog.toolSpecifications().size());

		assertEquals(28, MIGRATED_TOOL_NAMES.size());
		for (String name : MIGRATED_TOOL_NAMES)
			assertTrue("missing migrated tool " + name, catalog.contains(name));
		assertTrue(catalog.contains("get_server_datetime"));
	}

	@Test
	public void everyFunctionHasNameDescriptionAndModel() {
		for (TServerAiFunction fn : allFunctions()) {
			assertTrue(fn.getClass().getSimpleName() + " name", fn.getName() != null && !fn.getName().isBlank());
			assertTrue(fn.getClass().getSimpleName() + " description",
					fn.getDescription() != null && !fn.getDescription().isBlank());
			assertTrue(fn.getClass().getSimpleName() + " model", fn.getModel() != null);
			// nenhuma das migradas encerra o turno sem voltar ao modelo
			assertTrue(fn.getClass().getSimpleName() + " revertToModel", fn.revertToModel());
		}
	}
}
