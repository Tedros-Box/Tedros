package org.tedros.core.ejb.support;

import org.tedros.common.domain.TType;
import org.tedros.common.model.TFileEntity;
import org.tedros.core.ejb.service.TDomainPropertieService;
import org.tedros.core.support.TDomainPropertieSupport;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@Stateless(name="TDomainPropertieSupport")
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TDomainPropertieSupportImpl implements TDomainPropertieSupport {
	
	@EJB
	private TDomainPropertieService service;

	@Override
	public void createDomainPropertie(String name, String key, String defaultValue, String description, TType type) throws Exception {
		service.createDomainPropertie(name, key, defaultValue, description, type);
	}
	
	@Override
	public void createSystemPropertie(String key, String value) throws Exception {
		service.createSystemPropertie(key, value);
	}
	
	@Override
	public void createUserPropertie(String key, String value) throws Exception {
		service.createUserPropertie(key, value);
	}

	@Override
	public String getSystemPropertyValue(String key) {
		return service.getSystemPropertyValue(key);
	}

	@Override
	public TFileEntity getSystemPropertyFile(String key) {
		return service.getSystemPropertyFile(key);
	}

	@Override
	public boolean existsSystemProperty(String key) {
		return service.existsSystemProperty(key);
	}

	@Override
	public String getUserPropertyValue(String key, Long userId) {
		return service.getUserPropertyValue(key, userId);
	}

	@Override
	public TFileEntity getUserPropertyFile(String key, Long userId) {
		return service.getUserPropertyFile(key, userId);
	}

	@Override
	public boolean existsUserProperty(String key, Long userId) {
		return service.existsUserProperty(key, userId);
	}

}
