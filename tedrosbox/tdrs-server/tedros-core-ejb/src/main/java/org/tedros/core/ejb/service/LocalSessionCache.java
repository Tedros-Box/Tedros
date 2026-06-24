package org.tedros.core.ejb.service;

import java.util.concurrent.ConcurrentHashMap;

import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

class LocalSessionCache implements SessionCacheProvider {
    private final ConcurrentHashMap<String, TUser> cache = new ConcurrentHashMap<>();

    @Override
    public void addUser(TUser user) {
        cache.put(user.getAccessToken().getToken(), user);
    }

    @Override
    public TUser getUser(TAccessToken token) {
        return cache.get(token.getToken());
    }

    @Override
    public boolean isAssigned(TAccessToken token) {
        return cache.containsKey(token.getToken());
    }

    @Override
    public void remove(TAccessToken token) {
        cache.remove(token.getToken());
    }

    @Override
    public void destroy() {
        cache.clear();
    }
}