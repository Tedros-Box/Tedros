package org.tedros.core.controller;

import org.tedros.core.security.model.TProfile;
import org.tedros.server.controller.ITSecureEjbController;

import jakarta.ejb.Remote;

@Remote
public interface TProfileController extends ITSecureEjbController<TProfile>{


	static final String JNDI_NAME = "TProfileControllerRemote";
}
