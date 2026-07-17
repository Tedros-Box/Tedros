package org.tedros.ai.toolrelay.function.redmine;

import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ACTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR_MESSAGE;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.STATUS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SUCCESS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SYSTEM_INSTRUCTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionResults.noDataFound;
import static org.tedros.ai.toolrelay.function.TServerFunctionResults.safeMessage;

import java.util.Map;

import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TIntegrationGatewayProvider;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.integration.redmine.ai.model.RedmineIssueIdToFind;
import org.tedros.integration.redmine.api.model.TIssueEvidenceInfo;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code GetRedmineIssueAiFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class GetRedmineIssueAiFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(GetRedmineIssueAiFunction.class);

	public static final String NAME = "get_redmine_issue";
	public static final String DESCRIPTION = """
		    Call this function when user wants to analyze, review, audit, or understand a Redmine issue.
		    Always use this FIRST when a specific issue ID is mentioned.
		    Returns full issue data including:
		    • Title, description, status, priority, assignee
		    • Custom fields (deliverable, HPA, required profile, service type)
		    • All notes/comments in chronological order
		    • List of attachments with file names, sizes, and metadata
		    Never analyze content or attachments without calling this first.
		    Input: issueId (integer, required)
		    Output: Complete issue object with attachments list
		    """;

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
			LOGGER.info("Fetching Redmine issue #{}", v.getIssueId());
			RedmineApiGateway gateway = gateways.redmineGateway(ctx);

			TIssueEvidenceInfo issue = gateway.getTIssueEvidenceInfo(v.getIssueId());
			if (issue == null)
				return noDataFound();

			int attachCount = issue.getAttachments() != null ? issue.getAttachments().size() : 0;
			LOGGER.info("Issue #{} retrieved. {} attachment(s) found.", v.getIssueId(), attachCount);

			return Map.of(
					STATUS, SUCCESS,
					ACTION, "redmine_issue_retrieved",
					SYSTEM_INSTRUCTION, "Issue retrieved successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"issue", issue);

		} catch (Exception e) {
			LOGGER.error("Failed to fetch issue #" + v.getIssueId(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "redmine_issue_retrieved",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
