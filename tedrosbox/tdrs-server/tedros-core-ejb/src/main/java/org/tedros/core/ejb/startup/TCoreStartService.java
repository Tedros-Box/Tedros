package org.tedros.core.ejb.startup;

import org.tedros.common.domain.TType;
import org.tedros.core.cdi.bo.TDomainPropertieBO;
import org.tedros.core.domain.TSystemPropertie;
import org.tedros.core.ejb.service.TDomainPropertieService;
import org.tedros.server.util.TLoggerUtil;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Session Bean implementation class TCoreStartService
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TCoreStartService {

	private TLoggerUtil logger = TLoggerUtil.create(TDomainPropertieBO.class);
	
	@EJB
	private TDomainPropertieService serv;
	
	@PostConstruct
	public void init() {		
		for(TSystemPropertie p : TSystemPropertie.values()) {
			try {
				serv.createDomainPropertie(p.name(), p.getValue(), p.getDefaultValue(), p.getDescription(), p.getType());
				if(p.getType() == TType.SYSTEM) {
					serv.createSystemPropertie(p.getValue(), p.getDefaultValue());
				}
				if(p.getType() == TType.USER) {
					serv.createUserPropertie(p.getValue(), p.getDefaultValue());
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}
}
