package org.tedros.ai.toolrelay.function.gitlab;

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
import org.tedros.integration.gitlab.ai.model.TGitLabProjectId;
import org.tedros.integration.gitlab.api.model.GitLabMergeRequest;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code SearchGitLabOpenedMergeRequestFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class SearchGitLabOpenedMergeRequestFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(SearchGitLabOpenedMergeRequestFunction.class);

	public static final String NAME = "list_gitlab_opened_merge_request";
	public static final String DESCRIPTION = "Get gitlab opened merge requests for a project";

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
		return TGitLabProjectId.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		TGitLabProjectId v = (TGitLabProjectId) arg;
		try {
			LOGGER.info("Searching GitLab merge request for project id: {}", v.getProjectId());
			List<GitLabMergeRequest> lst = gateways.gitlabGateway(ctx)
					.getOpenedMergeRequests(v.getProjectId());
			return Map.of(
					STATUS, SUCCESS,
					ACTION, "gitlab_opened_merge_requests_retrieved",
					SYSTEM_INSTRUCTION, "Opened merge requests retrieved successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"opened_merge_requests", lst);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "gitlab_opened_merge_requests_error",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
