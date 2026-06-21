package com.tedros.hybrid.tools;

import dev.langchain4j.agent.tool.Tool;

public class SystemTools {

    @Tool("Sempre deve ser usado para abrir janelas ou interface graficas no sistema (exemplo: 'abrir janela do Excel', 'abrir calculadora').")
    public String abrirJanelaNoSistema(String nomeAplicativo) {
        System.out.println(" ⚡ [TOOL CALL] Inicializando UI gráfica invocada para o app: " + nomeAplicativo);
        return "Notifique que você acabou de concluir a abertura da janela do aplicativo: " + nomeAplicativo;
    }

    @Tool("Sempre deve ser usado para realizar comandos remotos, ping, buscas, terminal, ou utilidades restritas.")
    public String executarComandoSistema(String comando) {
        System.out.println(" ⚡ [TOOL CALL] Executando simulação de sub-processo backend: " + comando);
        return "[RESULTADO SUCESSO DO SISTEMA]: o comando restrito " + comando + " finalizou a operação corretamente no root.";
    }
}
