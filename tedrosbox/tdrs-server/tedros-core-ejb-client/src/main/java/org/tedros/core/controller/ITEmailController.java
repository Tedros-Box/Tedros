package org.tedros.core.controller;

import org.tedros.common.model.TFileEntity;
import org.tedros.server.controller.ITBaseController;
import org.tedros.server.result.TResult;
import org.tedros.server.security.TAccessToken;

import jakarta.ejb.Remote;

@Remote
public interface ITEmailController extends ITBaseController{
	

	static final String JNDI_NAME = "ITEmailControllerRemote";
	
	TResult<Boolean> send(TAccessToken token, String to, String subject, String content, boolean html);

	TResult<Boolean> send(TAccessToken token, String to, String subject, 
			String content, boolean html, TFileEntity file);

	
}
