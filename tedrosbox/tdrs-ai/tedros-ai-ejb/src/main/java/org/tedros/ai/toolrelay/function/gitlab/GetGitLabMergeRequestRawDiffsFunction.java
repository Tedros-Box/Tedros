package org.tedros.ai.toolrelay.function.gitlab;

import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ACTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR_MESSAGE;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.STATUS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SUCCESS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SYSTEM_INSTRUCTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionResults.safeMessage;

import java.util.Map;

import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TIntegrationGatewayProvider;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.integration.gitlab.ai.model.TGitLabMergeRequest;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code GetGitLabMergeRequestRawDiffsFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class GetGitLabMergeRequestRawDiffsFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(GetGitLabMergeRequestRawDiffsFunction.class);

	public static final String NAME = "get_merge_request_raw_diffs";
	public static final String DESCRIPTION = "Show raw diffs of the files changed in a merge request";

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
		return TGitLabMergeRequest.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		TGitLabMergeRequest v = (TGitLabMergeRequest) arg;
		try {
			LOGGER.info("Searching GitLab merge request raws diffs for project id: {} and merge request iid: {}",
					v.getProjectId(), v.getMergeRequestIid());
			String result = gateways.gitlabGateway()
					.getMergeRequestRawDiffs(v.getProjectId(), v.getMergeRequestIid());
			return Map.of(
					STATUS, SUCCESS,
					ACTION, "gitlab_merge_request_raw_diffs_retrieved",
					SYSTEM_INSTRUCTION, "The system has retrieved the merge request raws diffs successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"raw_diffs", result);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "gitlab_merge_request_raw_diffs_error",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
