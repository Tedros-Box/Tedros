package org.tedros.ai.toolrelay.function.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.TServerFunctionCatalog;
import org.tedros.ai.toolrelay.function.TServerFunctionConstants;
import org.tedros.ai.toolrelay.function.extension.SearchForDocumentsFunction;
import org.tedros.ai.toolrelay.function.extension.model.DocumentSearchParam;
import org.tedros.ai.toolrelay.function.samples.ListProductPriceAiFunction;
import org.tedros.ai.toolrelay.function.services.ListServicesFunction;
import org.tedros.ai.toolrelay.function.stock.FindProductFunction;
import org.tedros.ai.toolrelay.function.stock.ProductParam;
import org.tedros.core.security.model.TUser;
import org.tedros.extension.model.Document;
import org.tedros.sample.entity.ProductPrice;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.services.model.Service;
import org.tedros.stock.entity.Product;

/**
 * Testes das tools Extensions/Services/Stock/Samples migradas: nomes
 * identicos aos do FE, sucesso, vazio, falha do lookup e propagacao do token
 * do turno — controllers stubados (sem JNDI).
 */
public class MiscFunctionsTest {

	private static final TAccessToken TOKEN = new TAccessToken("tk-1");

	private static TAiToolContext ctx() {
		TUser user = new TUser("Davis");
		user.setId(1L);
		return new TAiToolContext(user, TOKEN);
	}

	// ---------------------------------------------------------------- stubs

	static class StubDocuments extends SearchForDocumentsFunction {
		TResult<List<Document>> result;
		volatile TAccessToken tokenUsed;

		@Override
		protected TResult<List<Document>> search(TAccessToken token, TSelect<Document> select) {
			this.tokenUsed = token;
			return result;
		}
	}

	static class StubServices extends ListServicesFunction {
		TResult<List<Service>> result;
		Exception toThrow;

		@Override
		protected TResult<List<Service>> listAll(TAccessToken token) throws Exception {
			if (toThrow != null)
				throw toThrow;
			return result;
		}
	}

	static class StubProducts extends FindProductFunction {
		TResult<List<Product>> result;
		volatile TAccessToken tokenUsed;

		@Override
		protected TResult<List<Product>> search(TAccessToken token, TSelect<Product> sel) {
			this.tokenUsed = token;
			return result;
		}
	}

	static class StubPrices extends ListProductPriceAiFunction {
		TResult<List<ProductPrice>> result;

		@Override
		protected TResult<List<ProductPrice>> listAll(TAccessToken token) {
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
				new SearchForDocumentsFunction(), new ListServicesFunction(),
				new FindProductFunction(), new ListProductPriceAiFunction()));
		assertEquals(4, catalog.toolSpecifications().size());
		for (String name : List.of("search_for_documents", "list_services",
				"search_products", "list_products_price"))
			assertTrue("missing tool " + name, catalog.contains(name));
	}

	@Test
	public void searchDocumentsSuccessEmptyAndTokenPropagation() {
		StubDocuments fn = new StubDocuments();

		fn.result = new TResult<>(TState.SUCCESS, List.of(new Document()));
		DocumentSearchParam param = new DocumentSearchParam(null, null, "manual");
		Map<String, Object> ok = asMap(fn.execute(param, ctx()));
		assertEquals("documents_found", ok.get(TServerFunctionConstants.ACTION));
		assertTrue(ok.containsKey("documents"));
		assertEquals(TOKEN, fn.tokenUsed);

		fn.result = new TResult<>(TState.SUCCESS, List.of());
		Map<String, Object> empty = asMap(fn.execute(param, ctx()));
		assertEquals("no_documents_found", empty.get(TServerFunctionConstants.ACTION));
	}

	@Test
	public void listServicesSuccessErrorAndNullValue() {
		StubServices fn = new StubServices();

		fn.result = new TResult<>(TState.SUCCESS, List.of(new Service()));
		assertEquals("services_listed",
				asMap(fn.execute(new Object(), ctx())).get(TServerFunctionConstants.ACTION));

		// FE: getValue() null cai no "no services found"
		fn.result = new TResult<>(TState.SUCCESS);
		assertEquals("no_services_found",
				asMap(fn.execute(new Object(), ctx())).get(TServerFunctionConstants.ACTION));

		fn.toThrow = new IllegalStateException("jndi down");
		Map<String, Object> err = asMap(fn.execute(new Object(), ctx()));
		assertEquals("services_list_error", err.get(TServerFunctionConstants.ACTION));
		assertEquals("jndi down", err.get(TServerFunctionConstants.ERROR_MESSAGE));
	}

	@Test
	public void findProductsSuccessMapsToProductParamAndEmpty() {
		StubProducts fn = new StubProducts();

		Product p = new Product();
		p.setCode("P1");
		p.setName("Parafuso");
		fn.result = new TResult<>(TState.SUCCESS, List.of(p));

		ProductParam criteria = new ProductParam();
		criteria.setName("parafuso");
		Map<String, Object> ok = asMap(fn.execute(criteria, ctx()));
		assertEquals("products_found", ok.get(TServerFunctionConstants.ACTION));
		@SuppressWarnings("unchecked")
		List<ProductParam> products = (List<ProductParam>) ok.get("products");
		assertEquals(1, products.size());
		assertEquals("P1", products.get(0).getCode());
		assertEquals(TOKEN, fn.tokenUsed);

		fn.result = new TResult<>(TState.SUCCESS, List.of());
		Map<String, Object> empty = asMap(fn.execute(criteria, ctx()));
		assertEquals("no_products_found", empty.get(TServerFunctionConstants.ACTION));
	}

	@Test
	public void listProductPricesEmptyFallsBackToNotFound() {
		StubPrices fn = new StubPrices();
		fn.result = new TResult<>(TState.SUCCESS, List.of());
		assertEquals("no_product_prices_found",
				asMap(fn.execute(new Object(), ctx())).get(TServerFunctionConstants.ACTION));
	}
}
