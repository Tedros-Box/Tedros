/**
 * 
 */
package org.tedros.core.controller;

import org.tedros.common.model.TMimeType;
import org.tedros.server.controller.ITEjbImportController;

import jakarta.ejb.Remote;

/**
 * @author Davis Gordon
 *
 */
@Remote
public interface TMimeTypeImportController extends ITEjbImportController<TMimeType> {

	static final String JNDI_NAME = "TMimeTypeImportControllerRemote";
}
