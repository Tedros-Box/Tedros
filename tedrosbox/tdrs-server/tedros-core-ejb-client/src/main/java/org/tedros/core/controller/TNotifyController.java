package org.tedros.core.controller;

import org.tedros.core.notify.model.TNotify;
import org.tedros.server.controller.ITSecureEjbController;

import jakarta.ejb.Remote;

@Remote
public interface TNotifyController extends ITSecureEjbController<TNotify>{

	static final String JNDI_NAME = "TNotifyControllerRemote";
	
}
