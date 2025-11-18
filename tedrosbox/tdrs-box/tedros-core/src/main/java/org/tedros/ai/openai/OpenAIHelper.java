package org.tedros.ai.openai;

import java.util.HashMap;
import java.util.Map;

public class OpenAIHelper {
	
	private OpenAIHelper() {
		
	}
	
	public static Map<String, Integer> buildModelContextLengths(){
	
		Map<String, Integer> map = new HashMap<>();
		// === GPT-5 Series (200k context) ===
	    map.put("gpt-5", 200_000);
	    map.put("gpt-5-mini", 200_000);
	    map.put("gpt-5-nano", 200_000);
	    map.put("gpt-5-2025-08-07", 200_000);
	    map.put("gpt-5-mini-2025-08-07", 200_000);
	    map.put("gpt-5-nano-2025-08-07", 200_000);
	    map.put("gpt-5-chat-latest", 200_000);

	    // === GPT-4.1 Series (128k context) ===
	    map.put("gpt-4.1", 128_000);
	    map.put("gpt-4.1-mini", 128_000);
	    map.put("gpt-4.1-nano", 128_000);
	    map.put("gpt-4.1-2025-04-14", 128_000);
	    map.put("gpt-4.1-mini-2025-04-14", 128_000);
	    map.put("gpt-4.1-nano-2025-04-14", 128_000);

	    // === o4-mini (128k) ===
	    map.put("o4-mini", 128_000);
	    map.put("o4-mini-2025-04-16", 128_000);

	    // === o3 Series (200k context) ===
	    map.put("o3", 200_000);
	    map.put("o3-2025-04-16", 200_000);
	    map.put("o3-mini", 200_000);
	    map.put("o3-mini-2025-01-31", 200_000);

	    // === o1 Series (128k context oficial, apesar de rumores de 200k interno) ===
	    map.put("o1", 128_000);
	    map.put("o1-2024-12-17", 128_000);
	    map.put("o1-preview", 128_000);
	    map.put("o1-preview-2024-09-12", 128_000);
	    map.put("o1-mini", 128_000);
	    map.put("o1-mini-2024-09-12", 128_000);

	    // === GPT-4o Series (128k context) ===
	    map.put("gpt-4o", 128_000);
	    map.put("gpt-4o-2024-11-20", 128_000);
	    map.put("gpt-4o-2024-08-06", 128_000);
	    map.put("gpt-4o-2024-05-13", 128_000);
	    map.put("gpt-4o-audio-preview", 128_000);
	    map.put("gpt-4o-audio-preview-2024-10-01", 128_000);
	    map.put("gpt-4o-audio-preview-2024-12-17", 128_000);
	    map.put("gpt-4o-audio-preview-2025-06-03", 128_000);
	    map.put("gpt-4o-mini-audio-preview", 128_000);
	    map.put("gpt-4o-mini-audio-preview-2024-12-17", 128_000);
	    map.put("gpt-4o-search-preview", 128_000);
	    map.put("gpt-4o-mini-search-preview", 128_000);
	    map.put("gpt-4o-search-preview-2025-03-11", 128_000);
	    map.put("gpt-4o-mini-search-preview-2025-03-11", 128_000);
	    map.put("chatgpt-4o-latest", 128_000);

	    // === GPT-4o-mini Series (128k) ===
	    map.put("gpt-4o-mini", 128_000);
	    map.put("gpt-4o-mini-2024-07-18", 128_000);

	    // === GPT-4 Turbo & Legacy (128k ou menos) ===
	    map.put("gpt-4-turbo", 128_000);
	    map.put("gpt-4-turbo-2024-04-09", 128_000);
	    map.put("gpt-4-0125-preview", 128_000);
	    map.put("gpt-4-turbo-preview", 128_000);
	    map.put("gpt-4-1106-preview", 128_000);
	    map.put("gpt-4-vision-preview", 128_000);

	    // === GPT-4 Classic (8k ou 32k) ===
	    map.put("gpt-4", 8_192);
	    map.put("gpt-4-0314", 8_192);
	    map.put("gpt-4-0613", 8_192);
	    map.put("gpt-4-32k", 32_768);
	    map.put("gpt-4-32k-0314", 32_768);
	    map.put("gpt-4-32k-0613", 32_768);

	    // === GPT-3.5 Turbo (16k max) ===
	    map.put("gpt-3.5-turbo", 16_385);
	    map.put("gpt-3.5-turbo-16k", 16_385);
	    map.put("gpt-3.5-turbo-0301", 4_096);
	    map.put("gpt-3.5-turbo-0613", 16_385);
	    map.put("gpt-3.5-turbo-1106", 16_385);
	    map.put("gpt-3.5-turbo-0125", 16_385);
	    map.put("gpt-3.5-turbo-16k-0613", 16_385);

	    // === Outros ===
	    map.put("codex-mini-latest", 8_192); // estimado
	    
	    return map;
	}

}
