/**
 * 
 */
package org.tedros.core.ejb.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.tedros.core.security.model.TAuthorization;
import org.tedros.core.security.model.TProfile;
import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * @author Davis Gordon
 *
 * Gerencia sessões de usuário. Suporta dois perfis de execução baseados na
 * variável de ambiente REDIS_HOST:
 * - Docker/Prod: Usa cache Redis distribuído.
 * - Local: Usa cache em memória (ConcurrentHashMap) para desenvolvimento leve.
 */
@Singleton
@Startup
public class TSecurityService {

    private static final Logger LOG = Logger.getLogger(TSecurityService.class.getName());

    private SessionCacheProvider provider;

    @PostConstruct
    public void init() {
        String redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null || redisHost.isBlank() || redisHost.equalsIgnoreCase("local")) {
            LOG.info("Perfil LOCAL ativo: Usando cache de sessão em memória (ConcurrentHashMap) em vez de Redis.");
            this.provider = new LocalSessionCache();
        } else {
            this.provider = new RedisSessionCache(redisHost);
        }
    }

    @PreDestroy
    public void destroy() {
        if (this.provider != null) {
            this.provider.destroy();
        }
    }

    // -------------------------------------------------------------------------
    // API pública (Delegada para a estratégia escolhida)
    // -------------------------------------------------------------------------

    public void addUser(TUser user) {
        if (user.getAccessToken() == null) {
            user.setAccessToken(new TAccessToken(UUID.randomUUID().toString()));
        }
        provider.addUser(user);
    }

    public TUser getUser(TAccessToken token) {
        return provider.getUser(token);
    }

    public boolean isAssigned(TAccessToken token) {
        return provider.isAssigned(token);
    }

    public void remove(TAccessToken token) {
        provider.remove(token);
    }

    public boolean isActionGranted(TAccessToken token, String securityId, String... action) {
        TUser user = getUser(token);
        if (user == null) return false;

        TProfile profile = user.getActiveProfile();
        if (profile == null) return false;

        List<TAuthorization> authorizations = profile.getAutorizations(securityId);
        if (authorizations == null || authorizations.isEmpty()) return false;

        List<String> actions = Arrays.asList(action);
        return authorizations.stream()
                .anyMatch(auth -> actions.stream()
                        .anyMatch(a -> auth.getType().trim().equals(a.trim())));
    }
}
