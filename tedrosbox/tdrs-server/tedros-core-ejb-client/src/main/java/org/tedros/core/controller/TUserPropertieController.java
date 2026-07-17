package org.tedros.core.controller;

import org.tedros.common.model.TFileEntity;
import org.tedros.core.setting.model.TUserPropertie;
import org.tedros.server.controller.ITSecureEjbController;
import org.tedros.server.result.TResult;
import org.tedros.server.security.TAccessToken;

import jakarta.ejb.Remote;

@Remote
public interface TUserPropertieController extends ITSecureEjbController<TUserPropertie>{

	static final String JNDI_NAME = "TUserPropertieControllerRemote";
	
	TResult<String> getValue(TAccessToken token, Long userId, String key);
	
	TResult<TFileEntity> getFile(TAccessToken token, Long userId, String key);
}
