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
import org.tedros.integration.redmine.ai.model.RedmineIssueIdToFind;
import org.tedros.integration.redmine.api.model.TTimeEntry;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code RedmineListIssueTimeEntryAiFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class RedmineListIssueTimeEntryAiFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(RedmineListIssueTimeEntryAiFunction.class);

	public static final String NAME = "get_redmine_issue_time_entries";
	public static final String DESCRIPTION = "Retrieves the history of time entries, work logs, and hours spent for a specific Redmine issue ID.";

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
		return RedmineIssueIdToFind.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		RedmineIssueIdToFind v = (RedmineIssueIdToFind) arg;
		try {
			LOGGER.info("Fetching time entries for Redmine issue ID: {}", v.getIssueId());
			RedmineApiGateway gateway = gateways.redmineGateway(ctx);

			List<TTimeEntry> entries = gateway.getTimeEntriesForIssue(v.getIssueId());

			LOGGER.info("Entries found for issue {}: {}", v.getIssueId(), entries.size());

			return Map.of(
					STATUS, SUCCESS,
					ACTION, "redmine_issue_time_entries_retrieved",
					SYSTEM_INSTRUCTION, "Time entries retrieved successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"time_entries", entries);

		} catch (Exception e) {
			LOGGER.error("Error fetching time entries", e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "redmine_issue_time_entries_error",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
