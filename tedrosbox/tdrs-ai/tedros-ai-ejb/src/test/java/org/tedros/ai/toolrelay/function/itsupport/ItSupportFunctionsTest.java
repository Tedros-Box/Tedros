package org.tedros.ai.toolrelay.function.itsupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.ai.toolrelay.function.TServerFunctionConstants;
import org.tedros.ai.toolrelay.function.itsupport.model.CatalogServiceModel;
import org.tedros.ai.toolrelay.function.itsupport.model.GmudFilterModel;
import org.tedros.ai.toolrelay.function.itsupport.model.ServiceCatalogFilterModel;
import org.tedros.ai.toolrelay.function.itsupport.model.ServiceCatalogModel;
import org.tedros.ai.toolrelay.function.itsupport.model.ServiceGroupModel;
import org.tedros.ai.toolrelay.function.itsupport.model.ServiceVariantModel;
import org.tedros.core.security.model.TUser;
import org.tedros.it.tools.entity.CatalogService;
import org.tedros.it.tools.entity.Gmud;
import org.tedros.it.tools.entity.ServiceCatalog;
import org.tedros.it.tools.entity.ServiceGroup;
import org.tedros.it.tools.entity.ServiceVariant;
import org.tedros.it.tools.model.ProductivityActivityDTO;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;

