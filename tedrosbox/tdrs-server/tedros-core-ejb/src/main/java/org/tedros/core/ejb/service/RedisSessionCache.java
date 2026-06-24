package org.tedros.core.ejb.service;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

class RedisSessionCache implements SessionCacheProvider {
	
	private static final Logger LOG = Logger.getLogger(RedisSessionCache.class.getName());

    /** TTL de sessão: 10 horas em segundos */
    private static final int SESSION_TTL_SECONDS = 36_000;

    /** Prefixo de namespace para as chaves Redis */
    private static final String KEY_PREFIX = "session:";
	
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public RedisSessionCache(String redisHost) {
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

    @Override
    public void addUser(TUser user) {
        String key = toKey(user.getAccessToken());
        String json = serialize(user);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.setex(key, SESSION_TTL_SECONDS, json);
        }
    }

    @Override
    public TUser getUser(TAccessToken token) {
        try (Jedis jedis = jedisPool.getResource()) {
            return deserialize(jedis.get(toKey(token)));
        }
    }

    @Override
    public boolean isAssigned(TAccessToken token) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(toKey(token));
        }
    }

    @Override
    public void remove(TAccessToken token) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(toKey(token));
        }
    }

    @Override
    public void destroy() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
