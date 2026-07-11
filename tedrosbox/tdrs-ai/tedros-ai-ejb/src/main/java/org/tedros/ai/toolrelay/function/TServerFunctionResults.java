package org.tedros.ai.toolrelay.function;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helpers de resultado para as tools de backend migradas do FE, reproduzindo
 * o JSON que o {@code LangChainFunctionExecutor} do FE envia ao LLM.
 *
 * @author Davis Gordon
 */
public final class TServerFunctionResults {

	private TServerFunctionResults() {

	}

	/**
	 * Equivalente ao callback do FE retornar {@code new Response(NO_DATA_FOUND_MESSAGE)}:
	 * o executor do FE embrulha objetos que nao sao {@code ToolCallResult} em
	 * {@code {"data": ...}}, e {@code Response} serializa como
	 * {@code {"message": ..., "object": null}}.
	 */
	public static Map<String, Object> noDataFound() {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("message", TServerFunctionConstants.NO_DATA_FOUND_MESSAGE);
		response.put("object", null);
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("data", response);
		return out;
	}

	/**
	 * Mensagem de erro nunca nula — {@code Map.of} nao aceita valor nulo e a
	 * mensagem vai para o LLM.
	 */
	public static String safeMessage(Exception e) {
		return e.getMessage() != null ? e.getMessage() : e.toString();
	}
}
