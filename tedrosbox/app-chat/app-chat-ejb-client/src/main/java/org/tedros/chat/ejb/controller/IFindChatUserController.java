package org.tedros.chat.ejb.controller;

import org.tedros.core.security.model.TUser;
import org.tedros.server.controller.ITSecureEjbController;

import jakarta.ejb.Remote;

@Remote
public interface IFindChatUserController extends ITSecureEjbController<TUser>{
	
	static final String JNDI_NAME = "IFindChatUserControllerRemote";
	
}
