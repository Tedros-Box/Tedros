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

import org.apache.commons.lang3.StringUtils;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.person.ejb.controller.IEmployeeController;
import org.tedros.person.model.Employee;
import org.tedros.server.query.TCompareOp;
import org.tedros.server.query.TJoinType;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.service.TServiceLocator;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code SearchEmployeeFunction} do FE (app-person-fx) —
 * name, description e strings de resultado copiados literalmente; controller
 * seguro via lookup JNDI server-side com o token do request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class SearchEmployeeFunction implements TServerAiFunction {

	private static final String NO_EMPLOYEE_FOUND_MATCHING_THE_CRITERIA = "No employee found matching the criteria.";

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(SearchEmployeeFunction.class);

	public static final String NAME = "search_employees";
	public static final String DESCRIPTION = "Searches the system for an employee by name or document number. "
			+ "Use this tool to find an employee's exact details, including their numerical ID ('id' or 'tedrosUserId'), "
			+ "which is required as input for other tools like 'get_employee_activities' or 'search_gmuds'.";

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
		return SearchEmployee.class;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		SearchEmployee v = (SearchEmployee) arg;

		LOGGER.info("Searching Persons with parameters: {}", v);

		TSelect sel = new TSelect(Employee.class);

		if (StringUtils.isNotBlank(v.getName())) {
			sel.addOrCondition("name", TCompareOp.LIKE, v.getName().trim().toLowerCase());
		}

		if (StringUtils.isNotBlank(v.getDocument())) {
			sel.addJoin(TJoinType.LEFT, TSelect.ALIAS, "documents", "docs");
			sel.addOrCondition("docs", "value", TCompareOp.LIKE, v.getDocument().trim().toLowerCase());
			sel.addOrCondition("docs", "content", TCompareOp.LIKE, v.getDocument().trim().toLowerCase());
		}

		try {
			TResult<List<Employee>> res = search(ctx.getToken(), sel);
			if (res.getState().equals(TState.SUCCESS)) {
				if (res.getValue().isEmpty()) {
					return Map.of(
							STATUS, ERROR,
							ACTION, "no_employee_found",
							ERROR_MESSAGE, NO_EMPLOYEE_FOUND_MATCHING_THE_CRITERIA);
				}
				return Map.of(
						STATUS, SUCCESS,
						ACTION, "employee_found",
						SYSTEM_INSTRUCTION, "Employees found successfully. "
								+ "Do not retry again. Proceed with the user's request.",
						"employees", res.getValue());
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "employee_search_failed",
					ERROR_MESSAGE, "Error searching employees: " + safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "no_employees_found",
				ERROR_MESSAGE, NO_EMPLOYEE_FOUND_MATCHING_THE_CRITERIA);
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<Employee>> search(TAccessToken token, TSelect<Employee> sel) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IEmployeeController controller = serv.lookupWithRetry(IEmployeeController.JNDI_NAME);
			return controller.search(token, sel);
		} finally {
			serv.close();
		}
	}
}
