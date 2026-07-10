package org.tedros.ai.toolrelay;

import org.tedros.ai.ejb.client.ITAiToolRelayController;
import org.tedros.ai.model.TAiTurnRequest;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.core.context.TedrosContext;
import org.tedros.core.service.remote.TEjbServiceLocator;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;

/**
 * Encapsula a chamada remota ao {@link ITAiToolRelayController} no backend:
 * lookup via {@link TEjbServiceLocator}, token do usuario logado e tratamento
 * do {@link TResult}.
 *
 * @author Davis Gordon
 */
public class ToolRelayClient {

	/**
	 * Envia um turno ao backend e retorna a resposta.
	 *
	 * @throws Exception se a chamada remota falhar ou o resultado nao for SUCCESS
	 */
	public TAiTurnResponse interact(TAiTurnRequest request) throws Exception {
		try (TEjbServiceLocator loc = TEjbServiceLocator.getInstance()) {
			ITAiToolRelayController serv = loc.lookup(ITAiToolRelayController.JNDI_NAME);
			TAccessToken token = TedrosContext.getLoggedUser().getAccessToken();
			TResult<TAiTurnResponse> res = serv.interact(token, request);
			if (TState.SUCCESS.equals(res.getState()) && res.getValue() != null)
				return res.getValue();
			throw new IllegalStateException(res.getMessage() != null
					? res.getMessage()
					: "AI relay service returned state " + res.getState());
		}
	}
}
