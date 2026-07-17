package org.tedros.ai.ejb.startup;

import org.tedros.ai.domain.AiRelayPropertie;
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
 * Cadastra as properties do modulo AI Tool Relay no boot do EAR.
 * Dispara de forma assincrona para nao bloquear a inicializacao.
 *
 * @author Davis Gordon
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class AiAppStartupService {

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
            for (AiRelayPropertie p : AiRelayPropertie.values()) {
            	support.createDomainPropertie(p.name(), p.getValue(), p.getDefaultValue(), p.getDescription(), p.getType());
				if(p.getType() == TType.SYSTEM) {
					support.createSystemPropertie(p.getValue(), p.getDefaultValue());
				}
            }
            
        } catch (Exception e) {
            TLoggerUtil.create(getClass()).error("ERROR at AI relay startup", e);
        }
    }
}
