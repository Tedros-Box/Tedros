package org.tedros.core.ejb.service;

import org.tedros.common.domain.TType;
import org.tedros.common.model.TFileEntity;
import org.tedros.core.cdi.bo.TDomainPropertieBO;
import org.tedros.core.cdi.bo.TPropertieBO;
import org.tedros.core.cdi.bo.TUserPropertieBO;
import org.tedros.core.setting.model.TDomainPropertie;
import org.tedros.server.ejb.service.TEjbService;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

@LocalBean
@Stateless(name="TDomainPropertieService")
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TDomainPropertieService extends TEjbService<TDomainPropertie>	{

	@Inject
	private TDomainPropertieBO bo;
	
	@Inject
	private TPropertieBO propertieBO;
	
	@Inject
	private TUserPropertieBO userPropertieBO;
	
	@Override
	public TDomainPropertieBO getBussinesObject() {
		return bo;
	}

	public String getDefaultValue(String key) {
		return bo.getDefaultValue(key);
	}
	
	public boolean exists(String key) {
		return bo.exists(key);
	}
	
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	public void createDomainPropertie(String name, String key, String defaultValue, String description, TType type) throws Exception {
		bo.createDomainPropertie(name, key, defaultValue, description, type);
	}
	
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	public void createSystemPropertie(String key, String value) throws Exception {
		bo.createSystemPropertie(key, value);
	}
	
	@TransactionAttribute(value = TransactionAttributeType.REQUIRED)
	public void createUserPropertie(String key, String value) throws Exception {
		bo.createUserPropertie(key, value);
	}
	
	public String getSystemPropertyValue(String key) {
		return propertieBO.getValue(key);
	}
	
	public TFileEntity getSystemPropertyFile(String key){
		return propertieBO.getFile(key);
	}
	
	public boolean existsSystemProperty(String key) {
		return propertieBO.exists(key);
	}
	
	public String getUserPropertyValue(String key, Long userId) {
		return userPropertieBO.getValue(key, userId);
	}
	
	public TFileEntity getUserPropertyFile(String key, Long userId){
		return userPropertieBO.getFile(key, userId);
	}
	
	public boolean existsUserProperty(String key, Long userId) {
		return userPropertieBO.exists(key, userId);
	}

}
