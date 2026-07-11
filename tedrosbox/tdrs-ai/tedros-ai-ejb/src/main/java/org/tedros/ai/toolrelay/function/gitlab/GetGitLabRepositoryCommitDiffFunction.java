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
import org.tedros.integration.gitlab.ai.model.TGitLabCommit;
import org.tedros.integration.gitlab.api.model.CommitDiffModel;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code GetGitLabRepositoryCommitDiffFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class GetGitLabRepositoryCommitDiffFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(GetGitLabRepositoryCommitDiffFunction.class);

	public static final String NAME = "get_gitlab_repository_commit_diff";
	public static final String DESCRIPTION = "Get the diff of a commit in a project by project id and commit sha";

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
		return TGitLabCommit.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		TGitLabCommit v = (TGitLabCommit) arg;
		try {
			LOGGER.info("Finding for a repository commit diff for projectId {} and Commit sha {}",
					v.getProjectId(), v.getCommitSha());
			List<CommitDiffModel> lst = gateways.gitlabGateway()
					.getRepositoryCommitDiff(v.getProjectId(), v.getCommitSha());
			return Map.of(
					STATUS, SUCCESS,
					ACTION, "gitlab_repository_commit_diff_retrieved",
					SYSTEM_INSTRUCTION, "Commit diff retrieved successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"commit_diff", lst);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "gitlab_repository_commit_diff_error",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
