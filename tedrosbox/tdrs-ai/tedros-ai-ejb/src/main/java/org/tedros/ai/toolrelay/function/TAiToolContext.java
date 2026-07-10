package org.tedros.ai.toolrelay.function;

import org.tedros.core.security.model.TUser;
import org.tedros.server.security.TAccessToken;

/**
 * Contexto de execucao de uma tool de backend: usuario autenticado da
 * conversa e o {@link TAccessToken} do request CORRENTE — tools que chamam
 * controllers seguros ({@code TSecureEjbController}) devem usar este token,
 * nunca um token guardado do inicio da conversa (tokens expiram).
 *
 * @author Davis Gordon
 */
public class TAiToolContext {

	private final TUser user;
	private final TAccessToken token;

	public TAiToolContext(TUser user, TAccessToken token) {
		this.user = user;
		this.token = token;
	}

	public TUser getUser() {
		return user;
	}

	public TAccessToken getToken() {
		return token;
	}
}
