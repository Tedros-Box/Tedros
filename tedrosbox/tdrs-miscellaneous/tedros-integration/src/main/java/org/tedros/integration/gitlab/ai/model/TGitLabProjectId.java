package org.tedros.integration.gitlab.ai.model;

import org.tedros.integration.annotation.TRequiredProperty;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("The model to get gitlab project by id")
public class TGitLabProjectId {
	
	@TRequiredProperty
	@JsonPropertyDescription("The gitlab project id")
	private Long projectId;
}
