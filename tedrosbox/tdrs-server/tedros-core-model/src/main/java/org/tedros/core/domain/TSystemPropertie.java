/**
 * 
 */
package org.tedros.core.domain;

import org.tedros.common.domain.TType;

/**
 * @author Davis Gordon
 *
 */
public enum TSystemPropertie {
	
	DEFAULT_COUNTRY_ISO2("sys.country.iso2","Defines a default country iso2 code to format currency "
			+ "and date. Default value: pt-BR", "pt-BR", TType.SYSTEM),
	TOTAL_PAGE_HISTORY ("sys.page.history","Defines the number of views opened in the history of pages."
			+ "Default value: 4", "4", TType.SYSTEM),
	OWNER ("sys.owner","Defines the name of the system owner", TType.SYSTEM),
	TOKEN ("sys.token","Set Tedros license key", TType.SYSTEM),
	ORGANIZATION ("sys.org","Defines the company trademark to display in reports. Default Tedros", "Tedros", TType.SYSTEM),
	REPORT_LOGOTYPE("sys.report.logo","Sets the trademark logo to be displayed in reports", TType.SYSTEM),
	
	SMTP_USER ("sys.smtp.email","Define SMTP user email", TType.SYSTEM),
	SMTP_PASS ("sys.smtp.pass","Set SMTP user password", TType.SYSTEM),
	SMTP_HOST ("sys.smtp.host","Define SMTP host server", TType.SYSTEM),
	SMTP_PORT ("sys.smtp.port","Defines SMTP server port", TType.SYSTEM),
	SMTP_SOCKET_PORT ("sys.smtp.socket.port","Defines SMTP server socket port", TType.SYSTEM),
	
	NOTIFY_INTERVAL_TIMER ("sys.notify.interval","Defines the interval time in minutes for sending e-mails "
			+ "queued by the Notify module. Default value: 5", "5", TType.SYSTEM),
	
	MONGODB_URI("sys.mongodb.uri","Define the MongoDB connection URI", TType.SYSTEM)
	;
	
	private String value;
	private String description;
	private String defaultValue;
	private TType type;

	/**
	 * @param value
	 */
	private TSystemPropertie(String value, String description, String defaultValue, TType type) {
		this.value = value;
		this.description = description;
		this.defaultValue = defaultValue;
		this.type = type;
	}
	
	/**
	 * @param value
	 */
	private TSystemPropertie(String value, String description, TType type) {
		this.value = value;
		this.description = description;
		this.defaultValue = "";
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

	public String getDefaultValue() {
		return defaultValue;
	}

	public TType getType() {
		return type;
	}
	
	

}
