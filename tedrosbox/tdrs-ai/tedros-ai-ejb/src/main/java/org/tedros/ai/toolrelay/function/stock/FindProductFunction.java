package org.tedros.ai.toolrelay.function.stock;

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

import org.apache.commons.lang3.StringUtils;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.server.query.TCompareOp;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.service.TServiceLocator;
import org.tedros.server.util.TLoggerUtil;
import org.tedros.stock.ejb.controller.IProductController;
import org.tedros.stock.entity.Product;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code FindProductFunction} do FE (app-stock-fx) —
 * name, description, montagem do TSelect e strings de resultado copiados
 * literalmente; controller seguro via lookup JNDI server-side com o token do
 * request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class FindProductFunction implements TServerAiFunction {

	private static final String NO_PRODUCTS_FOUND_MATCHING_THE_CRITERIA = "No products found matching the criteria.";

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(FindProductFunction.class);

	public static final String NAME = "search_products";
	public static final String DESCRIPTION = "Searches the product catalog/inventory using partial matches. ";

	private static final String INSERT_DATE = "insertDate";

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
		return ProductParam.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		ProductParam v = (ProductParam) arg;

		LOGGER.info("Searching Products with parameters: {}", v);

		TSelect<Product> sel = new TSelect<>(Product.class);

		if (StringUtils.isNotBlank(v.getCode()))
			sel.addOrCondition("code", TCompareOp.LIKE, v.getCode().trim().toLowerCase());

		if (StringUtils.isNotBlank(v.getName()))
			sel.addOrCondition("name", TCompareOp.LIKE, v.getName().trim().toLowerCase());

		if (StringUtils.isNotBlank(v.getDescription()))
			sel.addOrCondition("description", TCompareOp.LIKE, v.getDescription().trim().toLowerCase());

		if (StringUtils.isNotBlank(v.getTrademark()))
			sel.addOrCondition("trademark", TCompareOp.LIKE, v.getTrademark().trim().toLowerCase());

		if (StringUtils.isNotBlank(v.getUnitMeasure()))
			sel.addOrCondition("unitMeasure", TCompareOp.LIKE, v.getUnitMeasure().trim().toLowerCase());

		if (v.getMeasure() != null)
			sel.addOrCondition("measure", TCompareOp.EQUAL, v.getMeasure());

		if (v.getBeginDate() != null) {
			if (v.getBeginDate() != null && v.getEndDate() == null)
				sel.addOrCondition(INSERT_DATE, TCompareOp.EQUAL, v.getBeginDate());
			else {
				sel.addOrCondition(INSERT_DATE, TCompareOp.GREATER_EQ_THAN, v.getBeginDate());
				sel.addAndCondition(INSERT_DATE, TCompareOp.LESS_THAN, v.getEndDate());
			}
		} else if (v.getEndDate() != null)
			sel.addOrCondition(INSERT_DATE, TCompareOp.LESS_EQ_THAN, v.getEndDate());

		try {
			TResult<List<Product>> res = search(ctx.getToken(), sel);
			if (res.getState().equals(TState.SUCCESS)) {
				if (res.getValue().isEmpty()) {
					return Map.of(
							STATUS, ERROR,
							ACTION, "no_products_found",
							ERROR_MESSAGE, NO_PRODUCTS_FOUND_MATCHING_THE_CRITERIA);
				}

				List<ProductParam> lst = new ArrayList<>();
				res.getValue().forEach(p -> lst.add(new ProductParam(p)));

				return Map.of(
						STATUS, SUCCESS,
						"products", lst,
						ACTION, "products_found",
						SYSTEM_INSTRUCTION, "Products found successfully. "
								+ "Do not retry again. Proceed with the found products.");
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "product_search_failed",
					ERROR_MESSAGE, "Error searching products: " + safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "no_products_found",
				ERROR_MESSAGE, NO_PRODUCTS_FOUND_MATCHING_THE_CRITERIA);
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<Product>> search(TAccessToken token, TSelect<Product> sel) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IProductController controller = serv.lookupWithRetry(IProductController.JNDI_NAME);
			return controller.search(token, sel);
		} finally {
			serv.close();
		}
	}
}
