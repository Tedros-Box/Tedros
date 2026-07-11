package org.tedros.ai.toolrelay.function.itsupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.ai.toolrelay.function.TServerFunctionConstants;
import org.tedros.ai.toolrelay.function.itsupport.model.GmudFilterModel;
import org.tedros.core.security.model.TUser;
import org.tedros.it.tools.entity.Gmud;
import org.tedros.it.tools.model.ProductivityActivityDTO;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;

/**
 * Testes das consultas BE do itsupport migradas: validacao de criterios,
 * agregacao de atividades, propagacao do token do turno e tratamento de
 * TResult de erro — controller stubado (sem JNDI).
 */
public class ItSupportFunctionsTest {

	private static final TAccessToken TOKEN = new TAccessToken("tk-1");

	private static TAiToolContext ctx() {
		TUser user = new TUser("Davis");
		user.setId(1L);
		return new TAiToolContext(user, TOKEN);
	}

	// ---------------------------------------------------------------- fakes

	static class StubActivities extends GetEmployeeActivitiesAIFunction {
		TResult<List<ProductivityActivityDTO>> result;
		volatile TAccessToken tokenUsed;

		@Override
		protected TResult<List<ProductivityActivityDTO>> findActivities(TAccessToken token, Long userId,
				LocalDateTime start, LocalDateTime end) {
			this.tokenUsed = token;
			return result;
		}
	}

	static class StubGmuds extends SearchGmudAIFunction {
		TResult<List<Gmud>> result;
		volatile TAccessToken tokenUsed;
		volatile TSelect<Gmud> selectUsed;

		@Override
		protected TResult<List<Gmud>> search(TAccessToken token, TSelect<Gmud> select) {
			this.tokenUsed = token;
			this.selectUsed = select;
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
				new GetEmployeeActivitiesAIFunction(), new SearchGmudAIFunction()));
		assertTrue(catalog.contains("get_employee_activities"));
		assertTrue(catalog.contains("search_gmuds"));
		assertEquals(2, catalog.toolSpecifications().size());
	}

	@Test
	public void activitiesInvalidUserIdAndMissingDateAreRejected() {
		StubActivities fn = new StubActivities();

		Map<String, Object> r1 = asMap(fn.execute(new EmployeeActivityCriteria(null, "2026-07-01", null), ctx()));
		assertEquals("employee_activities_invalid_user_id", r1.get(TServerFunctionConstants.ACTION));

		Map<String, Object> r2 = asMap(fn.execute(new EmployeeActivityCriteria(5L, " ", null), ctx()));
		assertEquals("employee_activities_missing_start_date", r2.get(TServerFunctionConstants.ACTION));

		Map<String, Object> r3 = asMap(fn.execute(new EmployeeActivityCriteria(5L, "01/07/2026", null), ctx()));
		assertEquals("employee_activities_invalid_date", r3.get(TServerFunctionConstants.ACTION));
	}

	@Test
	public void activitiesSuccessAggregatesByDayAndAppUsingCurrentToken() {
		StubActivities fn = new StubActivities();

		ProductivityActivityDTO a = new ProductivityActivityDTO();
		a.setTimestamp(LocalDateTime.of(2026, 7, 1, 9, 0));
		a.setActiveWindowTitle("Eclipse");
		a.setMouseEventCount(10L);
		a.setKeyboardEventCount(20L);
		ProductivityActivityDTO b = new ProductivityActivityDTO();
		b.setTimestamp(LocalDateTime.of(2026, 7, 1, 10, 0));
		b.setActiveWindowTitle("Eclipse");
		b.setMouseEventCount(5L);
		b.setKeyboardEventCount(5L);

		fn.result = new TResult<>(TState.SUCCESS, List.of(a, b));

		Map<String, Object> result = asMap(fn.execute(
				new EmployeeActivityCriteria(5L, "2026-07-01", null), ctx()));

		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals("employee_activities_retrieved", result.get(TServerFunctionConstants.ACTION));
		assertEquals(1, result.get("days_analyzed"));
		@SuppressWarnings("unchecked")
		List<DailyActivitySummary> summaries = (List<DailyActivitySummary>) result.get("employee_activities_summary");
		assertEquals(1, summaries.size());
		assertEquals("2026-07-01", summaries.get(0).getDate());
		assertEquals(15L, summaries.get(0).getTotalMouseEvents());
		assertEquals(25L, summaries.get(0).getTotalKeyboardEvents());
		// token do request corrente foi usado na chamada segura
		assertEquals(TOKEN, fn.tokenUsed);
	}

	@Test
	public void activitiesControllerErrorStateIsReported() {
		StubActivities fn = new StubActivities();
		fn.result = new TResult<>(TState.ERROR, "access denied");

		Map<String, Object> result = asMap(fn.execute(
				new EmployeeActivityCriteria(5L, "2026-07-01", null), ctx()));

		assertEquals(TServerFunctionConstants.ERROR, result.get(TServerFunctionConstants.STATUS));
		assertEquals("employee_activities_" + TState.ERROR.name().toLowerCase(),
				result.get(TServerFunctionConstants.ACTION));
		assertEquals("access denied", result.get(TServerFunctionConstants.ERROR_MESSAGE));
	}

	@Test
	public void gmudsSuccessWithEmptyResultUsingCurrentToken() {
		StubGmuds fn = new StubGmuds();
		fn.result = new TResult<>(TState.SUCCESS, List.of());

		Map<String, Object> result = asMap(fn.execute(new GmudFilterModel(), ctx()));

		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals("gmuds_retrieved", result.get(TServerFunctionConstants.ACTION));
		assertNotNull(result.get("gmuds"));
		assertEquals(TOKEN, fn.tokenUsed);
		assertNotNull(fn.selectUsed);
	}

	@Test
	public void gmudsControllerErrorAndNullFilterAreHandled() {
		StubGmuds fn = new StubGmuds();
		fn.result = new TResult<>(TState.ERROR, "boom");

		Map<String, Object> err = asMap(fn.execute(new GmudFilterModel(), ctx()));
		assertEquals("gmuds_retrieval_" + TState.ERROR.name().toLowerCase(),
				err.get(TServerFunctionConstants.ACTION));
		assertEquals("boom", err.get(TServerFunctionConstants.ERROR_MESSAGE));

		Map<String, Object> noCriteria = asMap(fn.execute(null, ctx()));
		assertEquals("gmuds_retrieval_no_criteria", noCriteria.get(TServerFunctionConstants.ACTION));
	}
}
