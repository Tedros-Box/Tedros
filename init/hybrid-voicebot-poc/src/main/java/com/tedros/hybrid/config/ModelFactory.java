package com.tedros.hybrid.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.Duration;

public class ModelFactory {

    static {
        try {
            // Tenta carregar variáveis do arquivo raiz chamado .env
            Dotenv.configure().ignoreIfMissing().systemProperties().load();
        } catch (Exception ignored) {}
    }

    public static ChatLanguageModel getLocalAssistantModel() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:3b")
                .temperature(0.3)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    public static ChatLanguageModel getLocalRouterModel() {
        return OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.2:3b") // Changed to 3b since router model didn't load properly on 1b previously 
                .temperature(0.0) // Zero T. = Determinístico: Melhor para algoritmos Classificadores!
                .build();
    }

    public static ChatLanguageModel getGeminiModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(getEnv("GEMINI_API_KEY", "fallback"))
                .modelName("gemini-2.5-flash")
                .temperature(0.7)
                .build();
    }

    public static ChatLanguageModel getGrokModel() {
        // Grok é altamente compatível com a biblioteca padrão do OpenAI (muda-se a base e chama-se pelo nomem).
        return OpenAiChatModel.builder()
                .baseUrl("https://api.x.ai/v1")
                .apiKey(getEnv("GROK_API_KEY", "fallback"))
                .modelName("grok-beta")
                .temperature(0.7)
                .build();
    }

    public static ChatLanguageModel getOpenAiModel() {
        return OpenAiChatModel.builder()
                .apiKey(getEnv("OPENAI_API_KEY", "fallback"))
                .modelName("gpt-4o-mini")
                .temperature(0.7)
                .build();
    }
    
    private static String getEnv(String name, String defaultValue) {
        String value = System.getProperty(name);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(name);
        }
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }
}
