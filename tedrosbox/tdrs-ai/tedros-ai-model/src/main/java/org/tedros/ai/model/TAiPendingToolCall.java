package org.tedros.ai.model;

import java.io.Serializable;

public class TAiPendingToolCall implements Serializable {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1934724993305739958L;

	private String callId;
    
    private String name;
    
    private String argumentsJson;

	public String getCallId() {
		return callId;
	}

	public void setCallId(String callId) {
		this.callId = callId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getArgumentsJson() {
		return argumentsJson;
	}

	public void setArgumentsJson(String argumentsJson) {
		this.argumentsJson = argumentsJson;
	}
    
    
    
    
}
