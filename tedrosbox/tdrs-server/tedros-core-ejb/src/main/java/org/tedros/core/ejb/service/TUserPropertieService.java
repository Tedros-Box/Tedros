package org.tedros.core.ejb.service;

import org.tedros.common.model.TFileEntity;
import org.tedros.core.cdi.bo.TUserPropertieBO;
import org.tedros.core.setting.model.TUserPropertie;
import org.tedros.server.ejb.service.TEjbService;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;

@LocalBean
@Stateless(name="TUserPropertieService")
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TUserPropertieService extends TEjbService<TUserPropertie>	{

	@Inject
	private TUserPropertieBO bo;
	
	@Override
	public TUserPropertieBO getBussinesObject() {
		return bo;
	}

	public String getValue(String key, Long userId) {
		return bo.getValue(key, userId);
	}
	
	public TFileEntity getFile(String key, Long userId){
		return bo.getFile(key, userId);
	}
	
	public boolean exists(String key, Long userId) {
		return bo.exists(key, userId);
	}	

}
