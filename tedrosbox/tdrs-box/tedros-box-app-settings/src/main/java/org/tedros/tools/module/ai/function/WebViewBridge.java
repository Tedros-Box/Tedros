package org.tedros.tools.module.ai.function;

import java.awt.Desktop;
import java.net.URI;

import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class WebViewBridge {

	private final WebView webview;
	
	public WebViewBridge(WebView webview) {
		this.webview = webview;
		WebEngine we = this.webview.getEngine(); 
		we.setJavaScriptEnabled(true);
		JSObject window = (JSObject) we.executeScript("window");
		window.setMember("app", this);
	}
	
	public void run(String content) {
		getWebEngine().executeScript("appendAIResponse(" + toJSString(content) + ")");
	}

	private String toJSString(String content) {
	    // Escapa o conteúdo para ser uma string JS válida
	    return "\"" + content.replace("\\", "\\\\")
	                          .replace("\"", "\\\"")
	                          .replace("\n", "\\n")
	                          .replace("\r", "\\r") + "\"";
	}
	
	private WebEngine getWebEngine() {
		return webview.getEngine();
	}
	
	public void openExternalLink(String url) {
        System.out.println("Solicitado para abrir link externo: " + url);
        try {
            // Usa a classe Desktop para abrir o navegador padrão do sistema
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}