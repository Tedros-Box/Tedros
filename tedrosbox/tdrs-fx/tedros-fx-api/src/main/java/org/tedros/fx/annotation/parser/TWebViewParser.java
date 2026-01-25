package org.tedros.fx.annotation.parser;

import java.util.UUID;

import org.tedros.fx.annotation.parser.engine.TAnnotationParser;
import org.tedros.fx.annotation.scene.web.TWebView;
import org.tedros.fx.descriptor.TComponentDescriptor;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class TWebViewParser extends TAnnotationParser<TWebView, WebView> {

    @Override
    public void parse(TWebView ann, WebView w, String... byPass) throws Exception {
        
        // Lógica do Listener para redimensionamento automático
        ChangeListener<Scene> chl = (observable, oldScene, newScene) -> {
            if (newScene != null) {
                // Verifica se o usuário definiu ALGUM tamanho fixo na anotação
                boolean hasFixedSize = ann.minHeight() > 0 || ann.minWidth() > 0 ||
                                       ann.maxHeight() > 0 || ann.maxWidth() > 0 ||
                                       ann.prefHeight() > 0 || ann.prefWidth() > 0;

                // Se NÃO houver tamanho definido, vincula ao tamanho da Scene
                if (!hasFixedSize) {
                    // O bind garante que se a Scene mudar de tamanho, o WebView acompanha
                    w.prefWidthProperty().bind(newScene.widthProperty());
                    w.prefHeightProperty().bind(newScene.heightProperty());
                }
            }
        };
        
        w.sceneProperty().addListener(new WeakChangeListener<>(chl));
        
        // Adiciona ao repositório para evitar coleta de lixo prematura do Listener
        super.getComponentDescriptor().getForm().gettObjectRepository().add(UUID.randomUUID().toString(), chl);
        
        // --- Definições manuais (só serão aplicadas se existirem na anotação) ---
        
        if(ann.minHeight() > 0)
            w.setMinHeight(ann.minHeight());
        
        if(ann.minWidth() > 0)
            w.setMinWidth(ann.minWidth());
        
        if(ann.maxHeight() > 0)
            w.setMaxHeight(ann.maxHeight());
        
        if(ann.maxWidth() > 0)
            w.setMaxWidth(ann.maxWidth());
        
        if(ann.prefWidth() > 0)
            w.setPrefWidth(ann.prefWidth());
        
        if(ann.prefHeight() > 0)
            w.setPrefHeight(ann.prefHeight());
        
        if(ann.zoom() > 0)
            w.setZoom(ann.zoom());
        
        super.parse(ann, w, "engine", "minHeight","minWidth", "maxHeight", "maxWidth", "prefWidth", "prefHeight", "zoom");
    
        WebEngine e = w.getEngine();
        TWebEngineParser p = new TWebEngineParser();
        p.setComponentDescriptor(new TComponentDescriptor(super.getComponentDescriptor(), null));
        p.parse(ann.engine(), e);
    }
}