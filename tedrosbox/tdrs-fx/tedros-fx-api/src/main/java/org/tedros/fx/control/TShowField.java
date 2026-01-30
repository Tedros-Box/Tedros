package org.tedros.fx.control;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.tedros.api.descriptor.ITComponentDescriptor;
import org.tedros.app.component.ITComponent;
import org.tedros.core.TLanguage;
import org.tedros.fx.converter.TConverter;
import org.tedros.fx.domain.TDateStyle;
import org.tedros.fx.domain.TLayoutType;
import org.tedros.fx.form.TFieldBox;
import org.tedros.fx.form.TFieldBoxBuilder;
import org.tedros.fx.util.TMaskUtil;
import org.tedros.util.TDateUtil;
import org.tedros.util.TLoggerUtil;

import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * Read and show a field value.
 * Refactored to support ObservableList properly.
 * * @author Davis Gordon
 * @refactor Gemini AI
 */
public class TShowField extends StackPane implements ITField, ITComponent {

	private Pane mainContainer;
    private String tComponentId;
    private Observable value;
    private TField[] fields;
    private ITComponentDescriptor descriptor;
    private TLayoutType layout = TLayoutType.HBOX;
    
    
    public TShowField(TLayoutType layout, Observable value, TField... fields) {
        this(layout, value, null, fields);
    }

    public TShowField(TLayoutType layout, Observable value, ITComponentDescriptor descriptor, TField... fields) {
        this.descriptor = descriptor;
        this.layout = layout != null ? layout : TLayoutType.HBOX;
        this.value = value;
        this.fields = fields;
        try {
            init();
        } catch (Exception e) {
            TLoggerUtil.error(getClass(), e.getMessage(), e);
        }
    }

