package org.tedros.ai.toolrelay.function.extension.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.extension.ai.model.DomainInfo}
 * (app-extensions-fx).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DomainInfo {

	@JsonPropertyDescription("the code")
	private String code;

	@JsonPropertyDescription("the name")
	private String name;

	@JsonPropertyDescription("the description")
	private String description;

}
