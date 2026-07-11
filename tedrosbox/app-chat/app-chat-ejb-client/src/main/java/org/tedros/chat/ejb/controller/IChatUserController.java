/**
 * 
 */
package org.tedros.chat.ejb.controller;

import org.tedros.chat.entity.ChatUser;
import org.tedros.server.controller.ITSecureEjbController;

import jakarta.ejb.Remote;

/**
 * @author Davis Dun
 *
 */
@Remote
public interface IChatUserController extends ITSecureEjbController<ChatUser> {

	static final String JNDI_NAME = "IChatUserControllerRemote";
		
}