    public TShowField(Observable value, TField... fields) {
        this(TLayoutType.HBOX, value, null, fields);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable tValueProperty() {
        return value;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void init() {
        super.setAlignment(Pos.TOP_LEFT); // Geralmente listas ficam melhor alinhadas ao topo/esquerda
        
        // 1. Tratamento para Listas (ObservableList ou ListProperty)
        if (isListType(value)) {
            ObservableList<?> obsList = extractObservableList(value);
            
            // Listener para mudanças na estrutura da lista (add/remove)
            if(obsList != null) {
            	obsList.addListener((ListChangeListener) c -> updateListView(obsList));
			}
            // Se for uma ListProperty, também ouve a troca da lista inteira (set)
            if (value instanceof Property property) {
            	property.addListener((obs, oldVal, newVal) -> updateListView(extractObservableList(value)));
            }
            
            updateListView(obsList);

        } 
        // 2. Tratamento para Propriedades Simples/Objetos (Property)
        else if (value instanceof ObservableValue observableValue) {
        	observableValue.addListener((obs, oldVal, newVal) -> updateSingleView(newVal));            
            Object val = ((ObservableValue) value).getValue();
            updateSingleView(val);
        }
    }

    /**
     * Verifica se o Observable é um tipo de lista suportado.
     */
    private boolean isListType(Observable value) {
        return value instanceof ObservableList || value instanceof ListProperty;
    }

    /**
     * Extrai a ObservableList independentemente se veio de uma ListProperty ou já é a lista.
     */
    private ObservableList<?> extractObservableList(Observable value) {
        if (value instanceof ListProperty) {
            return ((ListProperty<?>) value).get();
        } else if (value instanceof ObservableList) {
            return (ObservableList<?>) value;
        }
        return null;
    }

    /**
     * Constrói a visualização para uma lista de itens.
     * Estratégia: Cria um VBox (vertical) onde cada filho é a visualização de um item.
     */
    private void updateListView(List<?> list) {
        this.getChildren().clear();
        
        // Container principal da lista é sempre Vertical para empilhar os itens
        VBox listContainer = new VBox(10); 
        listContainer.setAlignment(Pos.TOP_LEFT);
        
        if (list != null) {
            for (Object item : list) {
                if (item != null) {
                    try {
                        // Cria o layout do item (HBox, FlowPane, etc) baseado na configuração
                        Pane itemPane = createItemPane(item);
                        itemPane.getStyleClass().add("t-show-field-list-item"); // CSS hook
                        listContainer.getChildren().add(itemPane);
                    } catch (Exception e) {
                        TLoggerUtil.error(getClass(), "Error building list item view", e);
                    }
                }
            }
        }
        
        this.mainContainer = listContainer;
        super.getChildren().add(mainContainer);
    }

    /**
     * Constrói a visualização para um único objeto.
     */
    private void updateSingleView(Object item) {
        this.getChildren().clear();
        try {
            // Se o item for nulo, ainda podemos querer mostrar os labels vazios ou nada
            if (item != null || (fields != null && fields.length > 0)) {
                this.mainContainer = createItemPane(item);
                super.getChildren().add(mainContainer);
            }
        } catch (Exception e) {
            TLoggerUtil.error(getClass(), "Error building single view", e);
        }
    }

    /**
     * Cria o Pane contendo os campos de um objeto específico.
     * Respeita o TLayoutType configurado (HBOX, VBOX, FLOWPANE).
     */
    private Pane createItemPane(Object item) throws Exception {
        Pane pane = buildLayoutPane();
        
        if (fields != null && fields.length > 0) {
            // Se tem campos definidos na Annotation, itera sobre eles
            for (TField f : fields) {
                String v = getValue(item, f);
                TFieldBox fb = this.buildFieldBox(v, f);
                
                // Aplica margens baseadas no layout
                applyMargins(pane, fb);
                pane.getChildren().add(fb);
            }
        } else {
            // Se não tem campos definidos, exibe o toString do objeto
            String v = getValue(item);
            Node c = buildNode(TLanguage.getInstance(null).getString(v));
            pane.getChildren().add(c);
        }
        
        return pane;
    }

    private Pane buildLayoutPane() {
        switch (layout) {
            case FLOWPANE:
                FlowPane fp = new FlowPane();
                fp.setPrefWrapLength(USE_COMPUTED_SIZE);
                fp.setVgap(10);
                fp.setHgap(10);
                return fp;
            case VBOX:
                return new VBox(10);
            case HBOX:
            default:
                return new HBox(10);
        }
    }

    private void applyMargins(Pane pane, Node child) {
        Insets margin = new Insets(0, 10, 0, 0); // Default HBox/Flow margin
        if (layout == TLayoutType.VBOX) {
            margin = new Insets(0, 0, 10, 0);
        }

        if (pane instanceof HBox) {
            HBox.setMargin(child, margin);
        } else if (pane instanceof VBox) {
            VBox.setMargin(child, margin);
        } else if (pane instanceof FlowPane) {
            FlowPane.setMargin(child, margin);
        }
    }

    private TFieldBox buildFieldBox(String value, TField f) {
        TLabel l = StringUtils.isNotBlank(f.getLabel()) 
                ? new TLabel(TLanguage.getInstance(null).getString(f.getLabel())) 
                : null;
        
        if (l != null) l.setId("t-form-control-label");

        // LÓGICA ALTERADA AQUI: Decide entre HTML ou Texto Simples
        Node c;
        if (f.isRenderHtml()) {
            c = buildHtmlNode(value);
        } else {
            c = buildNode(TLanguage.getInstance(null).getString(value));
        }

        TFieldBox box = new TFieldBox(f.getName(), l, c, f.getLabelPosition());
        box.setId(null);
        
        if (descriptor != null)
            TFieldBoxBuilder.parse(descriptor, box);
            
        return box;
    }
    
    /**
     * Constrói um WebView leve para exibir o conteúdo HTML.
     */
    private Node buildHtmlNode(String htmlContent) {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        
        // Configurações visuais iniciais
        webView.setContextMenuEnabled(false); // Desabilita menu de contexto (opcional)
        webView.setPrefHeight(20); // Altura inicial pequena
        
        // Remove o fundo branco padrão para tentar integrar melhor ao form (hack de CSS no conteúdo)
        // Se o seu tema for escuro, o ideal é injetar CSS no htmlContent.

        // Carrega o conteúdo
        if (htmlContent != null) {
        	//String style = "<style>body { font-family: 'Segoe UI', sans-serif; font-size: 12px; margin: 0; padding: 0; }</style>";
        	engine.loadContent(htmlContent);
        }

        // TRUQUE DE REDIMENSIONAMENTO:
        // O WebView não se ajusta ao conteúdo (height) nativamente.
        // Precisamos ouvir quando o load termina e rodar um JS para pegar a altura.
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Ajusta a altura baseada no scrollHeight do documento HTML
                try {
                    Object result = engine.executeScript("document.body.scrollHeight");
                    if (result instanceof Integer value) {
                        webView.setPrefHeight(value + 10D); // +10 padding
                    } else if (result instanceof Double value) {
                        webView.setPrefHeight(value + 10);
                    }
                } catch (Exception e) {
                   // Falha silenciosa no resize, mantém default
                }
            }
        });

