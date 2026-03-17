package com.tedros.hybrid.router;

import com.tedros.hybrid.config.ModelFactory;
import com.tedros.hybrid.model.Assistant;
import com.tedros.hybrid.model.RouterAi;
import com.tedros.hybrid.tools.SystemTools;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;

public class HybridRouter implements Assistant {

    private final Assistant localAssistant;
    private final Assistant cloudAssistantGemini;
    private final Assistant cloudAssistantGrok;
    private final Assistant cloudAssistantOpenAi;
    private final RouterAi localRouter;

    public HybridRouter() {
        // Criando contexto unico que compartilha os últimos 20 passos em todos os
        // assistants!
        ChatMemory sharedMemory = MessageWindowChatMemory.withMaxMessages(20);
        SystemTools tools = new SystemTools();

        this.localAssistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(ModelFactory.getLocalAssistantModel())
                .chatMemory(sharedMemory)
                .tools(tools)
                .build();

        // O Tool Calling é atribuindo no cloud também para assegurar de que a LLM
        // reconheça e
        // compreenda a Message History onde ocorreu ferramenta vindo do Llama, evitando
        // crashes!
        this.cloudAssistantGemini = AiServices.builder(Assistant.class)
                .chatLanguageModel(ModelFactory.getGeminiModel())
                .chatMemory(sharedMemory).tools(tools).build();

        this.cloudAssistantGrok = AiServices.builder(Assistant.class)
                .chatLanguageModel(ModelFactory.getGrokModel())
                .chatMemory(sharedMemory).tools(tools).build();

        this.cloudAssistantOpenAi = AiServices.builder(Assistant.class)
                .chatLanguageModel(ModelFactory.getOpenAiModel())
                .chatMemory(sharedMemory).tools(tools).build();

        this.localRouter = AiServices.builder(RouterAi.class)
                .chatLanguageModel(ModelFactory.getLocalRouterModel())
                .build();
    }

    @Override
    public String chat(String userMessage) {
        String decision = "LOCAL";

        try {
            System.out.println("  [Router LLM] Mapeando topologia da pergunta...");
            decision = localRouter.route(userMessage).toUpperCase().trim();
        } catch (Exception e) {
            System.err.println(
                    "  [Router Falha] Classificador LLM falhou: " + e.getMessage() + ". Assumindo default -> LOCAL");
        }

        System.out.println("  [Router LLM] Fluxo roteado para -> " + decision);

        if (decision.contains("CLOUD")) {
            try {
                if (decision.contains("GEMINI"))
                    return cloudAssistantGemini.chat(userMessage);
                if (decision.contains("GROK"))
                    return cloudAssistantGrok.chat(userMessage);
                return cloudAssistantOpenAi.chat(userMessage); // Fallback do if
            } catch (Exception e) {
                System.err.println("\n  [Fallback Ativado] Provedor Cloud falhou (Erro HTTP / Falta de API Key).");
                System.err.println(
                        "  [Fallback Ativado] Retornando imediatamente para processador LocalOffline (Llama 3b)...");
                return localAssistant.chat(userMessage);
            }
        }

        // Padrão do fluxo base
        return localAssistant.chat(userMessage);
    }
}
