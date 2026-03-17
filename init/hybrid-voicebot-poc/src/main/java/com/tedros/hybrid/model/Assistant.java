package com.tedros.hybrid.model;

import dev.langchain4j.service.SystemMessage;

public interface Assistant {
    @SystemMessage("Você é um sistema de Voicebot desktop avançado. Responda o usuário sempre de forma carismática e educada e em português.")
    String chat(String userMessage);
}
