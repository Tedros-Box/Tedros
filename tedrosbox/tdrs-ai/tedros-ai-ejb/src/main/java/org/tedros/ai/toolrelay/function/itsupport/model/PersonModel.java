package org.tedros.ai.toolrelay.function.itsupport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.it.tools.gmud.ai.model.PersonModel}
 * (app-itsupport-tools-fx).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonModel {

	private Long id;
	private String name;
	private String email;

}
