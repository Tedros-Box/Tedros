/**
 * 
 */
package org.tedros.server.ejb.controller;

import org.tedros.server.entity.ITUser;
import org.tedros.server.security.TAccessToken;

import jakarta.ejb.Remote;

/**
 * @author Davis Gordon
 *
 */
@Remote
public interface ITSecurityController{
	
	ITUser getUser(TAccessToken token);
	
	boolean isAccessGranted(TAccessToken clent);

	boolean isPolicieAllowed(TAccessToken token, String securityId, String... action);
	
}
