package org.tedros.ai.ejb.client;

import org.tedros.ai.model.TAiTurnRequest;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.server.controller.ITSecureEjbController;
import org.tedros.server.result.TResult;
import org.tedros.server.security.TAccessToken;

import jakarta.ejb.Remote;

@Remote
public interface ITAiToolRelayController extends ITSecureEjbController<TAiTurnResponse> {

	static final String JNDI_NAME = "ITAiToolRelayControllerRemote";
	
	TResult<TAiTurnResponse> interact(TAccessToken token, TAiTurnRequest request);
		
}
