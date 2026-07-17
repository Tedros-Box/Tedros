package org.tedros.core.support;

import org.tedros.common.domain.TType;
import org.tedros.common.model.TFileEntity;

import jakarta.ejb.Remote;

@Remote
public interface TDomainPropertieSupport {
	
	static final String JNDI_NAME = "TDomainPropertieSupportRemote";
	
	void createDomainPropertie(String name, String key, String defaultValue, String description, TType type)
			throws Exception;

	void createSystemPropertie(String key, String value) throws Exception;

	void createUserPropertie(String key, String value) throws Exception;
	
	String getSystemPropertyValue(String key);
	
	TFileEntity getSystemPropertyFile(String key);
	
	boolean existsSystemProperty(String key);
	
	String getUserPropertyValue(String key, Long userId);
	
	TFileEntity getUserPropertyFile(String key, Long userId);
	
	boolean existsUserProperty(String key, Long userId);
	
		
}
