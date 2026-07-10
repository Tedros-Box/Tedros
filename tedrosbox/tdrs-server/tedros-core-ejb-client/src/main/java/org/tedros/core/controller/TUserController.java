package org.tedros.core.controller;

import org.tedros.core.security.model.TUser;
import org.tedros.server.controller.ITSecureEjbController;

import jakarta.ejb.Remote;

@Remote
public interface TUserController extends ITSecureEjbController<TUser>{
	

	static final String JNDI_NAME = "TUserControllerRemote";
}
