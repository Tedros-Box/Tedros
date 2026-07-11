package org.tedros.ai.toolrelay.function.itsupport.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Copia de {@code org.tedros.it.tools.gmud.ai.model.GmudReviewModel}
 * (app-itsupport-tools-fx).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GmudReviewModel {

	private PersonModel reviewer;

    private String comments;

    private String status;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private Date reviewDate;
}
