package org.tedros.ai.model;

import java.io.Serializable;

public class TAiTokenUsage implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -1700593175724176703L;

	private Long input;
	
	private Long output;
	
	private Long total;

	public Long getInput() {
		return input;
	}

	public void setInput(Long input) {
		this.input = input;
	}

	public Long getOutput() {
		return output;
	}

	public void setOutput(Long output) {
		this.output = output;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}
	
	

}
