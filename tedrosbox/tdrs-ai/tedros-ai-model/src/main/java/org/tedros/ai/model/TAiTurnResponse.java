package org.tedros.ai.model;

import java.util.List;

import org.tedros.server.entity.TEntity;

// Herda de TEntity apenas para cumprir os contratos dos controllers ejb
public class TAiTurnResponse extends TEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4641993027115129520L;

	private String conversationId;
    
	private TAiTurnResponseType type;     // FINAL | CLIENT_TOOL_CALLS | ERROR
    
	private String text;                  // quando FINAL (pode acompanhar CLIENT_TOOL_CALLS se o LLM gerou texto junto)
    
	private List<TAiPendingToolCall> pendingCalls; // quando CLIENT_TOOL_CALLS
    
	private String errorCode;             // ex: TURN_IN_PROGRESS, CONVERSATION_NOT_FOUND, MAX_DEPTH, LLM_ERROR
    
	private String errorMessage;
    
	private TAiTokenUsage tokenUsage;     // acumulado do turno (input/output/total)

	public String getConversationId() {
		return conversationId;
	}

	public void setConversationId(String conversationId) {
		this.conversationId = conversationId;
	}

	public TAiTurnResponseType getType() {
		return type;
	}

	public void setType(TAiTurnResponseType type) {
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<TAiPendingToolCall> getPendingCalls() {
		return pendingCalls;
	}

	public void setPendingCalls(List<TAiPendingToolCall> pendingCalls) {
		this.pendingCalls = pendingCalls;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public TAiTokenUsage getTokenUsage() {
		return tokenUsage;
	}

	public void setTokenUsage(TAiTokenUsage tokenUsage) {
		this.tokenUsage = tokenUsage;
	}
}
