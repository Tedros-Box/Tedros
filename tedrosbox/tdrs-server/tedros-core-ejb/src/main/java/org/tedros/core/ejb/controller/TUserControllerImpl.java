package org.tedros.core.ejb.controller;

import java.util.List;

import org.tedros.common.domain.TType;
import org.tedros.common.model.TDomainPropertie;
import org.tedros.common.model.TUserPropertie;
import org.tedros.core.controller.TUserController;
import org.tedros.core.domain.DomainApp;
import org.tedros.core.ejb.service.TDomainPropertieService;
import org.tedros.core.ejb.service.TUserPropertieService;
import org.tedros.core.ejb.service.TUserService;
import org.tedros.core.security.model.TUser;
import org.tedros.server.ejb.controller.ITSecurityController;
import org.tedros.server.ejb.controller.TSecureEjbController;
import org.tedros.server.result.TResult;
import org.tedros.server.security.ITSecurity;
import org.tedros.server.security.TAccessPolicie;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.security.TBeanPolicie;
import org.tedros.server.security.TBeanSecurity;
import org.tedros.server.security.TSecurityInterceptor;
import org.tedros.server.service.ITEjbService;
import org.tedros.server.util.TLoggerUtil;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@TSecurityInterceptor
@Stateless(name="TUserController")
@TBeanSecurity(@TBeanPolicie(id=DomainApp.USER_FORM_ID, 
policie= {TAccessPolicie.APP_ACCESS, TAccessPolicie.VIEW_ACCESS}))
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TUserControllerImpl extends TSecureEjbController<TUser> implements	ITSecurity, TUserController {

	private TLoggerUtil logger = TLoggerUtil.create(TUserControllerImpl.class);
	
	@EJB
	private TUserService serv;
	
	@EJB
	private TUserPropertieService userPropertieService;
	
	@EJB
	private TDomainPropertieService domainPropertieService;
	
	@EJB
	private ITSecurityController security;
	
	@Override
	public ITEjbService<TUser> getService() {
		return serv;
	}

	@Override
	public ITSecurityController getSecurityController() {
		return security;
	}
	
	@Override
	public TResult<TUser> save(TAccessToken token, TUser entity) {		
		TResult<TUser> result = super.save(token, entity);;
		try {
			verifySaveResult(result);
		} catch (Exception e) {
			return super.processException(token, entity, e);
		}
		return result;
	}
	
	@Override
	public TResult<List<TResult<TUser>>> save(TAccessToken token, List<TUser> entities) {
		TResult<List<TResult<TUser>>> results = super.save(token, entities);
		if(results.isSuccess()) {						
			for(TResult<TUser> result : results.getValue()) {
				try {
					verifySaveResult(result);
				} catch (Exception e) {
					return super.processException(token, result.getValue(), e);
				}
			}			
		}
		return results;
	}
	
	private void verifySaveResult(TResult<TUser> result) throws Exception {
		if(result.isSuccess()) {
			TUser user = result.getValue();
			TDomainPropertie template = new TDomainPropertie();
			template.setType(TType.USER);
			List<TDomainPropertie> domainList = domainPropertieService.findAll(template);				
			createUserPropertie(user, domainList);
			
		}
	}	
	
	private void createUserPropertie(TUser user, List<TDomainPropertie> domainList) throws Exception {
		for(TDomainPropertie domain : domainList) {
			if(domainPropertieService.existsUserProperty(domain.getKey(), user.getId())) {
				continue;
			}
			
			TUserPropertie propertie = new TUserPropertie();
			propertie.setDomain(domain);
			propertie.setValue(domain.getDefaultValue());
			propertie.setCreatedByUserId(user.getId());
			userPropertieService.save(propertie);
			
			logger.info("User Property {} created for {}!", domain.getKey(), user.getName());
		}
	}
}
