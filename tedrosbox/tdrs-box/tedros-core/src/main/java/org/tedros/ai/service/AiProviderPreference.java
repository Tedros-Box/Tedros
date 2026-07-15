package org.tedros.ai.service;

import org.apache.commons.lang3.StringUtils;

public record AiProviderPreference(
		String apiKey,
		String model,
		String systemPrompt
		) {
	
	public boolean hasApyKeyAndModel() {
		return StringUtils.isNoneBlank(apiKey, model);
	}
	
	public boolean hasAllProperties() {
		return StringUtils.isNoneBlank(apiKey, model, systemPrompt);
	}

}
