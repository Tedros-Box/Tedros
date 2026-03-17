package com.tedros.hybrid.setup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class OllamaSetup {

    public static void prepareEnvironment() {
        System.out.println("[SETUP] Verificando se o daemon do Ollama está rodando na porta 11434...");
        if (!isOllamaRunning()) {
            System.err.println("[ERRO CRÍTICO] O Ollama não responde em localhost:11434.");
            System.err.println("-> Solução: Abra o aplicativo Ollama no seu PC e tente rodar a POC novamente.");
            System.exit(1);
        }

        System.out.println("[SETUP] Ollama conectado na rede local com sucesso. Verificando modelos...");
        checkAndPullModel("llama3.2:3b");
        //checkAndPullModel("llama3.2:1b");
        System.out.println("[SETUP] ✓ Prontidão de modelos 100% atingida.");
    }

    private static boolean isOllamaRunning() {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:11434/api/tags").openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2500);
            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static void checkAndPullModel(String modelName) {
        if (isModelDownloaded(modelName)) {
            System.out.println("[SETUP] ✓ Modelo local confirmado: " + modelName);
        } else {
            System.out.println("[SETUP] Modelo " + modelName + " não encontrado. Iniciando pull automático via ProcessBuilder...");
            pullModel(modelName);
        }
    }

    private static boolean isModelDownloaded(String modelName) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:11434/api/tags").openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            if (conn.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder content = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) content.append(inputLine);
                in.close();
                
                return content.toString().contains("\"name\":\"" + modelName);
            }
        } catch (Exception e) {
            System.err.println("[SETUP] Erro ao validar listagem: " + e.getMessage());
        }
        return false;
    }

    private static void pullModel(String modelName) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("ollama", "pull", modelName);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            System.out.print("[SETUP-PULL] Baixando " + modelName + " ");
            while ((line = reader.readLine()) != null) {
                // Progresso super simples
                System.out.print(".");
            }
            System.out.println();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("[SETUP] ✓ Modelo " + modelName + " baixado com sucesso!");
            } else {
                System.err.println("\n[ERRO] Falha interna no comando pull para o modelo " + modelName);
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("\n[ERRO] CLI do Ollama indisponível em ambiente. Não foi possível rodar o pull.");
            System.exit(1);
        }
    }
}
