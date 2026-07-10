package org.tedros.ai.toolrelay.function;

import org.tedros.core.security.model.TUser;

/**
 * Contrato das tools de backend do relay — mesma ideia do {@code TFunction}
 * do frontend, executada no servidor.
 * <p>
 * Implementacoes sao beans CDI descobertos pelo {@link TServerFunctionCatalog}.
 * Tools que consomem outros servicos do Tedros devem usar as interfaces
 * {@code -ejb-client} com lookup JNDI (ex: {@code IPersonController}), nunca
 * dependencia direta do EJB de outro EAR.
 *
 * @author Davis Gordon
 */
public interface TServerAiFunction {

	String getName();

	String getDescription();

	/**
	 * DTO dos parametros da tool (mesma ideia do TFunction do FE); convertido
	 * para JSON Schema por {@link TJsonSchemaConverter#fromModelClass(Class)}.
	 */
	Class<?> getModel();

	/**
	 * Executa a tool. As implementacoes DEVEM respeitar a autorizacao do
	 * usuario autenticado recebido.
	 *
	 * @param arg  instancia de {@link #getModel()} desserializada dos argumentos
	 * @param user usuario autenticado da conversa
	 * @return resultado serializavel em JSON para o LLM
	 */
	Object execute(Object arg, TUser user);

	/**
	 * Se true (default), o resultado retorna ao LLM para nova iteracao do
	 * loop; se false, o turno encerra com o texto da ultima AiMessage.
	 */
	default boolean revertToModel() {
		return true;
	}
}
