package org.tedros.tools.module.ai.component;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.tedros.ai.function.TFunction;
import org.tedros.ai.function.TFunctionHelper;
import org.tedros.ai.service.AiProviderPreference;
import org.tedros.ai.service.AiServiceProvider;
import org.tedros.ai.service.AiTerosContext;
import org.tedros.ai.service.IAiTerosService;
import org.tedros.ai.web.TerosWebViewBridge;
import org.tedros.api.descriptor.ITComponentDescriptor;
import org.tedros.api.presenter.view.ITView;
import org.tedros.core.TLanguage;
import org.tedros.core.control.ITProgressIndicator;
import org.tedros.core.control.TProgressIndicator;
import org.tedros.fx.TUsualKey;
import org.tedros.fx.component.ITComponent;
import org.tedros.fx.control.TButton;
import org.tedros.fx.control.TLabel;
import org.tedros.fx.domain.TLabelPosition;
import org.tedros.fx.form.TFieldBox;
import org.tedros.fx.layout.TToolBar;
import org.tedros.fx.modal.TMessageBox;
import org.tedros.fx.process.TProcess;
import org.tedros.fx.process.TTaskImpl;
import org.tedros.tools.ToolsKey;
import org.tedros.tools.module.ai.function.ShowHtmlAiResponse;
import org.tedros.util.TLoggerUtil;
import org.tedros.util.TedrosFolder;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

public class AiProviderComparisonComponent extends VBox implements ITComponent {
	
	private static final Logger LOGGER = TLoggerUtil.getLogger(AiProviderComparisonComponent.class);
	
	private static final String GEMINI = "Gemini";
	private static final String GROK = "Grok";
	private static final String OPEN_AI = "OpenAI";

	private static final String AI_PROVIDER_PROPERTIE_MISSING = "<div style='color:red'>%s configuration is missing. Please set API Key and Model in system properties.</div>";

	private static final String SYSTEM_PROMPT = """
		    You are Teros, the official AI assistant of the Tedros System.
		    
		    Current Context: **COMPARISON & ANALYSIS MODE**
		    You are operating inside a specific comparison component where the user tests different AI providers.
		    
		    ==================================================
		    OUTPUT FORMAT (CRITICAL)
		    ==================================================
		    1. **RAW HTML ONLY**: Return ONLY the HTML string.
		    2. **NO MARKDOWN**: Never use code blocks (e.g., ```html).
		    3. **NO ESCAPING**: Do not escape tags (use <div>, not &lt;div&gt;).
		    4. **STRUCTURE**: Do not use <html>, <head> or <body> tags. Start directly with content (<h3>, <p>, <div>).
		    5. **STYLING**: Use inline CSS only. Dark text (#333) on transparent/light background.
		    
		    ==================================================
		    FUNCTION CALLING & SECURITY PROTOCOLS
		    ==================================================
		    You have access to system tools, but strict rules apply in this mode:
		    
		    1. **READ-ONLY ALLOWED**: You MAY use functions to fetch data, list views, or query system info.
		    2. **NAVIGATION FORBIDDEN**: You MUST NOT call functions that open windows, switch views, or navigate the UI (e.g., DO NOT use `callUpViewFunction` or `show_html_content`).
		    3. **WRITE FORBIDDEN**: DO NOT use functions that create files or insert records.		    
		    
		    If the user asks to "Open Screen X":
		    - Do NOT call the view function.
		    - Instead, fetch the data regarding "Screen X" and display the *information* directly in the HTML response.
		    - Explain that in Comparison Mode, navigation is disabled.
		    
		    ==================================================
		    RESPONSE LOGIC
		    ==================================================
		    1. Analyze the user request.
		    2. If tools are needed to GET data, call them.
		    3. Process the result.
		    4. Format the final answer in clean, semantic HTML5.
		    5. Ensure the response language matches the user's input (PT/EN).
		    
		    ==================================================
		    ERROR HANDLING
		    ==================================================
		    If a tool fails or requires navigation parameters you cannot fulfill:
		    - Return an HTML error message (<div style='color:red'>...</div>).
		    - Suggest the user try the main chat for operational tasks.
		    """;

