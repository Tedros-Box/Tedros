package org.tedros.ai.toolrelay.function.redmine;

import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ACTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR_MESSAGE;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.STATUS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SUCCESS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SYSTEM_INSTRUCTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionResults.safeMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TIntegrationGatewayProvider;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.integration.redmine.ai.model.FilterCondition;
import org.tedros.integration.redmine.ai.model.RedmineAssignedToFilter;
import org.tedros.integration.redmine.api.model.TIssueEvidenceInfo;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code RedmineFilterIssueByUserAiFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class RedmineFilterIssueByUserAiFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(RedmineFilterIssueByUserAiFunction.class);

	public static final String NAME = "filter_redmine_issues_by_assigned_to_id";
	public static final String DESCRIPTION = "Filter Redmine issues by assigned to ID.";

	@Inject
	TIntegrationGatewayProvider gateways;

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
		return RedmineAssignedToFilter.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		RedmineAssignedToFilter v = (RedmineAssignedToFilter) arg;
		try {
			LOGGER.info("Received request to filter Redmine issues by assigned to ID: {}",
					v.getAssigned_to_id());

			Map<String, FilterCondition> filters = new HashMap<>();
			filters.put("assigned_to_id", FilterCondition.equalsTo(v.getAssigned_to_id()));

			RedmineApiGateway gateway = gateways.redmineGateway();
			List<TIssueEvidenceInfo> issues = gateway.getIssuesByFilters(filters);

			LOGGER.info("Result found: {} issues assigned to ID: {}", issues.size(),
					v.getAssigned_to_id());

			return Map.of(
					STATUS, SUCCESS,
					ACTION, "redmine_issues_filtered_by_assigned_to_id",
					SYSTEM_INSTRUCTION, "Issues filtered successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"issues", issues);

		} catch (Exception e) {
			LOGGER.error("Error filtering issues by assigned to ID", e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "redmine_issues_filter_by_assigned_to_id_error",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
