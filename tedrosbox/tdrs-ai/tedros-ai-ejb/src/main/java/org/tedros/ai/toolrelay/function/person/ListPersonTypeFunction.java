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
import org.tedros.person.ejb.controller.IPersonTypeController;
import org.tedros.person.model.ClientCompanyType;
import org.tedros.person.model.CustomerType;
import org.tedros.person.model.LegalType;
import org.tedros.person.model.MemberType;
import org.tedros.person.model.NaturalType;
import org.tedros.person.model.PersonType;
import org.tedros.person.model.PhilanthropeType;
import org.tedros.person.model.StaffType;
import org.tedros.person.model.VoluntaryType;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.service.TServiceLocator;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code ListPersonTypeFunction} do FE (app-person-fx) —
 * name, description e strings de resultado copiados literalmente; controller
 * seguro via lookup JNDI server-side com o token do request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class ListPersonTypeFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(ListPersonTypeFunction.class);

	public static final String NAME = "list_person_types";
	public static final String DESCRIPTION = "List all types that can be given to a person";

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

		LOGGER.info("Listing all person types with classification: {}", v.getClassification());

		TSelect sel = null;
		if (v.getClassification() == null)
			sel = new TSelect(PersonType.class);
		else {
			switch (v.getClassification()) {
			case ALL:
				sel = new TSelect(PersonType.class);
				break;
			case CLIENT_COMPANY:
				sel = new TSelect(ClientCompanyType.class);
				break;
			case CUSTOMER:
				sel = new TSelect(CustomerType.class);
				break;
			case EMPLOYEE:
				sel = new TSelect(StaffType.class);
				break;
			case LEGAL_PERSON:
				sel = new TSelect(LegalType.class);
				break;
			case MEMBER:
				sel = new TSelect(MemberType.class);
				break;
			case NATURAL_PERSON:
				sel = new TSelect(NaturalType.class);
				break;
			case PHILANTHROPE:
				sel = new TSelect(PhilanthropeType.class);
				break;
			case VOLUNTARY:
				sel = new TSelect(VoluntaryType.class);
				break;
			default:
				break;
			}
		}

		try {
			TResult<List<PersonType>> res = search(ctx.getToken(), sel);
			if (res.getState().equals(TState.SUCCESS)) {
				if (res.getValue().isEmpty()) {
					return Map.of(
							STATUS, ERROR,
							ACTION, "no_person_types_found",
							ERROR_MESSAGE, "No person types available in the system.");
				}

				return Map.of(
						STATUS, SUCCESS,
						ACTION, "person_types_listed",
						SYSTEM_INSTRUCTION, "Person types listed successfully. "
								+ "Do not retry again. Proceed with the user's request.",
						"person_types", res.getValue());
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "person_types_list_error",
					ERROR_MESSAGE, safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "no_person_types_found",
				ERROR_MESSAGE, "No person types available in the system.");
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<PersonType>> search(TAccessToken token, TSelect<PersonType> sel) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IPersonTypeController controller = serv.lookupWithRetry(IPersonTypeController.JNDI_NAME);
			return controller.search(token, sel);
		} finally {
			serv.close();
		}
	}
}