    private TextArea promptArea;
    private TButton sendButton;
    private TButton clearButton;

    private WebView openaiWebView;
    private WebView grokWebView;
    private WebView geminiWebView;

    private TerosWebViewBridge openaiBridge;
    private TerosWebViewBridge grokBridge;
    private TerosWebViewBridge geminiBridge;

    private TerosService openaiService;
    private TerosService grokService;
    private TerosService geminiService;
    
    private ITProgressIndicator openaiProgressIndicator;
    private ITProgressIndicator grokProgressIndicator;
    private ITProgressIndicator geminiProgressIndicator;
    
    private SimpleBooleanProperty loadingProperty = new SimpleBooleanProperty(false);
        
    private AiProviderPreference openAiPref;
    private AiProviderPreference grokPref;
    private AiProviderPreference geminiPref;
    
    @SuppressWarnings("rawtypes")
	private ITView view;

    public AiProviderComparisonComponent() {
        super(10);
        setPadding(new Insets(10));
        setFillWidth(true);
    }

    @Override
    public void tInitializeComponent(ITComponentDescriptor descriptor) {
    	getProviderProperties();
        initializeUI();
        loadWebViews();
        configAiServices();
    }
    
    @Override
	public void tStopComponent() {
		grokService.cancel();
		openaiService.cancel();
		geminiService.cancel();
	}

	private void configAiServices() {		
        try {
            // OpenAI Service
            openaiService = new TerosService();
            openaiService.buildIaTerosService(SYSTEM_PROMPT, AiServiceProvider.OPENAI);
            openaiProgressIndicator.bind(openaiService.runningProperty());
            
            openaiService.onFailedProperty().addListener((obs, oldVal, newVal) -> 
            	Platform.runLater(() -> showAlert(openaiService.getException().getMessage())));
        	
            openaiService.setOnSucceeded(e -> {
            	String htmlMessage = openaiService.getValue();
            	updateWebViewContent(AiServiceProvider.OPENAI, openaiBridge, htmlMessage);
        	});
            
            // Grok Service
            grokService = new TerosService();
            grokService.buildIaTerosService(SYSTEM_PROMPT, AiServiceProvider.GROK);
            grokProgressIndicator.bind(grokService.runningProperty());
            
            grokService.onFailedProperty().addListener((obs, oldVal, newVal) -> 
        	    Platform.runLater(() -> showAlert(grokService.getException().getMessage())));
        	
            grokService.setOnSucceeded(e -> {
            	String htmlMessage = grokService.getValue();
            	updateWebViewContent(AiServiceProvider.GROK, grokBridge, htmlMessage);
        	});
        	
            // Gemini Service
            geminiService = new TerosService();
            geminiService.buildIaTerosService(SYSTEM_PROMPT, AiServiceProvider.GEMINI);
            geminiProgressIndicator.bind(geminiService.runningProperty());
            
            geminiService.onFailedProperty().addListener((obs, oldVal, newVal) ->
            	Platform.runLater(() -> showAlert("Erro na análise: " + geminiService.getException().getMessage())));
        	
            geminiService.setOnSucceeded(e -> {
            	String htmlMessage = geminiService.getValue();
            	updateWebViewContent(AiServiceProvider.GEMINI, geminiBridge, htmlMessage);
        	});
            
            loadingProperty.bind(openaiService.runningProperty()
					.or(grokService.runningProperty())
					.or(geminiService.runningProperty()));
            
            sendButton.disableProperty().bind(loadingProperty);

        } catch (Exception e) {
            TLoggerUtil.error(AiProviderComparisonComponent.class, "Failed to load AI response HTML template.", e);
        }
        
	}

