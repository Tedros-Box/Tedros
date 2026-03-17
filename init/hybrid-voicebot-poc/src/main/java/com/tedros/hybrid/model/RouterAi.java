package com.tedros.hybrid.model;

import dev.langchain4j.service.SystemMessage;

public interface RouterAi {
    @SystemMessage(
        "Você atua como um roteador proxy na arquitetura do chatbot. Analise a requisição do usuário. " +
        "Sua ÚNICA função é classificar e responder EXATAMENTE uma das 4 palavras abaixo e nada mais:\n\n" +
        "LOCAL -> Para pedir algo de interação diária, ferramentas, usar o PC do usuario, e dúvidas corriqueiras.\n" +
        "CLOUD GEMINI -> Para coisas muito complexas e geração técnica profunda\n" +
        "CLOUD GROK -> Para informações recentes de mercado\n" +
        "CLOUD OPENAI -> Para tradução e analise complexa de texto\n\n" +
        "Nunca escreva vírgulas ou textos extras. NUNCA EXPLIQUE NADA."
    )
    String route(String userMessage);
}
