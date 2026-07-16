/**
 * 
 */
package org.tedros.tools.ai.setting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.tedros.ai.function.TFunction;
import org.tedros.ai.function.TFunctionHelper;
import org.tedros.ai.service.AiTerosContext;
import org.tedros.ai.service.IAiTerosService;
import org.tedros.api.descriptor.ITComponentDescriptor;
import org.tedros.core.TLanguage;
import org.tedros.core.context.TedrosContext;
import org.tedros.core.message.TMessageType;
import org.tedros.core.repository.TRepository;
import org.tedros.fx.TUsualKey;
import org.tedros.fx.control.TButton;
import org.tedros.fx.form.TSetting;
import org.tedros.fx.modal.TMessageBox;
import org.tedros.tools.ToolsKey;
import org.tedros.tools.ai.model.TerosMV;
import org.tedros.tools.module.ai.function.ShowHtmlAiResponse;
import org.tedros.tools.module.ai.settings.AiChatUtil;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.event.WeakEventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;


/**
 * @author Davis Gordon
 *
 */
public class TerosSetting extends TSetting {

	private static final String TEROS_NAME = "Teros";	
	protected static IAiTerosService TEROS;
	
	private AiChatUtil util = new AiChatUtil();;
    private TRepository repo = new TRepository();;
    private boolean scrollFlag = false;
    
	/**
	 * @param descriptor
	 */
	public TerosSetting(ITComponentDescriptor descriptor) {
		super(descriptor);
		ChangeListener<Instant> reloadListener = (a,o,n)->{
			checkAIServiceAvailability();	
		};
		repo.add("reloadListener", reloadListener);				
		AiTerosContext.reloadProperty().addListener(new WeakChangeListener<>(reloadListener));
	}	

