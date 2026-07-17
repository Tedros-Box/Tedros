/**
 * 
 */
package org.tedros.chat.ejb.startup;

import org.tedros.chat.domain.ChatPropertie;
import org.tedros.common.domain.TType;
import org.tedros.core.support.TDomainPropertieSupport;
import org.tedros.server.util.TLoggerUtil;
import org.tedros.server.util.TServiceLocator;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.enterprise.concurrent.ManagedExecutorService;

/**
 * @author Davis Gordon
 *
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class ChatStartService {
	
	private TLoggerUtil logger = TLoggerUtil.create(ChatStartService.class);
	
	@Resource
    private ManagedExecutorService executor; // pool de threads do TomEE

    @PostConstruct
    public void init() {
        // Dispara assíncrono e SAI IMEDIATAMENTE (EAR continua inicializando)
        executor.submit(this::run);
    }
	
	private void run() {
        try(TServiceLocator serv = TServiceLocator.getInstance()) {
        	TDomainPropertieSupport support = serv.lookupWithRetry(TDomainPropertieSupport.JNDI_NAME);
            for (ChatPropertie p : ChatPropertie.values()) {
            	support.createDomainPropertie(p.name(), p.getValue(), null, p.getDescription(), p.getType());
				if(p.getType() == TType.SYSTEM) {
					support.createSystemPropertie(p.getValue(), null);
				}
            }
        } catch (Exception e) {
        	logger.error(e.getMessage(), e);
        }
    }

}
