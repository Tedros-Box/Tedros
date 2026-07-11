package org.tedros.ai.toolrelay.function.itsupport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.it.tools.gmud.ai.model.GmudItemModel}
 * (app-itsupport-tools-fx).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmudItemModel {

	private Integer stepOrder;

    private String action;

    private PersonModel responsible;

    private String status;
}
