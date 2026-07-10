package org.tedros.ai.model;

import java.io.Serializable;

public class TAiClientToolResult implements Serializable {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 2421242834176938827L;

	private String callId;                // id do ToolExecutionRequest original
    
	private String name;
    
	private String resultJson;            // resultado serializado (mesmo formato do ToolCallResult.getResult())
    
	private boolean revertToModel;        // semântica do revertToTheAIModelInCaseOfSuccess

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

	public String getResultJson() {
		return resultJson;
	}

	public void setResultJson(String resultJson) {
		this.resultJson = resultJson;
	}

	public boolean isRevertToModel() {
		return revertToModel;
	}

	public void setRevertToModel(boolean revertToModel) {
		this.revertToModel = revertToModel;
	}
	
	
}
