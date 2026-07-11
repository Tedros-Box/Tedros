package org.tedros.ai.toolrelay.function.samples;

import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ACTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR_MESSAGE;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.STATUS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SUCCESS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SYSTEM_INSTRUCTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionResults.safeMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.model.Empty;
import org.tedros.sample.ejb.controller.IProductPriceController;
import org.tedros.sample.entity.ProductPrice;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.service.TServiceLocator;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code ListProductPriceAiFunction} do FE
 * (app-samples-fx) — name, description e strings de resultado copiados
 * literalmente; controller seguro via lookup JNDI server-side com o token do
 * request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class ListProductPriceAiFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(ListProductPriceAiFunction.class);

	public static final String NAME = "list_products_price";
	public static final String DESCRIPTION = "Lists all products prices";

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
		LOGGER.info("Listing all products prices");

		try {
			TResult<List<ProductPrice>> res = listAll(ctx.getToken());

			if (res.getState().equals(TState.SUCCESS) && !res.getValue().isEmpty()) {
				List<Price> lst = new ArrayList<>();
				res.getValue().forEach(p -> lst.add(new Price(p)));
				return Map.of(
						STATUS, SUCCESS,
						ACTION, "product_prices_listed",
						SYSTEM_INSTRUCTION, "Product prices listed successfully. "
								+ "Do not retry again. Proceed with the user's request.",
						"product_prices", lst);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "product_price_list_error",
					ERROR_MESSAGE, safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "no_product_prices_found",
				ERROR_MESSAGE, "No product prices available in the system.");
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<ProductPrice>> listAll(TAccessToken token) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IProductPriceController controller = serv.lookupWithRetry(IProductPriceController.JNDI_NAME);
			return controller.listAll(token, ProductPrice.class);
		} finally {
			serv.close();
		}
	}
}
