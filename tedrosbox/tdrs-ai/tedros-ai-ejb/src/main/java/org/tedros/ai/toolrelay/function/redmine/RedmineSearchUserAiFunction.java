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
import org.tedros.integration.redmine.ai.model.RedmineUserFilter;
import org.tedros.integration.redmine.api.model.TRedmineUser;
import org.tedros.integration.redmine.gateway.RedmineApiGateway;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Versao de backend da {@code RedmineSearchUserAiFunction} do FE
 * (app-itsupport-tools-fx) — name, description e strings de resultado
 * copiados literalmente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class RedmineSearchUserAiFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(RedmineSearchUserAiFunction.class);

	public static final String NAME = "filter_redmine_user_by_name";
	public static final String DESCRIPTION = "Filter redmine users by user name";

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
		return RedmineUserFilter.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		RedmineUserFilter v = (RedmineUserFilter) arg;
		try {
			LOGGER.info("Redmine searching user by name: {}", v.getUserName());
			RedmineApiGateway gateway = gateways.redmineGateway();

			List<TRedmineUser> users = gateway.findUser(v.getUserName());

			LOGGER.info("Result users found: {}", users.size());

			return Map.of(
					STATUS, SUCCESS,
					ACTION, "redmine_users_retrieved",
					SYSTEM_INSTRUCTION, "Users retrieved successfully. "
							+ "Do not retry again. Proceed with the user's request.",
					"users", users);

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "redmine_users_error",
					ERROR_MESSAGE, safeMessage(e));
		}
	}
}
