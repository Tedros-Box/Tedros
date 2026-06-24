package org.tedros.core.ejb.service;

import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

interface SessionCacheProvider {
    void addUser(TUser user);
    TUser getUser(TAccessToken token);
    boolean isAssigned(TAccessToken token);
    void remove(TAccessToken token);
    void destroy();
}
