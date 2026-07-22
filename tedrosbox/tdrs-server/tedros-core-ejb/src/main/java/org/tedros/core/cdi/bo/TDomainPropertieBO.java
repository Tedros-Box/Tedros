package org.tedros.core.cdi.bo;

import java.util.List;
import java.util.Optional;

import org.tedros.common.domain.TType;
import org.tedros.common.model.TDomainPropertie;
import org.tedros.common.model.TPropertie;
import org.tedros.common.model.TUserPropertie;
import org.tedros.core.cdi.eao.TDomainPropertieEao;
import org.tedros.core.cdi.eao.TPropertieEao;
import org.tedros.core.cdi.eao.TUserEao;
import org.tedros.core.cdi.eao.TUserPropertieEao;
import org.tedros.core.security.model.TUser;
import org.tedros.server.cdi.bo.TGenericBO;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class TDomainPropertieBO extends TGenericBO<TDomainPropertie> {
	
	private TLoggerUtil logger = TLoggerUtil.create(TDomainPropertieBO.class);
	
	@Inject
	private TDomainPropertieEao eao;
	
	@Inject
	private TPropertieEao propertieEao;
	
	@Inject
	private TUserPropertieEao userPropertieEao;
	
	@Inject
	private TUserEao userEao;
	
	@Override
	public TDomainPropertieEao getEao() {
		return eao;
	}

	public String getDefaultValue(String key) {
		return eao.getDefaultValue(key);
	}
	
	
	public boolean exists(String key) {		
		boolean res = eao.exist(key);
		
		logger.info("Property {} : {}", key, res ? "Already setted!" : "Not exists!");
		
		return res;
	}
	
	public void createDomainPropertie(String name, String key, String defaultValue, String description, TType type) throws Exception {
		
		Optional<TDomainPropertie> opt = eao.getByKey(key);
		
		if(opt.isPresent()) {
			TDomainPropertie domainPropertie = opt.get();
			
			if(domainPropertie.isSameValues(name, key, defaultValue, description, type)) {
				return;
			}
			
			domainPropertie.setKey(key);
			domainPropertie.setName(name);
			domainPropertie.setType(type);
			domainPropertie.setDescription(description);
			domainPropertie.setDefaultValue(defaultValue);
			eao.merge(domainPropertie);
			logger.info("Domain property {} updated!", key);
		}else {
			TDomainPropertie domainPropertie = new TDomainPropertie();
			domainPropertie.setKey(key);
			domainPropertie.setName(name);
			domainPropertie.setType(type);
			domainPropertie.setDescription(description);
			domainPropertie.setDefaultValue(defaultValue);
			eao.persist(domainPropertie);
			logger.info("Domain property {} created!", key);
		}
		
		
	}
	
	
	public void createSystemPropertie(String key, String value) throws Exception {
		
		if(propertieEao.exist(key))
			return;
		
		Optional<TDomainPropertie> opt = eao.getByKey(key);
		
		if(opt.isEmpty())
			throw new IllegalStateException("The domain key propertie" + key + " doesnt exists!");
		
		TDomainPropertie domainPropertie = opt.get();
			
		TPropertie propertie = new TPropertie();
		propertie.setDomain(domainPropertie);
		propertie.setValue(value);
		propertieEao.persist(propertie);
		
		logger.info("System Property {} created!", key);
	}
	
	public void createUserPropertie(String key, String value) throws Exception {
		
		Optional<TDomainPropertie> opt = eao.getByKey(key);
		
		if(opt.isEmpty())
			throw new IllegalStateException("The domain key propertie" + key + " doesnt exists!");
		
		TDomainPropertie domainPropertie = opt.get();
		
		List<TUser> users = userEao.listAll(TUser.class);
		if(users==null || users.isEmpty())
			return;
		
		for(TUser user : users){
			
			if(userPropertieEao.exist(key, user.getId())) {
				continue;
			}
			
			TUserPropertie propertie = new TUserPropertie();
			propertie.setDomain(domainPropertie);
			propertie.setValue(value);
			propertie.setCreatedByUserId(user.getId());
			userPropertieEao.persist(propertie);
			
			logger.info("User Property {} created for {}!", key, user.getName());
		}
	}

}
