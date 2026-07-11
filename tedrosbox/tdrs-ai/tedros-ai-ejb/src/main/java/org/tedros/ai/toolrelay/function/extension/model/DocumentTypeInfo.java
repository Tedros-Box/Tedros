package org.tedros.ai.toolrelay.function.extension.model;

import org.tedros.extension.model.DocType;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.extension.ai.model.DocumentTypeInfo}
 * (app-extensions-fx).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=true)
@JsonClassDescription("Document type information")
public class DocumentTypeInfo extends DomainInfo {

	@JsonPropertyDescription("the document type")
	private DocType documentType;

}
