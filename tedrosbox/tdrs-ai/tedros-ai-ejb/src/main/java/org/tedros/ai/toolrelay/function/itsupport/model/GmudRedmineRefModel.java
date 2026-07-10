package org.tedros.ai.toolrelay.function.itsupport.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.it.tools.gmud.ai.model.GmudRedmineRefModel}
 * (app-itsupport-tools-fx).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmudRedmineRefModel {

	private Integer redmineIssueId;
    private String redmineIssueTitle;
}
