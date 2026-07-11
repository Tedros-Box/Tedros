/**
 * 
 */
package org.tedros.core.controller;

import org.tedros.core.ai.model.TAiChatMessage;
import org.tedros.server.controller.ITSecureEjbController;

import jakarta.ejb.Remote;

/**
 * @author Davis Gordon
 *
 */
@Remote
public interface TAiChatMessageController extends ITSecureEjbController<TAiChatMessage> {

	static final String JNDI_NAME = "TAiChatMessageControllerRemote";
}
