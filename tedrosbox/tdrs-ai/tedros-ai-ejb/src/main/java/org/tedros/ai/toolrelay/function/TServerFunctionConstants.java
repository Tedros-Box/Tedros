package org.tedros.ai.toolrelay.function;

/**
 * Constantes de resultado das tools de backend — replica LITERAL das
 * constantes de {@code org.tedros.ai.function.TFunction} (tedros-core, FE),
 * inacessivel neste EAR. As strings foram calibradas para o LLM: qualquer
 * mudanca aqui deve ser espelhada no FE e vice-versa.
 *
 * @author Davis Gordon
 */
public final class TServerFunctionConstants {

	public static final String SUCCESS = "success";
	public static final String ERROR = "error";
	public static final String STATUS = "status";
	public static final String ACTION = "action";
	public static final String INFO_MESSAGE = "info_message";
	public static final String ERROR_MESSAGE = "error_message";
	public static final String SYSTEM_INSTRUCTION = "system_instruction";

	public static final String NO_DATA_FOUND_MESSAGE = "No data found. ";

	private TServerFunctionConstants() {

	}
}
