/**
 * 
 */
package org.tedros.core.controller;

import org.tedros.core.ai.model.TAiCompletion;
import org.tedros.core.ai.model.completion.TCompletionRequest;
import org.tedros.core.ai.model.completion.TCompletionResult;
import org.tedros.server.controller.ITSecureEjbController;
import org.tedros.server.result.TResult;
import org.tedros.server.security.TAccessToken;

import jakarta.ejb.Remote;

/**
 * @author Davis Gordon
 *
 */
@Remote
public interface TAiCompletionController extends ITSecureEjbController<TAiCompletion> {

	static final String JNDI_NAME = "TAiCompletionControllerRemote";
	
	TResult<TCompletionResult> completion(TAccessToken token, TCompletionRequest request);

	//TResult<TImageResult> createImage(TAccessToken token, TCreateImageRequest request);
}