	private void loadWebViews() {
		
		openaiWebView.getEngine().documentProperty().addListener((a, o, n)->{
			if(n==null) return;
			LOGGER.info("Creating TerosWebViewBridge for {}", AiServiceProvider.OPENAI.name());
			openaiBridge = new TerosWebViewBridge(openaiWebView);
			LOGGER.info("The TerosWebViewBridge created for {}", AiServiceProvider.OPENAI.name());
		    if(!openAiPref.hasApyKeyAndModel())
	        	updateWebViewContent(AiServiceProvider.OPENAI, openaiBridge, AI_PROVIDER_PROPERTIE_MISSING.formatted(OPEN_AI));
		});
		
		grokWebView.getEngine().documentProperty().addListener((a, o, n)->{
			if(n==null) return;
			LOGGER.info("Creating TerosWebViewBridge for {}", AiServiceProvider.GROK.name());
        	grokBridge = new TerosWebViewBridge(grokWebView);
        	LOGGER.info("The TerosWebViewBridge created for {}", AiServiceProvider.GROK.name());
		    if (!grokPref.hasApyKeyAndModel())
	        	updateWebViewContent(AiServiceProvider.GROK, grokBridge, AI_PROVIDER_PROPERTIE_MISSING.formatted(GROK));
		});

		geminiWebView.getEngine().documentProperty().addListener((a, o, n)->{
			if(n==null) return;
			LOGGER.info("Creating TerosWebViewBridge for {}", AiServiceProvider.GEMINI.name());
			geminiBridge = new TerosWebViewBridge(geminiWebView);
			LOGGER.info("The TerosWebViewBridge created for {}", AiServiceProvider.GEMINI.name());
		    if (!geminiPref.hasApyKeyAndModel())
	        	updateWebViewContent(AiServiceProvider.GEMINI, geminiBridge, AI_PROVIDER_PROPERTIE_MISSING.formatted(GEMINI));
		});
		
		String templateUrl = "file:" + TedrosFolder.MODULE_FOLDER.getFullPath() + "TCORE_19780222"
		        + java.io.File.separator
		        + "teros_ia_response.html";
		
		openaiWebView.getEngine().load(templateUrl);
		grokWebView.getEngine().load(templateUrl);
		geminiWebView.getEngine().load(templateUrl);
	}

	private void getProviderProperties() {
		openAiPref = AiTerosContext.getAiProviderPreference(AiServiceProvider.OPENAI);
        grokPref = AiTerosContext.getAiProviderPreference(AiServiceProvider.GROK);
        geminiPref = AiTerosContext.getAiProviderPreference(AiServiceProvider.GEMINI);
	}

