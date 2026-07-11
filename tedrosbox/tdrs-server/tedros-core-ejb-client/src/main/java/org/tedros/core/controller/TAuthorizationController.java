package org.tedros.core.controller;

import java.util.List;

import org.tedros.core.security.model.TAuthorization;
import org.tedros.server.controller.ITSecureEjbController;
import org.tedros.server.result.TResult;
import org.tedros.server.security.TAccessToken;

import jakarta.ejb.Remote;

@Remote
public interface TAuthorizationController extends ITSecureEjbController<TAuthorization>{

	static final String JNDI_NAME = "TAuthorizationControllerRemote";
	
	@SuppressWarnings("rawtypes")
	TResult process(TAccessToken token, List<TAuthorization> newLst);

	
}
