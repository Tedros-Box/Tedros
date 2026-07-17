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
import org.tedros.person.ejb.controller.IPersonStatusController;
import org.tedros.person.model.ClientCompanyStatus;
import org.tedros.person.model.CustomerStatus;
import org.tedros.person.model.EmployeeStatus;
import org.tedros.person.model.LegalStatus;
import org.tedros.person.model.MemberStatus;
import org.tedros.person.model.NaturalStatus;
import org.tedros.person.model.PersonStatus;
import org.tedros.person.model.PhilanthropeStatus;
import org.tedros.person.model.VoluntaryStatus;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.util.TLoggerUtil;
import org.tedros.server.util.TServiceLocator;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code ListPersonStatusFunction} do FE
 * (app-person-fx) — name, description e strings de resultado copiados
 * literalmente; controller seguro via lookup JNDI server-side com o token do
 * request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class ListPersonStatusFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(ListPersonStatusFunction.class);

	public static final String NAME = "list_statuses_person";
	public static final String DESCRIPTION = "List all statuses that a person can receive";

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
		return ClassificationParam.class;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		ClassificationParam v = (ClassificationParam) arg;

		LOGGER.info("Listing all person statuses with classification: {}", v.getClassification());

		TSelect sel = null;
		if (v.getClassification() == null)
			sel = new TSelect(PersonStatus.class);
		else {
			switch (v.getClassification()) {
			case ALL:
				sel = new TSelect(PersonStatus.class);
				break;
			case CLIENT_COMPANY:
				sel = new TSelect(ClientCompanyStatus.class);
				break;
			case CUSTOMER:
				sel = new TSelect(CustomerStatus.class);
				break;
			case EMPLOYEE:
				sel = new TSelect(EmployeeStatus.class);
				break;
			case LEGAL_PERSON:
				sel = new TSelect(LegalStatus.class);
				break;
			case MEMBER:
				sel = new TSelect(MemberStatus.class);
				break;
			case NATURAL_PERSON:
				sel = new TSelect(NaturalStatus.class);
				break;
			case PHILANTHROPE:
				sel = new TSelect(PhilanthropeStatus.class);
				break;
			case VOLUNTARY:
				sel = new TSelect(VoluntaryStatus.class);
				break;
			default:
				break;
			}
		}

		try {
			TResult<List<PersonStatus>> res = search(ctx.getToken(), sel);
			if (res.getState().equals(TState.SUCCESS)) {
				if (res.getValue().isEmpty()) {
					return Map.of(
							STATUS, ERROR,
							ACTION, "no_person_statuses_found",
							ERROR_MESSAGE, "No person statuses available in the system.");
				}

				return Map.of(
						STATUS, SUCCESS,
						ACTION, "person_statuses_listed",
						SYSTEM_INSTRUCTION, "Person statuses listed successfully. "
								+ "Do not retry again. Proceed with the user's request.",
						"person_statuses", res.getValue());
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "person_statuses_list_error",
					ERROR_MESSAGE, safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "no_person_statuses_found",
				ERROR_MESSAGE, "No person statuses available in the system.");
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<PersonStatus>> search(TAccessToken token, TSelect<PersonStatus> sel) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IPersonStatusController controller = serv.lookupWithRetry(IPersonStatusController.JNDI_NAME);
			return controller.search(token, sel);
		} finally {
			serv.close();
		}
	}
}