    private void initializeUI() {
    	TLanguage lang = TLanguage.getInstance();
        // Input Area
        TLabel promptLabel = new TLabel("Prompt:");
        promptArea = new TextArea();
        promptArea.setPromptText(lang.getString(ToolsKey.PROMPT_ENTER_PROMPT_FOR_ALL_MODELS));
        promptArea.setPrefRowCount(4);
        promptArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isShiftDown()) {
            	sendRequests();
                event.consume();
            }
        });
        VBox.setVgrow(promptArea, Priority.ALWAYS); // Allow prompt to grow slightly but mostly content below

        // Controls
        sendButton = new TButton(lang.getString(TUsualKey.SEND));
        sendButton.setOnAction(e -> sendRequests());

        clearButton = new TButton(lang.getString(TUsualKey.CLEAR));
        clearButton.setOnAction(e -> promptArea.clear());

        TToolBar controls = new TToolBar(sendButton, clearButton);

        VBox inputBox = new VBox(5, promptLabel, promptArea, controls);

        // Output Area (WebViews)
        openaiWebView = new WebView();
        grokWebView = new WebView();
        geminiWebView = new WebView();

        SplitPane splitPane = new SplitPane();
        splitPane.setStyle("-fx-background-color: transparent;");
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getItems().addAll(wrapWebView(openaiWebView, OPEN_AI), wrapWebView(grokWebView, GROK),
                wrapWebView(geminiWebView, GEMINI));
        splitPane.setDividerPositions(0.33, 0.66);
        splitPane.setMaxHeight(400);

        VBox.setVgrow(splitPane, Priority.ALWAYS);

        this.getChildren().addAll(inputBox, splitPane);
    }

    private StackPane wrapWebView(WebView wv, String title) {
        TLabel label = new TLabel(title);
        
        TFieldBox fieldBox = new TFieldBox(title, label, wv, TLabelPosition.TOP);
        fieldBox.setId("t-fieldbox-info");
        
        StackPane stack = new StackPane(fieldBox);
        
        if(title.equals(GROK)) {
        	grokProgressIndicator = new TProgressIndicator(stack);
        } else if(title.equals(GEMINI)) {
			geminiProgressIndicator = new TProgressIndicator(stack);
		} else if(title.equals(OPEN_AI)) {
			openaiProgressIndicator = new TProgressIndicator(stack);
		}
        
        return stack;
    }
    
    private void showAlert(String content) {
    	view.tShowModal(new TMessageBox(content), true); 
    }

    private void sendRequests() {
        String prompt = promptArea.getText();
        if (prompt == null || prompt.trim().isEmpty()) {
            return;
        }
        
        executeComparison(prompt);
        promptArea.clear();
    }

    private void executeComparison(String prompt) {    	
    	
    	if(!openAiPref.hasApyKeyAndModel() && !grokPref.hasApyKeyAndModel() && !geminiPref.hasApyKeyAndModel()) {
			showAlert(TLanguage.getInstance()
					.getString(ToolsKey.MESSAGE_NO_AI_PROVIDER_CONFIGURED));
			return;
		}
                
    	if(openAiPref.hasApyKeyAndModel()) {
	        openaiService.prompt = prompt;
	        openaiService.systemPrompt = SYSTEM_PROMPT;
    	}
        
    	if(grokPref.hasApyKeyAndModel()) {
	        grokService.prompt = prompt;
	        grokService.systemPrompt = SYSTEM_PROMPT;
    	}
        
    	if(geminiPref.hasApyKeyAndModel()) {
    		geminiService.prompt = prompt;
    		geminiService.systemPrompt = SYSTEM_PROMPT;
    	}

    	if(geminiPref.hasApyKeyAndModel()) geminiService.startProcess();
    	if(openAiPref.hasApyKeyAndModel()) openaiService.startProcess();
    	if(grokPref.hasApyKeyAndModel()) grokService.startProcess();
    }
    
    private void updateWebViewContent(AiServiceProvider provider, TerosWebViewBridge bridge, String content) {
        if (bridge != null) {
            Platform.runLater(() -> {
            	LOGGER.info("The Provider {} returned successfully, sending content to the TerosWebViewBridge", provider.name());
            	bridge.run(content);
            });
        }
    }
    
    private static class TerosService extends TProcess<String> {
    	
		private String prompt;
		private String systemPrompt;
		private IAiTerosService iaServ;
		
		@SuppressWarnings("rawtypes")
		public void buildIaTerosService(String sysPrompt, AiServiceProvider provider) {
			try {
	            // Assuming AiTerosServiceFactory and IAiTerosService usage
				iaServ = AiTerosContext.newInstanceWithLangChain4jAdapters(sysPrompt, provider);
	            List<TFunction> lst = new ArrayList<>(); 
	            lst.add(TFunctionHelper.getViewInfoFunction());
	            
	            for(TFunction f : TFunctionHelper.getAppsFunction()) {
	            	
	            	if(f.getClass()==ShowHtmlAiResponse.class || f.getName().contains("create"))
	            		continue;
	            	
	            	TLoggerUtil.info(AiProviderComparisonComponent.class, 
	            			"Adding function %s / %s".formatted(f.getName(), f.getClass().getSimpleName()));
	            	
	            	lst.add(f);
	            }
				
	            iaServ.createFunctionExecutor(lst.toArray(new TFunction[0]));
	            
	            
	        } catch (Exception e) {
	            TLoggerUtil.error(AiProviderComparisonComponent.class, "Error calling " + provider, e);
	        }
		}

		@Override
		protected TTaskImpl<String> createTask() {
			return new TTaskImpl<String>() {
				@Override
				protected String call() throws Exception {
					return iaServ.call(prompt, systemPrompt);
				}

				@Override
				public String getServiceNameInfo() {
					return null;
				}
			};
		}
    }
	
}
