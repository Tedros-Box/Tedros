package org.tedros.chat.ejb.controller;

import org.tedros.chat.entity.ChatMessage;
import org.tedros.server.controller.ITSecureEjbController;

import jakarta.ejb.Remote;

@Remote
public interface IChatMessageController extends ITSecureEjbController<ChatMessage>{
	
	static final String JNDI_NAME = "IChatMessageControllerRemote";
	
}