	@Override
	public void run() {
		
		super.getForm().gettPresenter().getView().gettProgressIndicator().setMediumLogo();
		
		listenSendButton();
		listenClearButton();
		listenResetButton();
		listenShowViewerButton();
		
		TextArea text = super.getControl("prompt");
		text.setPromptText("[Enter] + [Shift] = "+TLanguage.getInstance().getString(TUsualKey.SEND));
		text.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER && event.isShiftDown()) {
                sendAction();
                event.consume();
            }
        });
		
		// auto scroll
		ScrollPane sp = (ScrollPane) super.getLayout("title"); 
		
		sp.setVvalue(1.0);
		sp.vvalueProperty().addListener((a,o,n)->{
			if(scrollFlag) {
				sp.setVvalue(sp.getVmax());
				if(n.doubleValue()==sp.getVmax())
					scrollFlag = false;
			}
		});
		
		checkAIServiceAvailability();
		
	}
	
	private void checkAIServiceAvailability() {
		
		super.getForm().gettPresenter().getView().tHideModal();
		
		if(AiTerosContext.getArtificialIntelligenceEnabled()) {
				
			List<String> messages = new ArrayList<>();
			
			if(StringUtils.isBlank(AiTerosContext.getAiApiKey()))
				messages.add(TLanguage.getInstance().getString(ToolsKey.MESSAGE_AI_KEY_REQUIRED));
			
			if(StringUtils.isBlank(AiTerosContext.getAiModel()))
				messages.add(TLanguage.getInstance().getString(ToolsKey.MESSAGE_AI_MODEL_REQUIRED));
			
			if(StringUtils.isBlank(AiTerosContext.getAiSystemPrompt()))
				messages.add(TLanguage.getInstance().getString(ToolsKey.MESSAGE_AI_PROMPT_REQUIRED));
			
			if(!messages.isEmpty())
				super.getForm().gettPresenter().getView()
				.tShowModal(new TMessageBox(messages, TMessageType.WARNING), false);
			else
				createAiService();
			
		}else {
			TEROS = null;
			super.getForm().gettPresenter().getView()
			.tShowModal(new TMessageBox(TLanguage.getInstance()
					.getString(ToolsKey.MESSAGE_AI_DISABLED), TMessageType.WARNING), false);
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void createAiService() {
		
		if(!AiTerosContext.getArtificialIntelligenceEnabled()) {
			return;
		}			
		
		TEROS = AiTerosContext.newInstanceAiTerosService();		
		TFunction[] arr = new TFunction[] {
				TFunctionHelper.listAllViewPathFunction(),
				TFunctionHelper.listAllAppsFunction(),
				TFunctionHelper.getViewInfoFunction(),
				TFunctionHelper.callUpViewFunction(),
				TFunctionHelper.getModelBeingEditedFunction(),
				TFunctionHelper.getViewModelFunction(),
				TFunctionHelper.getPreferencesFunction(),
				TFunctionHelper.getCreateFileFunction()};		
		arr = ArrayUtils.addAll(arr, TFunctionHelper.getAppsFunction());
		TEROS.createFunctionExecutor(arr);
	}

	/**
	 * @param mv
	 */
	private void listenClearButton() {
		Runnable action = () -> {
			TerosMV mv = getModelView();
			mv.getPrompt().setValue(null);
			TextArea text = super.getControl("prompt");
			if(text != null) text.setText("");
		};
		EventHandler<ActionEvent> evAction = e -> action.run();
		EventHandler<javafx.scene.input.MouseEvent> evMouse = e -> action.run();
		
		repo.add("evActionClear", evAction);
		repo.add("evMouseClear", evMouse);
		
		TButton clearBtn = (TButton) getDescriptor()
				.getFieldDescriptor("clearBtn").getComponent();
		clearBtn.setOnAction(new WeakEventHandler<>(evAction));
		clearBtn.setOnMousePressed(new WeakEventHandler<>(evMouse));
	}

	private void listenResetButton() {
		EventHandler<ActionEvent> evAction = e -> resetAction();
		EventHandler<javafx.scene.input.MouseEvent> evMouse = e -> resetAction();
		
		repo.add("evActionReset", evAction);
		repo.add("evMouseReset", evMouse);
		
		TButton resetBtn = (TButton) super.getDescriptor()
				.getFieldDescriptor("resetBtn").getComponent();
		resetBtn.setOnAction(new WeakEventHandler<>(evAction));
		resetBtn.setOnMousePressed(new WeakEventHandler<>(evMouse));
	}
	
	private void listenShowViewerButton() {
		EventHandler<ActionEvent> evAction = e -> ShowHtmlAiResponse.showViewer();
		EventHandler<javafx.scene.input.MouseEvent> evMouse = e -> ShowHtmlAiResponse.showViewer();
		
		repo.add("evActionViewer", evAction);
		repo.add("evMouseViewer", evMouse);
		
		TButton showViewerBtn = (TButton) getDescriptor()
				.getFieldDescriptor("showViewerBtn").getComponent();
		showViewerBtn.setOnAction(new WeakEventHandler<>(evAction));
		showViewerBtn.setOnMousePressed(new WeakEventHandler<>(evMouse));
	}
	
	private void listenSendButton() {
		// Action event (Teclado - Enter/Espaço sobre o botão)
		EventHandler<ActionEvent> evAction = e -> {
			sendAction();
		};
		// Mouse event (Clique do mouse)
		EventHandler<javafx.scene.input.MouseEvent> evMouse = e -> {
			sendAction();
		};
		repo.add("evAction", evAction);
		repo.add("evMouse", evMouse);
		
		TButton sendBtn = (TButton) getDescriptor()
				.getFieldDescriptor("sendBtn").getComponent();
		sendBtn.setOnAction(new WeakEventHandler<>(evAction));
		sendBtn.setOnMousePressed(new WeakEventHandler<>(evMouse));
	}

	private void sendAction() {
		TerosMV mv = getModelView(); 
		TextArea text = super.getControl("prompt");
		
		String prompt = text.getText();
		if(StringUtils.isBlank(prompt))
			return;
		
		try {
			showMsg(TedrosContext.getLoggedUser().getName(), prompt);
			mv.getPrompt().setValue(null);
			text.setText(""); // Garante a limpeza visual imediata
			TerosProcess p = new TerosProcess();
			p.stateProperty().addListener((a,o,n)->{
				String msg = p.getValue();
				if(n.equals(State.SUCCEEDED) && StringUtils.isNotBlank(msg) ) 
					showMsg(TEROS_NAME, msg);
			});
			super.getForm().gettPresenter().getView()
			.gettProgressIndicator().bind(p.runningProperty());
			p.setPrompt(prompt);
			p.startProcess();
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * @param m
	 */
	private void resetAction(){
		scrollFlag = true;
		VBox gp = super.getLayout("messages");
		gp.getChildren().clear();
		TEROS.cleanMessageHistory();
	}

	/**
	 * @param m
	 */
	private void showMsg(String user, String txt){
		scrollFlag = true;
		StackPane p1 = util.buildMsgPane(user, txt, new Date(), 520, false);
		GridPane.setVgrow(p1, Priority.ALWAYS);
		VBox gp = super.getLayout("messages");
		gp.getChildren().add(p1);
	}
	
	@Override
	public void dispose() {
		repo.clear();
		repo = null;
		TEROS = null;
		TerosMV mv = getModelView();
		mv.getMessages().clear();
	}

}
