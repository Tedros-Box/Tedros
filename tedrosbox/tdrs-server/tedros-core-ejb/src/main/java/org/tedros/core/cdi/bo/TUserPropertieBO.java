package org.tedros.core.cdi.bo;

import org.tedros.common.model.TFileEntity;
import org.tedros.common.model.TUserPropertie;
import org.tedros.core.cdi.eao.TUserPropertieEao;
import org.tedros.server.cdi.bo.TGenericBO;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class TUserPropertieBO extends TGenericBO<TUserPropertie> {
	
	@Inject
	private TUserPropertieEao eao;
	
	@Override
	public TUserPropertieEao getEao() {
		return eao;
	}

	public String getValue(String key, Long userId) {
		return eao.getValue(key, userId);
	}
	
	public TFileEntity getFile(String key, Long userId){
		return eao.getFile(key, userId);
	}
	
	public boolean exists(String key, Long userId) {		
		return eao.exist(key, userId);
	}

}
