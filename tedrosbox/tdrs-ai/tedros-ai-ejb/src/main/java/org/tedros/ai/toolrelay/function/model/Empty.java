package org.tedros.ai.toolrelay.function.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Copia de {@code org.tedros.ai.function.model.Empty} (tedros-core, FE) para
 * as tools migradas que nao recebem parametros — mesmo campo {@code param}
 * para que o JSON Schema gerado seja identico ao do FE.
 *
 * @author Davis Gordon
 */
@JsonClassDescription("An empty model class, used when no parameters are needed.")
public class Empty {

	@JsonPropertyDescription("This param is not used.")
	private String param;

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

}
