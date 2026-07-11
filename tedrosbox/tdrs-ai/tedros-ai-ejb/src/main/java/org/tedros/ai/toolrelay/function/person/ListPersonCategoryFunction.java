package org.tedros.ai.toolrelay.function.person;

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
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.model.Empty;
import org.tedros.person.ejb.controller.IPersonCategoryController;
import org.tedros.person.model.PersonCategory;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.service.TServiceLocator;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code ListPersonCategoryFunction} do FE
 * (app-person-fx) — name, description e strings de resultado copiados
 * literalmente; controller seguro via lookup JNDI server-side com o token do
 * request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class ListPersonCategoryFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(ListPersonCategoryFunction.class);

	public static final String NAME = "list_categories_person";
	public static final String DESCRIPTION = "List all categories that can be assigned to a person";

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
		LOGGER.info("Listing all person categories");
		try {
			TResult<List<PersonCategory>> res = listAll(ctx.getToken());
			if (res.getState().equals(TState.SUCCESS)) {
				if (res.getValue().isEmpty()) {
					return Map.of(
							STATUS, ERROR,
							ACTION, "no_person_categories_found",
							ERROR_MESSAGE, "No person categories available in the system.");
				}

				return Map.of(
						STATUS, SUCCESS,
						ACTION, "person_categories_listed",
						SYSTEM_INSTRUCTION, "Person categories listed successfully. "
								+ "Do not retry again. Proceed with the user's request.",
						"person_categories", res.getValue());
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "person_categories_list_error",
					ERROR_MESSAGE, safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "no_person_categories_found",
				ERROR_MESSAGE, "No person categories available in the system.");
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<PersonCategory>> listAll(TAccessToken token) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IPersonCategoryController controller = serv.lookupWithRetry(IPersonCategoryController.JNDI_NAME);
			return controller.listAll(token, PersonCategory.class);
		} finally {
			serv.close();
		}
	}
}
