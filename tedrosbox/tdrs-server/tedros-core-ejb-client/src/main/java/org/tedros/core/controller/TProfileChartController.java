package org.tedros.core.controller;

import org.tedros.server.controller.ITEjbChartController;

import jakarta.ejb.Remote;

@Remote
public interface TProfileChartController extends ITEjbChartController{


	static final String JNDI_NAME = "TProfileChartControllerRemote";
}
