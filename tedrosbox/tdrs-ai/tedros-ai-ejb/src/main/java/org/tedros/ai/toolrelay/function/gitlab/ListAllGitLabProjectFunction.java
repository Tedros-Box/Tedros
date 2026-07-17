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
import org.tedros.ai.toolrelay.function.model.Empty;
import org.tedros.integration.gitlab.api.model.GitLabProject;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code ListAllGitLabProjectFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class ListAllGitLabProjectFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(ListAllGitLabProjectFunction.class);

	public static final String NAME = "list_gitlab_projects";
	public static final String DESCRIPTION = "Get a list of all gitlab projects";

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
			LOGGER.info("Listing all GitLab projects");
			List<GitLabProject> lst = gateways.gitlabGateway(ctx).getAllProjects();
			return Map.of(
					STATUS, SUCCESS,
					ACTION, "gitlab_projects_listed",
					SYSTEM_INSTRUCTION, "Projects listed successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"projects", lst);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "gitlab_projects_list_error",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
