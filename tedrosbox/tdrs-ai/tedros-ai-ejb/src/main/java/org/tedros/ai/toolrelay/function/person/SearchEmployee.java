package org.tedros.ai.toolrelay.function.person;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.person.ai.function.SearchEmployee}
 * (app-person-fx) — mesmos campos/tipos para schema identico.
 *
 * @author Davis Gordon
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("Criteria for searching a employee")
public class SearchEmployee {

	@JsonPropertyDescription("The full or partial name of the employee.")
	private String name;

	@JsonPropertyDescription("Official identification document (e.g., Tax ID, CPF, SSN, Passport number, Driver's License).")
	private String document;

}
