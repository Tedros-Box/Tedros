package org.tedros.ai.toolrelay;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.tedros.ai.model.TAiClientToolSpec;
import org.tedros.ai.service.langchain.LangChainFunctionExecutor;
import org.tedros.util.TLoggerUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.internal.JsonSchemaElementUtils;

/**
 * Converte as {@link ToolSpecification}s do {@link LangChainFunctionExecutor}
 * para {@link TAiClientToolSpec}s, serializando cada schema de parametros
 * para JSON Schema em String (inverso do {@code TJsonSchemaConverter} do
 * backend).
 *
 * @author Davis Gordon
 */
public class ClientToolSpecExporter {

	private static final Logger LOGGER = TLoggerUtil.getLogger(ClientToolSpecExporter.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public List<TAiClientToolSpec> export(LangChainFunctionExecutor executor) {
		List<TAiClientToolSpec> specs = new ArrayList<>();
		if (executor == null)
			return specs;
		for (ToolSpecification ts : executor.getToolSpecifications()) {
			TAiClientToolSpec spec = new TAiClientToolSpec();
			spec.setName(ts.name());
			spec.setDescription(ts.description());
			spec.setParametersSchemaJson(schemaJson(ts));
			specs.add(spec);
		}
		return specs;
	}

	private String schemaJson(ToolSpecification ts) {
		try {
			if (ts.parameters() == null)
				return "{}";
			Map<String, Object> map = JsonSchemaElementUtils.toMap(ts.parameters());
			return MAPPER.writeValueAsString(map);
		} catch (Exception e) {
			LOGGER.error("Erro serializando schema da tool {}", ts.name(), e);
			return "{}";
		}
	}
}
