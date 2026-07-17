package org.tedros.ai.toolrelay.function.redmine;

import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ACTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR_MESSAGE;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.STATUS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SUCCESS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SYSTEM_INSTRUCTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionResults.safeMessage;

import java.util.List;
import java.util.Map;

import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TIntegrationGatewayProvider;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.model.Empty;
import org.tedros.integration.redmine.api.model.TIssueStatus;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code RedmineListIssueStatusAiFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class RedmineListIssueStatusAiFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(RedmineListIssueStatusAiFunction.class);

	public static final String NAME = "list_all_redmine_statuses";
	public static final String DESCRIPTION = "List all the statuses of Redmine issues";

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
		return Empty.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		try {
			LOGGER.info("Listing all Redmine issue statuses");
			RedmineApiGateway gateway = gateways.redmineGateway(ctx);

			List<TIssueStatus> statuses = gateway.listIssueStatuses();

			LOGGER.info("Result found: {} statuses", statuses.size());

			return Map.of(
					STATUS, SUCCESS,
					ACTION, "redmine_issue_statuses_listed",
					SYSTEM_INSTRUCTION, "Issue statuses listed successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"issue_statuses", statuses);

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "redmine_issue_statuses_list_error",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
