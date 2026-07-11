package org.tedros.ai.ejb.controller;


import org.tedros.ai.ejb.client.ITAiToolRelayController;
import org.tedros.ai.ejb.service.GenericEntityService;
import org.tedros.ai.ejb.service.TAiToolRelayService;
import org.tedros.ai.model.TAiTurnRequest;
import org.tedros.ai.model.TAiTurnResponse;
import org.tedros.core.domain.DomainApp;
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

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@TSecurityInterceptor
@Stateless(name="ITAiToolRelayController")
@TBeanSecurity({@TBeanPolicie(id = DomainApp.IA_VIEW_ID, 
policie = { TAccessPolicie.APP_ACCESS, TAccessPolicie.VIEW_ACCESS })})
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TAiToolRelayController extends TSecureEjbController<TAiTurnResponse> implements ITAiToolRelayController, ITSecurity  {

	@EJB
	private GenericEntityService<TAiTurnResponse> serv;

	@EJB
	private ITSecurityController securityController;

	@EJB
	private TAiToolRelayService relayService;
	
	@Override
	public GenericEntityService<TAiTurnResponse> getService() {
		return serv;
	}
	
	@Override
	public ITSecurityController getSecurityController() {
		return securityController;
	}

	@Override
	public TResult<TAiTurnResponse> interact(TAccessToken token, TAiTurnRequest request) {
		try {
			TAiTurnResponse resp = relayService.interact(token, request);
			return new TResult<>(TState.SUCCESS, resp);
		} catch (Exception e) {
			return processException(token, null, e);
		}
	}
	
}
