package org.tedros.ai.toolrelay.function.services;

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
import org.tedros.server.result.TResult;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.service.TServiceLocator;
import org.tedros.server.util.TLoggerUtil;
import org.tedros.services.ejb.controller.IServiceController;
import org.tedros.services.model.Service;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code ListServicesFunction} do FE (app-services-fx) —
 * name, description e strings de resultado copiados literalmente; controller
 * seguro via lookup JNDI server-side com o token do request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class ListServicesFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(ListServicesFunction.class);

	public static final String NAME = "list_services";
	public static final String DESCRIPTION = "List all provision services";

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
		LOGGER.info("Listing all services");

		try {
			TResult<List<Service>> res = listAll(ctx.getToken());
			if (res.getValue() != null) {
				return Map.of(
						STATUS, SUCCESS,
						ACTION, "services_listed",
						SYSTEM_INSTRUCTION, "Services listed successfully. "
								+ "Do not retry again. Proceed with the user's request.",
						"services", res.getValue());
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "services_list_error",
					ERROR_MESSAGE, safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "no_services_found",
				ERROR_MESSAGE, "No services available in the system.");
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<Service>> listAll(TAccessToken token) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IServiceController controller = serv.lookupWithRetry(IServiceController.JNDI_NAME);
			return controller.listAll(token, Service.class);
		} finally {
			serv.close();
		}
	}
}
