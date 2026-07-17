package org.tedros.ai.toolrelay.function.itsupport;

import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ACTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR_MESSAGE;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.STATUS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SUCCESS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SYSTEM_INSTRUCTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionResults.safeMessage;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.itsupport.model.GmudFilterModel;
import org.tedros.ai.toolrelay.function.itsupport.model.GmudModel;
import org.tedros.it.tools.ejb.controller.IGmudController;
import org.tedros.it.tools.entity.Gmud;
import org.tedros.server.query.TCompareOp;
import org.tedros.server.query.TJoinType;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.util.TServiceLocator;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code SearchGmudAIFunction} do FE
 * (app-itsupport-tools-fx) — name, description, strings de resultado e a
 * montagem do TSelect copiados literalmente; a chamada ao controller usa
 * lookup JNDI server-side com o token do request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class SearchGmudAIFunction implements TServerAiFunction {

	public static final String NAME = "search_gmuds";
	public static final String DESCRIPTION = "Search for Change Management Requests (GMUDs) based on multiple criteria. "
            + "Filter by title, type (Normal, Standard, Emergency), status, requester, reviewer, "
            + "scheduled dates, or external references (GitLab Project, Redmine Issue). "
            + "Returns a list of matching GMUDs.";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public Class<?> getModel() {
		return GmudFilterModel.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		GmudFilterModel filter = (GmudFilterModel) arg;

		String requesterAlias = "requester";
		String issueReferencesAlias = "issueReferences";
		String reviewsAlias = "reviews";

		TSelect<Gmud> select = new TSelect<>(Gmud.class);
		select.addJoin(TJoinType.INNER, TSelect.ALIAS, "requester", requesterAlias);
		select.addJoin(TJoinType.INNER, TSelect.ALIAS, "issueReferences", issueReferencesAlias);
		select.addJoin(TJoinType.INNER, TSelect.ALIAS, "reviews", reviewsAlias);

		if (filter != null) {
			if (StringUtils.isNotBlank(filter.getTitle())) {
				select.addAndCondition("title", TCompareOp.LIKE, filter.getTitle());
			}

			if (filter.getType() != null) {
				select.addAndCondition("type", TCompareOp.EQUAL, filter.getType().getDescription());
			}

			if (filter.getStatus() != null) {
				select.addAndCondition("status", TCompareOp.EQUAL, filter.getStatus().getDescription());
			}

			if (filter.getRequesterId() != null && filter.getRequesterId() > 0) {
				select.addAndCondition(requesterAlias, "id", TCompareOp.EQUAL, filter.getRequesterId());
			}

			if (StringUtils.isNotBlank(filter.getRequesterName())) {
				select.addAndCondition(requesterAlias, "name", TCompareOp.LIKE, filter.getRequesterName());
			}

			if (filter.getStartScheduledDate() != null) {
				select.addAndCondition("scheduledDate", TCompareOp.GREATER_EQ_THAN, filter.getStartScheduledDate());
			}

			if (filter.getEndScheduledDate() != null) {
				select.addAndCondition("scheduledDate", TCompareOp.LESS_EQ_THAN, filter.getEndScheduledDate());
			}

			if (filter.getGitLabProjectId() != null && filter.getGitLabProjectId() > 0) {
				select.addAndCondition("projectId", TCompareOp.EQUAL, filter.getGitLabProjectId());
			}

			if (StringUtils.isNotBlank(filter.getGitLabProjectName())) {
				select.addAndCondition("projectName", TCompareOp.LIKE, filter.getGitLabProjectName());
			}

			if (filter.getRedmineIssueId() != null && filter.getRedmineIssueId() > 0) {
				select.addAndCondition(issueReferencesAlias, "issueId", TCompareOp.EQUAL, filter.getRedmineIssueId());
			}

			if (StringUtils.isNotBlank(filter.getRedmineIssueTitle())) {
				select.addAndCondition(issueReferencesAlias, "issueTitle", TCompareOp.LIKE, filter.getRedmineIssueTitle());
			}

			if (StringUtils.isNotBlank(filter.getReviewerId())) {
				select.addAndCondition(reviewsAlias, "id", TCompareOp.EQUAL, filter.getReviewerId());
			}

			if (StringUtils.isNotBlank(filter.getReviewerName())) {
				select.addAndCondition(reviewsAlias, "name", TCompareOp.LIKE, filter.getReviewerName());
			}

			try {
				TResult<List<Gmud>> result = search(ctx.getToken(), select);

				if (result.isSuccess()) {

					List<Gmud> lst = result.getValue();

					return Map.of(
							STATUS, SUCCESS,
							ACTION, "gmuds_retrieved",
							SYSTEM_INSTRUCTION, "Gmuds retrieved successfully. "
									+ "Do not retry again. Proceed with the user's request.",
							"gmuds", toModels(lst));

				} else {
					String status = result.getState().name();
					String message = StringUtils.isNoneBlank(result.getMessage())
							? result.getMessage()
									: "Unknown error";

					return Map.of(
							STATUS, ERROR,
							ACTION, "gmuds_retrieval_" + status.toLowerCase(),
							ERROR_MESSAGE, message);
				}

			} catch (Exception e) {
				return Map.of(
						STATUS, ERROR,
						ACTION, "gmuds_retrieval_error",
						ERROR_MESSAGE, safeMessage(e));
			}

		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "gmuds_retrieval_no_criteria",
				ERROR_MESSAGE, "No filter criteria provided.");
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<Gmud>> search(TAccessToken token, TSelect<Gmud> select) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IGmudController controller = serv.lookupWithRetry(IGmudController.JNDI_NAME);
			return controller.search(token, select);
		} finally {
			serv.close();
		}
	}

	private static List<GmudModel> toModels(List<Gmud> entities) {
		return entities.stream().map(GmudModel::new).toList();
	}

}
