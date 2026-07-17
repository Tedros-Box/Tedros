package org.tedros.core.ejb.controller;

import org.tedros.common.model.TFileEntity;
import org.tedros.core.controller.TUserPropertieController;
import org.tedros.core.domain.DomainApp;
import org.tedros.core.ejb.service.TUserPropertieService;
import org.tedros.core.setting.model.TUserPropertie;
import org.tedros.server.ejb.controller.ITSecurityController;
import org.tedros.server.ejb.controller.TSecureEjbController;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.ITSecurity;
import org.tedros.server.security.TAccessPolicie;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.security.TBeanPolicie;
import org.tedros.server.security.TBeanSecurity;
import org.tedros.server.security.TSecurityInterceptor;
import org.tedros.server.service.ITEjbService;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@TSecurityInterceptor
@Stateless(name="TUserPropertieController")
@TBeanSecurity(@TBeanPolicie(id=DomainApp.USER_PROPERTIE_FORM_ID, 
policie= {TAccessPolicie.APP_ACCESS, TAccessPolicie.VIEW_ACCESS}))
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TUserPropertieControllerImpl extends TSecureEjbController<TUserPropertie> implements TUserPropertieController, ITSecurity {

	@EJB
	private TUserPropertieService serv;
	
	@EJB
	private ITSecurityController securityController;

	@Override
	public ITEjbService<TUserPropertie> getService() {
		return serv;
	}

	/**
	 * @return the securityController
	 */
	public ITSecurityController getSecurityController() {
		return securityController;
	}

	@Override
	public TResult<String> getValue(TAccessToken token, Long userId, String key) {
		try {
			String v = serv.getValue(key, userId);
			return new TResult<>(TState.SUCCESS, "", v);
		} catch (Exception e) {
			return super.processException(token, null, e);
		}
	}

	@Override
	public TResult<TFileEntity> getFile(TAccessToken token, Long userId, String key) {
		try {
			TFileEntity v = serv.getFile(key, userId);
			return new TResult<>(TState.SUCCESS, v);
		} catch (Exception e) {
			return super.processException(token, null, e);
		}
	}
	
}
