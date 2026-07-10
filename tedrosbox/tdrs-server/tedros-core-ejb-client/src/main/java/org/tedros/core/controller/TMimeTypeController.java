package org.tedros.core.controller;

import org.tedros.common.model.TMimeType;
import org.tedros.server.controller.ITSecureEjbController;

import jakarta.ejb.Remote;

@Remote
public interface TMimeTypeController extends ITSecureEjbController<TMimeType>{


	static final String JNDI_NAME = "TMimeTypeControllerRemote";
}
