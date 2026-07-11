package org.tedros.ai.toolrelay.function.person;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.person.ai.function.ClassificationParam}
 * (app-person-fx) — mesmos campos/tipos para schema identico.
 *
 * @author Davis Gordon
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonClassDescription("the person classification")
public class ClassificationParam {

	@JsonPropertyDescription("[NOT NULL] Acceptable values: "
			+ "LEGAL_PERSON, NATURAL_PERSON, EMPLOYEE, " +
			"CUSTOMER, CLIENT_COMPANY, MEMBER, PHILANTHROPE, VOLUNTARY, ALL")
	private Classification classification;

}
