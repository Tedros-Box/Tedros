package org.tedros.ai.model;

import java.io.Serializable;

public class TAiClientToolSpec implements Serializable {
    
	/**
	 * 
	 */
	private static final long serialVersionUID = 1272043538905817256L;

	private String name;
    
	private String description;
    
	private String parametersSchemaJson;  // JSON Schema (string) dos parâmetros

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getParametersSchemaJson() {
		return parametersSchemaJson;
	}

	public void setParametersSchemaJson(String parametersSchemaJson) {
		this.parametersSchemaJson = parametersSchemaJson;
	}
	
	
}
