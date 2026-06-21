package com.tedros.hybrid;

import com.tedros.hybrid.router.HybridRouter;
import com.tedros.hybrid.setup.OllamaSetup;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Passo 1 - Zero intervenção Humana (Garante que os models locais existam).
        OllamaSetup.prepareEnvironment();

        System.out.println("\n=======================================================");
        System.out.println(">> Inicializando VoiceBot LLM Desktop Environment...");
        System.out.println("=======================================================\n");

        // Passo 2 - Constrói o Roteador, Models em API e Memória.
        HybridRouter voiceBot = new HybridRouter();
        System.out.println("[Sistema] Carregamento completo. Como posso ajudar?\n");

        // Passo 3 - Loop
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\uD83D\uDC64 User > ");
                String input = scanner.nextLine();

                if (input == null || input.trim().equalsIgnoreCase("sair")) {
                    System.out.println("Encerrando bot...");
                    break;
                }
                if (input.trim().isEmpty())
                    continue;

                try {
                    String resposta = voiceBot.chat(input);
                    System.out.println("\n\uD83E\uDD16 Assistente > " + resposta + "\n");
                } catch (Exception e) {
                    System.err.println("\n[ERRO CRÍTICO] Loop de conversa interceptado: " + e.getMessage());
                }
            }
        }
    }
}