/**
 * Testes das consultas BE do itsupport migradas: validacao de criterios,
 * agregacao de atividades, consulta e filtragem do catalogo de servicos,
 * propagacao do token do turno e tratamento de TResult de erro — controller stubado (sem JNDI).
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

	static class StubServiceCatalog extends SearchServiceCatalogAiFunction {
		TResult<List<ServiceCatalog>> result;
		volatile TAccessToken tokenUsed;

		@Override
		protected TResult<List<ServiceCatalog>> search(TAccessToken token) {
			this.tokenUsed = token;
			return result;
		}
	}

	private static ServiceCatalog createSampleCatalog() {
		ServiceCatalog catalog = new ServiceCatalog();
		catalog.setName("Catálogo Padrão TI");

		// Group 1: Codificação
		ServiceGroup g1 = new ServiceGroup();
		g1.setName("Codificação");
		g1.setCatalog(catalog);

		// Service 1.1: Backend Development
		CatalogService s1 = new CatalogService();
		s1.setNumber(1);
		s1.setName("Desenvolvimento Backend");
		s1.setGroup(g1);

		ServiceVariant v1a = new ServiceVariant();
		v1a.setVariantId("a");
		v1a.setComplexity("Baixa");
		v1a.setScope("Criação de CRUD simples");
		v1a.setEstimatedHours(8.0);
		v1a.setDeliverables("Código fonte e testes");
		v1a.setRequiredProfiles("Desenvolvedor Jr");
		v1a.setActivitiesPerformed("Codificação e commit");
		v1a.setService(s1);

		ServiceVariant v1b = new ServiceVariant();
		v1b.setVariantId("b");
		v1b.setComplexity("Alta");
		v1b.setScope("Arquitetura de microsserviços");
		v1b.setEstimatedHours(40.0);
		v1b.setDeliverables("Arquitetura e endpoints");
		v1b.setRequiredProfiles("Arquiteto / Dev Senior");
		v1b.setActivitiesPerformed("Design de arquitetura e implementação");
		v1b.setService(s1);

		s1.setVariants(new ArrayList<>(List.of(v1a, v1b)));

		// Service 1.2: Frontend Development
		CatalogService s2 = new CatalogService();
		s2.setNumber(2);
		s2.setName("Desenvolvimento Frontend");
		s2.setGroup(g1);

		ServiceVariant v2a = new ServiceVariant();
		v2a.setVariantId("a");
		v2a.setComplexity("Média");
		v2a.setScope("Telas e componentes JavaFX");
		v2a.setEstimatedHours(16.0);
		v2a.setDeliverables("Telas funcionais");
		v2a.setRequiredProfiles("Desenvolvedor Pleno");
		v2a.setActivitiesPerformed("Construção de views");
		v2a.setService(s2);

		s2.setVariants(new ArrayList<>(List.of(v2a)));

		g1.setServices(new ArrayList<>(List.of(s1, s2)));

		// Group 2: Testes
		ServiceGroup g2 = new ServiceGroup();
		g2.setName("Testes e Qualidade");
		g2.setCatalog(catalog);

		CatalogService s3 = new CatalogService();
		s3.setNumber(3);
		s3.setName("Testes Automatizados");
		s3.setGroup(g2);

		ServiceVariant v3a = new ServiceVariant();
		v3a.setVariantId("a");
		v3a.setComplexity("Única");
		v3a.setScope("Elaboração de testes E2E");
		v3a.setEstimatedHours(24.0);
		v3a.setDeliverables("Relatório de testes e pipeline");
		v3a.setRequiredProfiles("QA Engineer");
		v3a.setActivitiesPerformed("Criação de cenários de teste");
		v3a.setService(s3);

		s3.setVariants(new ArrayList<>(List.of(v3a)));
		g2.setServices(new ArrayList<>(List.of(s3)));

		catalog.setGroups(new ArrayList<>(List.of(g1, g2)));
		return catalog;
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
				new GetEmployeeActivitiesAIFunction(), new SearchGmudAIFunction(), new SearchServiceCatalogAiFunction()));
		assertTrue(catalog.contains("get_employee_activities"));
		assertTrue(catalog.contains("search_gmuds"));
		assertTrue(catalog.contains("search_service_catalog"));
		assertEquals(3, catalog.toolSpecifications().size());
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

	// ---------------------------------------------------------------- Service Catalog Tests

	@Test
	public void serviceCatalogSuccessWithoutFilterReturnsFullStructure() {
		StubServiceCatalog fn = new StubServiceCatalog();
		fn.result = new TResult<>(TState.SUCCESS, List.of(createSampleCatalog()));

		Map<String, Object> result = asMap(fn.execute(new ServiceCatalogFilterModel(), ctx()));

		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		assertEquals("service_catalog_retrieved", result.get(TServerFunctionConstants.ACTION));
		assertEquals(TOKEN, fn.tokenUsed);

		@SuppressWarnings("unchecked")
		List<ServiceCatalogModel> catalogs = (List<ServiceCatalogModel>) result.get("catalogs");
		assertNotNull(catalogs);
		assertEquals(1, catalogs.size());
		ServiceCatalogModel cat = catalogs.get(0);
		assertEquals("Catálogo Padrão TI", cat.getName());
		assertEquals(2, cat.getGroups().size());

		// Group 1: Codificação com 2 serviços e 3 variantes no total
		ServiceGroupModel g1 = cat.getGroups().get(0);
		assertEquals("Codificação", g1.getName());
		assertEquals(2, g1.getServices().size());
		assertEquals(2, g1.getServices().get(0).getVariants().size());
		assertEquals(1, g1.getServices().get(1).getVariants().size());

		// Group 2: Testes com 1 serviço e 1 variante
		ServiceGroupModel g2 = cat.getGroups().get(1);
		assertEquals("Testes e Qualidade", g2.getName());
		assertEquals(1, g2.getServices().size());
		assertEquals(1, g2.getServices().get(0).getVariants().size());
	}

	@Test
	public void serviceCatalogFilterByCategory() {
		StubServiceCatalog fn = new StubServiceCatalog();
		fn.result = new TResult<>(TState.SUCCESS, List.of(createSampleCatalog()));

		ServiceCatalogFilterModel filter = ServiceCatalogFilterModel.builder()
				.categoryName("testes")
				.build();

		Map<String, Object> result = asMap(fn.execute(filter, ctx()));
		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));

		@SuppressWarnings("unchecked")
		List<ServiceCatalogModel> catalogs = (List<ServiceCatalogModel>) result.get("catalogs");
		assertEquals(1, catalogs.size());
		ServiceCatalogModel cat = catalogs.get(0);
		assertEquals(1, cat.getGroups().size());
		assertEquals("Testes e Qualidade", cat.getGroups().get(0).getName());
	}

	@Test
	public void serviceCatalogFilterByServiceNumberAndName() {
		StubServiceCatalog fn = new StubServiceCatalog();
		fn.result = new TResult<>(TState.SUCCESS, List.of(createSampleCatalog()));

		ServiceCatalogFilterModel filter = ServiceCatalogFilterModel.builder()
				.serviceNumber(1)
				.serviceName("backend")
				.build();

		Map<String, Object> result = asMap(fn.execute(filter, ctx()));
		@SuppressWarnings("unchecked")
		List<ServiceCatalogModel> catalogs = (List<ServiceCatalogModel>) result.get("catalogs");
		assertEquals(1, catalogs.size());
		ServiceGroupModel group = catalogs.get(0).getGroups().get(0);
		assertEquals(1, group.getServices().size());
		assertEquals("Desenvolvimento Backend", group.getServices().get(0).getName());
		assertEquals(Integer.valueOf(1), group.getServices().get(0).getNumber());
		assertEquals(2, group.getServices().get(0).getVariants().size());
	}

	@Test
	public void serviceCatalogFilterByVariantAndComplexity() {
		StubServiceCatalog fn = new StubServiceCatalog();
		fn.result = new TResult<>(TState.SUCCESS, List.of(createSampleCatalog()));

		ServiceCatalogFilterModel filter = ServiceCatalogFilterModel.builder()
				.variantId("a")
				.complexity("Baixa")
				.build();

		Map<String, Object> result = asMap(fn.execute(filter, ctx()));
		@SuppressWarnings("unchecked")
		List<ServiceCatalogModel> catalogs = (List<ServiceCatalogModel>) result.get("catalogs");
		assertEquals(1, catalogs.size());
		ServiceGroupModel group = catalogs.get(0).getGroups().get(0);
		assertEquals(1, group.getServices().size());
		CatalogServiceModel svc = group.getServices().get(0);
		assertEquals("Desenvolvimento Backend", svc.getName());
		assertEquals(1, svc.getVariants().size());
		ServiceVariantModel variant = svc.getVariants().get(0);
		assertEquals("a", variant.getVariantId());
		assertEquals("Baixa", variant.getComplexity());
		assertEquals(Double.valueOf(8.0), variant.getEstimatedHours());
	}

	@Test
	public void serviceCatalogFilterByEstimatedHoursRange() {
		StubServiceCatalog fn = new StubServiceCatalog();
		fn.result = new TResult<>(TState.SUCCESS, List.of(createSampleCatalog()));

		ServiceCatalogFilterModel filter = ServiceCatalogFilterModel.builder()
				.minEstimatedHours(10.0)
				.maxEstimatedHours(30.0)
				.build();

		Map<String, Object> result = asMap(fn.execute(filter, ctx()));
		@SuppressWarnings("unchecked")
		List<ServiceCatalogModel> catalogs = (List<ServiceCatalogModel>) result.get("catalogs");
		assertEquals(1, catalogs.size());
		ServiceCatalogModel cat = catalogs.get(0);
		// Group 1 should have frontend (16h), Group 2 should have QA (24h)
		assertEquals(2, cat.getGroups().size());

		ServiceGroupModel g1 = cat.getGroups().get(0);
		assertEquals(1, g1.getServices().size());
		assertEquals("Desenvolvimento Frontend", g1.getServices().get(0).getName());
		assertEquals(Double.valueOf(16.0), g1.getServices().get(0).getVariants().get(0).getEstimatedHours());

		ServiceGroupModel g2 = cat.getGroups().get(1);
		assertEquals(1, g2.getServices().size());
		assertEquals("Testes Automatizados", g2.getServices().get(0).getName());
		assertEquals(Double.valueOf(24.0), g2.getServices().get(0).getVariants().get(0).getEstimatedHours());
	}

	@Test
	public void serviceCatalogFilterByTextualFields() {
		StubServiceCatalog fn = new StubServiceCatalog();
		fn.result = new TResult<>(TState.SUCCESS, List.of(createSampleCatalog()));

		ServiceCatalogFilterModel filter = ServiceCatalogFilterModel.builder()
				.scope("microsserviços")
				.deliverables("endpoints")
				.requiredProfiles("Senior")
				.activitiesPerformed("Design")
				.build();

		Map<String, Object> result = asMap(fn.execute(filter, ctx()));
		@SuppressWarnings("unchecked")
		List<ServiceCatalogModel> catalogs = (List<ServiceCatalogModel>) result.get("catalogs");
		assertEquals(1, catalogs.size());
		ServiceGroupModel group = catalogs.get(0).getGroups().get(0);
		assertEquals(1, group.getServices().size());
		CatalogServiceModel svc = group.getServices().get(0);
		assertEquals("Desenvolvimento Backend", svc.getName());
		assertEquals(1, svc.getVariants().size());
		ServiceVariantModel variant = svc.getVariants().get(0);
		assertEquals("b", variant.getVariantId());
		assertEquals("Alta", variant.getComplexity());
		assertEquals(Double.valueOf(40.0), variant.getEstimatedHours());
	}

	@Test
	public void serviceCatalogNoMatchReturnsEmptyList() {
		StubServiceCatalog fn = new StubServiceCatalog();
		fn.result = new TResult<>(TState.SUCCESS, List.of(createSampleCatalog()));

		ServiceCatalogFilterModel filter = ServiceCatalogFilterModel.builder()
				.categoryName("Inexistente")
				.build();

		Map<String, Object> result = asMap(fn.execute(filter, ctx()));
		assertEquals(TServerFunctionConstants.SUCCESS, result.get(TServerFunctionConstants.STATUS));
		@SuppressWarnings("unchecked")
		List<ServiceCatalogModel> catalogs = (List<ServiceCatalogModel>) result.get("catalogs");
		assertNotNull(catalogs);
		assertTrue(catalogs.isEmpty());
	}

	@Test
	public void serviceCatalogControllerErrorAndExceptionHandled() {
		StubServiceCatalog fn = new StubServiceCatalog();
		fn.result = new TResult<>(TState.ERROR, "Database connection failed");

		Map<String, Object> err = asMap(fn.execute(new ServiceCatalogFilterModel(), ctx()));
		assertEquals(TServerFunctionConstants.ERROR, err.get(TServerFunctionConstants.STATUS));
		assertEquals("service_catalog_retrieval_" + TState.ERROR.name().toLowerCase(),
				err.get(TServerFunctionConstants.ACTION));
		assertEquals("Database connection failed", err.get(TServerFunctionConstants.ERROR_MESSAGE));

		// Exception handling
		StubServiceCatalog fnExc = new StubServiceCatalog() {
			@Override
			protected TResult<List<ServiceCatalog>> search(TAccessToken token) {
				throw new RuntimeException("EJB lookup failed");
			}
		};
		Map<String, Object> excResult = asMap(fnExc.execute(new ServiceCatalogFilterModel(), ctx()));
		assertEquals(TServerFunctionConstants.ERROR, excResult.get(TServerFunctionConstants.STATUS));
		assertEquals("service_catalog_retrieval_error", excResult.get(TServerFunctionConstants.ACTION));
		assertEquals("EJB lookup failed", excResult.get(TServerFunctionConstants.ERROR_MESSAGE));
	}
}
