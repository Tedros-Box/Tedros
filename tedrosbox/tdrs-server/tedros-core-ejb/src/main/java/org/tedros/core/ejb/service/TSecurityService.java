/**
 * 
 */
package org.tedros.core.ejb.service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.tedros.core.security.model.TAuthorization;
import org.tedros.core.security.model.TProfile;
import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @author Davis Gordon
 *
 * Gerencia sessões de usuário em um cache Redis distribuído.
 * Qualquer instância do TomEE pode validar o estado de login
 * sem afinidade com a instância original.
 *
 * Chave Redis  : valor do TAccessToken (String UUID)
 * Valor Redis  : TUser serializado em JSON
 * TTL          : 10 horas (36000 segundos)
 */
@Singleton
@Startup
public class TSecurityService {

    private static final Logger LOG = Logger.getLogger(TSecurityService.class.getName());

    /** TTL de sessão: 10 horas em segundos */
    private static final int SESSION_TTL_SECONDS = 36_000;

    /** Prefixo de namespace para as chaves Redis */
    private static final String KEY_PREFIX = "session:";

    private JedisPool jedisPool;
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "redis");
        int redisPort = 6379;
        try {
            String portEnv = System.getenv("REDIS_PORT");
            if (portEnv != null && !portEnv.isBlank()) {
                redisPort = Integer.parseInt(portEnv.trim());
            }
        } catch (NumberFormatException e) {
            LOG.warning("REDIS_PORT inválida, usando 6379.");
        }

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);

        jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
        objectMapper = new ObjectMapper();
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.findAndRegisterModules();
        
        // Módulo para desserializar TAccessToken (que não tem construtor padrão)
        com.fasterxml.jackson.databind.module.SimpleModule module = new com.fasterxml.jackson.databind.module.SimpleModule();
        module.addDeserializer(TAccessToken.class, new com.fasterxml.jackson.databind.JsonDeserializer<TAccessToken>() {
            @Override
            public TAccessToken deserialize(com.fasterxml.jackson.core.JsonParser p, com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {
                com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
                String token = node.has("token") ? node.get("token").asText() : node.asText();
                return new TAccessToken(token);
            }
        });
        objectMapper.registerModule(module);

        LOG.info("TSecurityService conectado ao Redis em " + redisHost + ":" + redisPort);
    }

    @PreDestroy
    public void destroy() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------------

    private String toKey(TAccessToken token) {
        return KEY_PREFIX + token.getToken();
    }

    private String serialize(TUser user) {
        try {
            return objectMapper.writeValueAsString(user);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao serializar TUser para JSON", e);
            throw new RuntimeException("Falha na serialização do TUser", e);
        }
    }

    private TUser deserialize(String json) {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, TUser.class);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Erro ao desserializar TUser do JSON", e);
            return null;
        }
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /**
     * Armazena o usuário no Redis com TTL de 10 horas.
     * Gera um novo TAccessToken se o usuário não possuir um.
     */
    public void addUser(TUser user) {
        if (user.getAccessToken() == null) {
            user.setAccessToken(new TAccessToken(UUID.randomUUID().toString()));
        }
        String key  = toKey(user.getAccessToken());
        String json = serialize(user);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, SESSION_TTL_SECONDS, json);
        }
    }

    /**
     * Recupera o TUser associado ao token, ou null se não existir / expirado.
     */
    public TUser getUser(TAccessToken token) {
        try (Jedis jedis = jedisPool.getResource()) {
            return deserialize(jedis.get(toKey(token)));
        }
    }

    /**
     * Retorna true se o token está ativo no Redis.
     */
    public boolean isAssigned(TAccessToken token) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(toKey(token));
        }
    }

    /**
     * Remove a sessão do Redis (logout).
     */
    public void remove(TAccessToken token) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(toKey(token));
        }
    }

    /**
     * Verifica se uma ou mais ações estão autorizadas para o token/securityId.
     */
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
