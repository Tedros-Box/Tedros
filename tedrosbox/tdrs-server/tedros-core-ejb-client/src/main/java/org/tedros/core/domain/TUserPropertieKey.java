/**
 * 
 */
package org.tedros.core.domain;

/**
 * Known keys for user-specific properties.
 */
public enum TUserPropertieKey {

	REDMINE_API_KEY ("user.redmine.api.key");
	
	private String value;
	
	private TUserPropertieKey(String k) {
		this.value = k;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}
	
	
}
