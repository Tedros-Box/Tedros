package org.tedros.ai.toolrelay.function.person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.ai.toolrelay.function.TServerFunctionConstants;
import org.tedros.core.security.model.TUser;
import org.tedros.person.model.Employee;
import org.tedros.person.model.Person;
import org.tedros.person.model.PersonCategory;
import org.tedros.person.model.PersonStatus;
import org.tedros.person.model.PersonType;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;

/**
 * Testes das tools Person migradas: nomes identicos aos do FE, sucesso,
 * resultado vazio (erro "none found" do FE), falha do lookup e propagacao do
 * token do turno — controllers stubados (sem JNDI).
 */
public class PersonFunctionsTest {

	private static final TAccessToken TOKEN = new TAccessToken("tk-1");

	private static TAiToolContext ctx() {
		TUser user = new TUser("Davis");
		user.setId(1L);
		return new TAiToolContext(user, TOKEN);
	}

	// ---------------------------------------------------------------- stubs

	static class StubListEmployees extends ListEmployeesFunction {
		TResult<List<Employee>> result;
		Exception toThrow;
		volatile TAccessToken tokenUsed;

		@Override
		protected TResult<List<Employee>> listAll(TAccessToken token) throws Exception {
			this.tokenUsed = token;
			if (toThrow != null)
				throw toThrow;
			return result;
		}
	}

	static class StubSearchEmployee extends SearchEmployeeFunction {
		TResult<List<Employee>> result;
		volatile TAccessToken tokenUsed;

		@Override
		protected TResult<List<Employee>> search(TAccessToken token, TSelect<Employee> sel) {
			this.tokenUsed = token;
			return result;
		}
	}

	static class StubSearchPerson extends SearchPersonFunction {
		TResult<List<Person>> result;
		volatile TSelect<Person> selectUsed;

		@Override
		protected TResult<List<Person>> search(TAccessToken token, TSelect<Person> sel) {
			this.selectUsed = sel;
			return result;
		}
	}

	static class StubListCategories extends ListPersonCategoryFunction {
		TResult<List<PersonCategory>> result;

		@Override
		protected TResult<List<PersonCategory>> listAll(TAccessToken token) {
			return result;
		}
	}

	static class StubListStatuses extends ListPersonStatusFunction {
		TResult<List<PersonStatus>> result;

		@Override
		protected TResult<List<PersonStatus>> search(TAccessToken token, TSelect<PersonStatus> sel) {
			return result;
		}
	}

	static class StubListTypes extends ListPersonTypeFunction {
		TResult<List<PersonType>> result;

		@Override
		protected TResult<List<PersonType>> search(TAccessToken token, TSelect<PersonType> sel) {
			return result;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> asMap(Object result) {
		assertTrue(result instanceof Map);
		return (Map<String, Object>) result;
	}

	// ---------------------------------------------------------------- tests

	@Test
	public void namesMatchFrontendAndRegisterWithoutCollision() {
		TServerFunctionCatalog catalog = new TServerFunctionCatalog(List.<TServerAiFunction>of(
				new ListEmployeesFunction(), new SearchEmployeeFunction(), new SearchPersonFunction(),
				new ListPersonCategoryFunction(), new ListPersonStatusFunction(), new ListPersonTypeFunction()));
		assertEquals(6, catalog.toolSpecifications().size());
		for (String name : List.of("list_all_employees", "search_employees", "search_person",
				"list_categories_person", "list_statuses_person", "list_person_types"))
			assertTrue("missing tool " + name, catalog.contains(name));
	}

	@Test
	public void listEmployeesSuccessEmptyAndFailure() {
		StubListEmployees fn = new StubListEmployees();

		fn.result = new TResult<>(TState.SUCCESS, List.of(new Employee()));
		Map<String, Object> ok = asMap(fn.execute(new Object(), ctx()));
		assertEquals("employees_found", ok.get(TServerFunctionConstants.ACTION));
		assertEquals(TOKEN, fn.tokenUsed);

		fn.result = new TResult<>(TState.SUCCESS, List.of());
		Map<String, Object> empty = asMap(fn.execute(new Object(), ctx()));
		assertEquals("none_employee_found", empty.get(TServerFunctionConstants.ACTION));

		fn.toThrow = new IllegalStateException("jndi down");
		Map<String, Object> err = asMap(fn.execute(new Object(), ctx()));
		assertEquals("employee_listing_failed", err.get(TServerFunctionConstants.ACTION));
		assertEquals("Error listing employees: jndi down", err.get(TServerFunctionConstants.ERROR_MESSAGE));
	}

	@Test
	public void listEmployeesNonSuccessStateFallsBackToNoneFound() {
		StubListEmployees fn = new StubListEmployees();
		fn.result = new TResult<>(TState.ERROR, "denied");
		Map<String, Object> result = asMap(fn.execute(new Object(), ctx()));
		// mesmo comportamento do FE: estado != SUCCESS cai no "none found"
		assertEquals("none_employees_found", result.get(TServerFunctionConstants.ACTION));
	}

	@Test
	public void searchEmployeesSuccessAndEmpty() {
		StubSearchEmployee fn = new StubSearchEmployee();

		fn.result = new TResult<>(TState.SUCCESS, List.of(new Employee()));
		SearchEmployee criteria = new SearchEmployee("davis", null);
		Map<String, Object> ok = asMap(fn.execute(criteria, ctx()));
		assertEquals("employee_found", ok.get(TServerFunctionConstants.ACTION));
		assertEquals(TOKEN, fn.tokenUsed);

		fn.result = new TResult<>(TState.SUCCESS, List.of());
		Map<String, Object> empty = asMap(fn.execute(criteria, ctx()));
		assertEquals("no_employee_found", empty.get(TServerFunctionConstants.ACTION));
	}

	@Test
	public void searchPersonBuildsSelectAndReturnsPersons() {
		StubSearchPerson fn = new StubSearchPerson();
		fn.result = new TResult<>(TState.SUCCESS, List.of(new Person()));

		Search criteria = new Search();
		criteria.setName("Maria");
		criteria.setClassification(Classification.NATURAL_PERSON);
		Map<String, Object> ok = asMap(fn.execute(criteria, ctx()));

		assertEquals("persons_found", ok.get(TServerFunctionConstants.ACTION));
		assertTrue(ok.containsKey("persons"));
		assertTrue(fn.selectUsed != null);
	}

	@Test
	public void listCategoriesStatusesAndTypes() {
		StubListCategories cat = new StubListCategories();
		cat.result = new TResult<>(TState.SUCCESS, List.of(new PersonCategory()));
		assertEquals("person_categories_listed",
				asMap(cat.execute(new Object(), ctx())).get(TServerFunctionConstants.ACTION));

		cat.result = new TResult<>(TState.SUCCESS, List.of());
		assertEquals("no_person_categories_found",
				asMap(cat.execute(new Object(), ctx())).get(TServerFunctionConstants.ACTION));

		StubListStatuses st = new StubListStatuses();
		st.result = new TResult<>(TState.SUCCESS, List.of(new PersonStatus()));
		assertEquals("person_statuses_listed",
				asMap(st.execute(new ClassificationParam(Classification.EMPLOYEE), ctx()))
						.get(TServerFunctionConstants.ACTION));

		StubListTypes tp = new StubListTypes();
		tp.result = new TResult<>(TState.SUCCESS, List.of(new PersonType()));
		assertEquals("person_types_listed",
				asMap(tp.execute(new ClassificationParam(null), ctx()))
						.get(TServerFunctionConstants.ACTION));
	}
}
