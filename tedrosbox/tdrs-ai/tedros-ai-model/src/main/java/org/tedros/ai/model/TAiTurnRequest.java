package org.tedros.ai.model;

import java.io.Serializable;
import java.util.List;

public class TAiTurnRequest implements Serializable {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = -350307122883050395L;

	private String conversationId;        // null no primeiro turno; BE gera e devolve
    
	private TAiTurnRequestType type;      // MESSAGE | TOOL_RESULTS | CLOSE
    
	private String userMessage;           // quando MESSAGE
    
	private String sysPrompt;             // opcional, complementa o prompt do turno
    
	private List<TAiClientToolSpec> clientToolSpecs; // enviado no primeiro MESSAGE da conversa
    
	private List<TAiClientToolResult> toolResults;   // quando TOOL_RESULTS

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public TAiTurnRequestType getType() {
		return type;
	}

	public void setType(TAiTurnRequestType type) {
		this.type = type;
	}

	public String getUserMessage() {
		return userMessage;
	}

	public void setUserMessage(String userMessage) {
		this.userMessage = userMessage;
	}

	public String getSysPrompt() {
		return sysPrompt;
	}

	public void setSysPrompt(String sysPrompt) {
		this.sysPrompt = sysPrompt;
	}

	public List<TAiClientToolSpec> getClientToolSpecs() {
		return clientToolSpecs;
	}

	public void setClientToolSpecs(List<TAiClientToolSpec> clientToolSpecs) {
		this.clientToolSpecs = clientToolSpecs;
	}

	public List<TAiClientToolResult> getToolResults() {
		return toolResults;
	}

	public void setToolResults(List<TAiClientToolResult> toolResults) {
		this.toolResults = toolResults;
	}
	
	
}
