/**
 * 
 */
package org.tedros.chat.domain;

import org.tedros.common.domain.TType;

/**
 * @author Davis Gordon
 *
 */
public enum ChatPropertie {
	
	CHAT_SERVER_IP("chat.server.ip", "Define the chat server ip", TType.SYSTEM),
	CHAT_SERVER_PORT("chat.server.port","Define the chat server port", TType.SYSTEM);
	
	private String value;
	private String description;
	private TType type;
	
	private ChatPropertie(String v, String description, TType type) {
		this.value = v;
		this.description = description;
		this.type = type;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	public TType getType() {
		return type;
	}
}
