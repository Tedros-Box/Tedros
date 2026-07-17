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
import org.tedros.person.ejb.controller.IEmployeeController;
import org.tedros.person.model.Employee;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.util.TLoggerUtil;
import org.tedros.server.util.TServiceLocator;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code ListEmployeesFunction} do FE (app-person-fx) —
 * name, description e strings de resultado copiados literalmente; controller
 * seguro via lookup JNDI server-side com o token do request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class ListEmployeesFunction implements TServerAiFunction {

	private static final String NONE_EMPLOYEE_FOUND = "None employee found";

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(ListEmployeesFunction.class);

	public static final String NAME = "list_all_employees";

	public static final String DESCRIPTION = "Retrieves a complete list of all registered employees in the system. "
			+ "Returns their details, including their internal IDs. Use this if you need to find an employee but don't "
			+ "have specific search criteria, or if you need to run an operation across multiple employees.";

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
		LOGGER.info("Listing all employess");

		try {
			TResult<List<Employee>> res = listAll(ctx.getToken());
			if (res.getState().equals(TState.SUCCESS)) {
				if (res.getValue().isEmpty()) {
					return Map.of(
							STATUS, ERROR,
							ACTION, "none_employee_found",
							ERROR_MESSAGE, NONE_EMPLOYEE_FOUND);
				}
				return Map.of(
						STATUS, SUCCESS,
						ACTION, "employees_found",
						SYSTEM_INSTRUCTION, "Employees found successfully. "
								+ "Do not retry again. Proceed with the user's request.",
						"employees", res.getValue());
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "employee_listing_failed",
					ERROR_MESSAGE, "Error listing employees: " + safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "none_employees_found",
				ERROR_MESSAGE, NONE_EMPLOYEE_FOUND);
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<Employee>> listAll(TAccessToken token) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IEmployeeController controller = serv.lookupWithRetry(IEmployeeController.JNDI_NAME);
			return controller.listAll(token, Employee.class);
		} finally {
			serv.close();
		}
	}
}
