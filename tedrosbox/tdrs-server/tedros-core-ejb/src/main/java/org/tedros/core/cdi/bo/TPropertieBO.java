package org.tedros.core.cdi.bo;

import org.tedros.common.model.TFileEntity;
import org.tedros.core.cdi.eao.TPropertieEao;
import org.tedros.core.setting.model.TPropertie;
import org.tedros.server.cdi.bo.TGenericBO;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class TPropertieBO extends TGenericBO<TPropertie> {
	
	private TLoggerUtil logger = TLoggerUtil.create(TPropertieBO.class);
	
	@Inject
	private TPropertieEao eao;
	
	@Override
	public TPropertieEao getEao() {
		return eao;
	}

	public String getValue(String key) {
		return eao.getValue(key);
	}
	
	public String getValueOrDefault(String key) {
		return eao.getValueOrDefault(key);
	}
	
	public TFileEntity getFile(String key){
		return eao.getFile(key);
	}
	
	public boolean exists(String key) {		
		boolean res = eao.exist(key);
		
		logger.info("Application property {} : {}", key, res ? "Already setted!" : "Not exists!");
		
		return res;
	}

}