        return webView;
    }

    private Node buildNode(String value) {
        TLabel c = new TLabel(value);
        c.setId("t-form-label-field-value");
        return c;
    }

    // --- Métodos de Reflexão e Extração de Valor (Refinados) ---

    private String getValue(Object obj) {
        if (obj == null) return "";
        if (obj instanceof Property) {
            obj = ((Property<?>) obj).getValue();
        }
        return obj != null ? obj.toString() : "";
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String getValue(Object obj, TField f) throws Exception {
        if (obj == null) return "";

        String v = "";
        
        // Se o field name for vazio, usa o próprio objeto, senão busca via reflexão
        Object t = StringUtils.isNotBlank(f.getName()) 
                ? getObject(f.getName(), obj) 
                : obj;

        if (t != null) {
            if (t instanceof Property property) 
                t = property.getValue();

            // Verifica se há conversor customizado
            if (f.getConverter() != null && f.getConverter() != TConverter.class) {
                TConverter c = f.getConverter().getDeclaredConstructor().newInstance();
                c.setComponentDescriptor(descriptor);
                c.setIn(t);
                return (String) c.getOut();
            }

            // Formatação padrão baseada no tipo
            if (t instanceof Date date) {
                if (StringUtils.isNotBlank(f.getFormat())) {
                    v = TDateUtil.format(date, f.getFormat());
                } else {
                    v = TDateUtil.create(TLanguage.getLocale())
                            .setDateStyle(f.getDateStyle() != null ? f.getDateStyle().getValue() : TDateStyle.DEFAULT.getValue())
                            .setTimeStyle(f.getTimeStyle() != null ? f.getTimeStyle().getValue() : null)
                            .format((Date) t);
                }
            } else {
                v = TLanguage.getInstance().getString(t.toString());
                if (StringUtils.isNotBlank(f.getMask())) {
                    v = TMaskUtil.applyMask(v, f.getMask());
                } else if (StringUtils.isNotBlank(f.getFormat())) {
                    try {
                        v = String.format(f.getFormat(), t);
                    } catch (Exception e) {
                        v = t.toString(); // Fallback se o formato falhar
                    }
                }
            }
        }
        return v;
    }
    
    private Object getObject(String path, Object obj) {
        if (obj == null) return null;
        
        Object o = null;
        String fieldName = path.contains(".") ? StringUtils.substringBefore(path, ".") : path;
        String remainingPath = path.contains(".") ? StringUtils.substringAfter(path, ".") : "";

        Class<?> target = obj.getClass();
        
        // Loop para encontrar o campo na hierarquia de classes
        while (target != null && target != Object.class) {
            try {
                Field f = target.getDeclaredField(fieldName);
                f.setAccessible(true);
                o = f.get(obj);
                break; // Achou, para o loop
            } catch (NoSuchFieldException e) {
                target = target.getSuperclass(); // Tenta na superclasse
            } catch (IllegalAccessException e) {
                TLoggerUtil.error(getClass(), "Access denied to field: " + fieldName, e);
                return null;
            }
        }

        // Recursividade para propriedades aninhadas (ex: funcionario.endereco.rua)
        if (o != null && StringUtils.isNotBlank(remainingPath)) {
            return getObject(remainingPath, o);
        }

        return o;
    }

    @Override
    public void settFieldStyle(String style) {
        setStyle(style);
    }

    @Override
    public String gettComponentId() {
        return tComponentId;
    }

    @Override
    public void settComponentId(String id) {
        tComponentId = id;
    }
}