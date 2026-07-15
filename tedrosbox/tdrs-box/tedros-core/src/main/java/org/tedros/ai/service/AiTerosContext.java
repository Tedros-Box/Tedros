package org.tedros.ai.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.tedros.core.context.TedrosContext;
import org.tedros.core.controller.TPropertieController;
import org.tedros.core.domain.TSystemPropertie;
import org.tedros.core.service.remote.TEjbServiceLocator;
import org.tedros.core.setting.model.TPropertie;
import org.tedros.server.query.TCompareOp;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.util.TLoggerUtil;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class AiTerosContext {
	
	private static final Logger LOGGER = TLoggerUtil.getLogger(AiTerosContext.class);
	
	public static final String TOOL_RELAY_ENABLED = "sys.ai.toolrelay.enabled";
	private static final String PROPERTIE_LOADED = "- Propertie {} loaded.";
	private static final String PROPERTIE_NOT_DEFINED = "- Propertie {} not defined!";
	
	private static AiTerosServiceFactory aiServiceFactory;
	
	private static List<TPropertie> properties;
	
	private static ObjectProperty<Instant> reloadProperty;	
	private static BooleanProperty aiEnabledProperty;
	private static BooleanProperty aiToolRelayEnabledProperty;
	private static ObjectProperty<AiServiceProvider> aiServiceProviderProperty;
	private static StringProperty aiApiKeyProperty;
	private static StringProperty aiModelProperty;
	private static StringProperty aiSystemPromptProperty;
	
	static {
		reloadProperty = new SimpleObjectProperty<>(Instant.now());
		aiEnabledProperty = new SimpleBooleanProperty();
		aiToolRelayEnabledProperty = new SimpleBooleanProperty();
		aiModelProperty = new SimpleStringProperty();
		aiApiKeyProperty = new SimpleStringProperty();
		aiServiceProviderProperty = new SimpleObjectProperty<>();
		aiSystemPromptProperty = new SimpleStringProperty();
		loadProperties();
		aiServiceFactory = AiTerosServiceFactory.instance();
	}
	
	private AiTerosContext() {
		
	}
	
	public static void loadProperties() {
		
		LOGGER.info("Starting load ai properties.");
		
		TSelect<TPropertie> select = new TSelect<>(TPropertie.class);
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.AI_ENABLED.getValue());
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.AI_SERVICE_PROVIDER.getValue());
		select.addOrCondition("key", TCompareOp.EQUAL, TOOL_RELAY_ENABLED);
		
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.OPENAI_KEY.getValue());
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.OPENAI_MODEL.getValue());
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.OPENAI_PROMPT.getValue());
		
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.GEMINI_KEY.getValue());
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.GEMINI_MODEL.getValue());
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.GEMINI_PROMPT.getValue());
		
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.GROK_KEY.getValue());
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.GROK_MODEL.getValue());
		select.addOrCondition("key", TCompareOp.EQUAL, TSystemPropertie.GROK_PROMPT.getValue());
		
		try (TEjbServiceLocator loc = TEjbServiceLocator.getInstance()) {
			TPropertieController serv = loc.lookup(TPropertieController.JNDI_NAME);
			TResult<List<TPropertie>> res = serv.search(TedrosContext.getLoggedUser().getAccessToken(), select);
			if(res.getState().equals(TState.SUCCESS) && CollectionUtils.isNotEmpty(res.getValue())) {
				properties = res.getValue();
				
				LOGGER.info("Properties returned for assembly: {}", properties);
				
				properties.stream()
				.filter(p -> p.getKey().equals(TOOL_RELAY_ENABLED))
				.findFirst()
				.ifPresentOrElse(p -> {
					if(StringUtils.isNotBlank(p.getValue())) {
						setAiToolRelayEnabled(BooleanUtils.toBoolean(p.getValue()));
						LOGGER.info(PROPERTIE_LOADED, TOOL_RELAY_ENABLED);
					}else{
						setAiToolRelayEnabled(false);
						LOGGER.info("- Propertie {} set to default value false", TOOL_RELAY_ENABLED);
					}
				}, ()->{
					setAiToolRelayEnabled(false);
					LOGGER.info("- Propertie {} set to default value false", TOOL_RELAY_ENABLED);
				});
				
				properties.stream()
				.filter(p -> p.getKey().equals(TSystemPropertie.AI_ENABLED.getValue()))
				.findFirst()
				.ifPresentOrElse(p -> {
					if(StringUtils.isNotBlank(p.getValue())) {
						setArtificialIntelligenceEnabled(BooleanUtils.toBoolean(p.getValue()));
						LOGGER.info(PROPERTIE_LOADED, TSystemPropertie.AI_ENABLED.getValue());
					}else{
						setArtificialIntelligenceEnabled(false);
						LOGGER.info("- Propertie {} set to default value false", TSystemPropertie.AI_ENABLED.getValue());
					}
				}, ()->{
					setArtificialIntelligenceEnabled(false);
					LOGGER.info("- Propertie {} set to default value false", TSystemPropertie.AI_ENABLED.getValue());
				});
				
				Optional<TPropertie> aiServProvOpt = properties.stream()
						.filter(p -> p.getKey().equals(TSystemPropertie.AI_SERVICE_PROVIDER.getValue()))
						.findFirst();
						
				if(aiServProvOpt.isPresent()) {
					TPropertie prop = aiServProvOpt.get();
					String val = StringUtils.isNotBlank(prop.getValue()) ? 
							prop.getValue() : AiServiceProvider.GROK.name();				
					
					AiServiceProvider provider = AiServiceProvider.valueOf(val);
					setAiServiceProvider(provider);
					LOGGER.info("- Using {} as AI Service Provider.", provider.name());
					
					TSystemPropertie aiModelPropertie = switch (provider) {
							case GROK -> TSystemPropertie.GROK_MODEL;
							case OPENAI -> TSystemPropertie.OPENAI_MODEL;
							case GEMINI -> TSystemPropertie.GEMINI_MODEL;
						};
					
					properties.stream()
					.filter(p -> p.getKey().equals(aiModelPropertie.getValue()))
					.findFirst()
					.ifPresentOrElse(p -> {
						if(StringUtils.isNotBlank(p.getValue())) {
							setAiModel(p.getValue());
							LOGGER.info(PROPERTIE_LOADED, aiModelPropertie.getValue());
						}else{
							LOGGER.info(PROPERTIE_NOT_DEFINED, aiModelPropertie.getValue());
						}
					}, () -> LOGGER.info(PROPERTIE_NOT_DEFINED, aiModelPropertie.getValue()));
					
					TSystemPropertie aiPromptPropertie = switch (provider) {
							case GROK -> TSystemPropertie.GROK_PROMPT;
							case OPENAI -> TSystemPropertie.OPENAI_PROMPT;
							case GEMINI -> TSystemPropertie.GEMINI_PROMPT;
						};
					
					properties.stream()
					.filter(p -> p.getKey().equals(aiPromptPropertie.getValue()))
					.findFirst()
					.ifPresentOrElse(p -> {
						if(StringUtils.isNotBlank(p.getValue())) {
							setAiSystemPrompt(p.getValue());
							LOGGER.info(PROPERTIE_LOADED, aiPromptPropertie.getValue());
						}else{
							LOGGER.info(PROPERTIE_NOT_DEFINED, aiPromptPropertie.getValue());
						}
					}, () -> LOGGER.info(PROPERTIE_NOT_DEFINED, aiPromptPropertie.getValue()));
					
					TSystemPropertie aiApiKeyPropertie = switch (provider) {
							case GROK -> TSystemPropertie.GROK_KEY;
							case OPENAI -> TSystemPropertie.OPENAI_KEY;
							case GEMINI -> TSystemPropertie.GEMINI_KEY;
						}; 
					
					properties.stream()
					.filter(p -> p.getKey().equals(aiApiKeyPropertie.getValue()))
					.findFirst()
					.ifPresentOrElse(p -> {
						if(StringUtils.isNotBlank(p.getValue())) {
							setAiApiKey(p.getValue());
							LOGGER.info(PROPERTIE_LOADED, aiApiKeyPropertie.getValue());
						}else{
							LOGGER.info(PROPERTIE_NOT_DEFINED, aiApiKeyPropertie.getValue());
						}
					}, ()-> LOGGER.info(PROPERTIE_NOT_DEFINED, aiApiKeyPropertie.getValue()));
					
				}
				
			}
			
			reloadProperty.setValue(Instant.now());
			
		} catch (Exception e) {
			LOGGER.error(e.toString(), e);
		}
	}
	
	public static AiProviderPreference getAiProviderPreference(AiServiceProvider provider) {
		
		TSystemPropertie keyProp; 
		TSystemPropertie modelProp;
		TSystemPropertie promptProp;
		
		switch (provider) {
		case GEMINI: 			
			keyProp = TSystemPropertie.GEMINI_KEY;
			modelProp = TSystemPropertie.GEMINI_MODEL;
			promptProp = TSystemPropertie.GEMINI_PROMPT;
			break;
		case OPENAI:
			keyProp = TSystemPropertie.OPENAI_KEY;
			modelProp = TSystemPropertie.OPENAI_MODEL;
			promptProp = TSystemPropertie.OPENAI_PROMPT;
			break;
		case GROK:
			keyProp = TSystemPropertie.GROK_KEY;
			modelProp = TSystemPropertie.GROK_MODEL;
			promptProp = TSystemPropertie.GROK_PROMPT;
			break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + provider);
		};
		
		String apikey = properties.stream()
				.filter(p->p.getKey().equals(keyProp.getValue()))
				.findFirst()
				.get().getValue();
		
		String model = properties.stream()
				.filter(p->p.getKey().equals(modelProp.getValue()))
				.findFirst()
				.get().getValue();
		
		String prompt = properties.stream()
				.filter(p->p.getKey().equals(promptProp.getValue()))
				.findFirst()
				.get().getValue();
		
		return new AiProviderPreference(apikey, model, prompt);
	}
	
	/**
	 * Returns a new instance of {@link IAiTerosService} with pre-configured preferences.
	 * 
	 * @param assistantPrompt
	 * @return IAiTerosService
	 */
	public static IAiTerosService newInstanceAiTerosService() {		
		if(getAiToolRelayEnabled()) {
			return newInstanceToolRelay();
		}else {
			return newInstanceWithLangChain4jAdapters(getAiSystemPrompt(), getAiServiceProvider());
		}		
	}
	
	/**
	 * Returns a new instance of {@link IAiTerosService} with pre-configured preferences.
	 * 
	 * @param assistantPrompt
	 * @return IAiTerosService
	 */
	public static IAiTerosService newInstanceAiTerosService(String assistantPrompt) {		
		if(getAiToolRelayEnabled()) {
			return newInstanceToolRelay();
		}else {
			return newInstanceWithLangChain4jAdapters(assistantPrompt, getAiServiceProvider());
		}		
	}
	
	/**
	 * Creates and returns the same instance of {@link IAiTerosService} with pre-configured preferences.
	 * 
	 * @param assistantPrompt
	 * @return IAiTerosService
	 */
	public static IAiTerosService createAiTerosService(String assistantPrompt) {		
		if(getAiToolRelayEnabled()) {
			return createToolRelay();
		}else {
			return createWithLangChain4jAdapters(assistantPrompt, getAiServiceProvider());
		}		
	}
	
	/**
	 * Creates and returns the same instance of {@link IAiTerosService}. 
	 * Delegates execution to the back-end.
	 * 
	 * @return IAiTerosService
	 */
	public static IAiTerosService createToolRelay() {
		return aiServiceFactory.createToolRelay();
	}

	/**
	 * Returns a new instance of {@link IAiTerosService}. 
	 * Delegates execution to the back-end.
	 * 
	 * @return IAiTerosService
	 */
	public static IAiTerosService newInstanceToolRelay() {
		return aiServiceFactory.newInstanceToolRelay();
	}

	/**
	 * Creates and returns the same instance of {@link IAiTerosService}. 
	 * Delegates execution to the front-end.
	 * 
	 * @return IAiTerosService
	 */
	public static IAiTerosService createWithLangChain4jAdapters(String assistantPrompt, AiServiceProvider provider) {
		AiProviderPreference pref = getAiProviderPreference(provider);
		return aiServiceFactory.createWithLangChain4jAdapters(pref.apiKey(), pref.model(), assistantPrompt, provider);
	}
	
	/**
	 * Returns a new instance of {@link IAiTerosService}. 
	 * Delegates execution to the front-end.
	 * 
	 * @return IAiTerosService
	 */
	public static IAiTerosService newInstanceWithLangChain4jAdapters(String assistantPrompt, AiServiceProvider provider) {
		AiProviderPreference pref = getAiProviderPreference(provider);
		return aiServiceFactory.newInstanceWithLangChain4jAdapters(pref.apiKey(), pref.model(), assistantPrompt, provider);
	}
	
	/**
	 * Set the ai backend tool relay service 
	 * */
	private static void setAiToolRelayEnabled(boolean status){
		aiToolRelayEnabledProperty.setValue(status);
	}
	
	/**
	 * Get the ai backend tool relay service status
	 * */
	public static Boolean getAiToolRelayEnabled(){
		return aiToolRelayEnabledProperty.get();
	}

	/**
	 * Get the ai backend tool relay service property
	 * */
	public static ReadOnlyBooleanProperty aiToolRelayEnabledProperty(){
		return aiToolRelayEnabledProperty;
	}
	
	/**
	 * Set the artificial intelligence status
	 * */
	private static void setArtificialIntelligenceEnabled(boolean status){
		aiEnabledProperty.setValue(status);;
	}
	
	/**
	 * Get the artificial intelligence status
	 * */
	public static Boolean getArtificialIntelligenceEnabled(){
		return aiEnabledProperty.get();
	}

	/**
	 * Get the artificial intelligence property
	 * */
	public static ReadOnlyBooleanProperty artificialIntelligenceEnabledProperty(){
		return aiEnabledProperty;
	}
	
	//
	
	/**
	 * @return the ai provider
	 */
	public static AiServiceProvider getAiServiceProvider() {
		return aiServiceProviderProperty.get();
	}
	/**
	 * @param ai provider to set
	 */
	private static void setAiServiceProvider(AiServiceProvider provider) {
		aiServiceProviderProperty.setValue(provider);
	}
	
	/**
	 * ai provider property to listen.
	 * */
	public static ReadOnlyObjectProperty<AiServiceProvider> aiServiceProviderProperty(){
		return aiServiceProviderProperty;
	}
	
	/**
	 * @return the ai api key
	 */
	public static String getAiApiKey() {
		return aiApiKeyProperty.get();
	}
	/**
	 * @param key the ai api key to set
	 */
	private static void setAiApiKey(String key) {
		aiApiKeyProperty.setValue(key);
	}
	
	/**
	 * ai apy key property to listen.
	 * */
	public static ReadOnlyStringProperty aiApiKeyProperty(){
		return aiApiKeyProperty;
	}
	
	/**
	 * @return the aiModel
	 */
	public static String getAiModel() {
		return aiModelProperty.get();
	}
	/**
	 * @param model the aiModel to set
	 */
	private static void setAiModel(String model) {
		aiModelProperty.setValue(model);
	}
	
	/**
	 * ai model property to listen.
	 * */
	public static ReadOnlyStringProperty aiModelProperty(){
		return aiModelProperty;
	}
	/**
	 * @return the aiSystemPrompt
	 */
	public static String getAiSystemPrompt() {
		return aiSystemPromptProperty.get();
	}
	/**
	 * @param model the aiSystemPrompt to set
	 */
	private static void setAiSystemPrompt(String prompt) {
		aiSystemPromptProperty.setValue(prompt);
	}
	
	public static ReadOnlyObjectProperty<Instant> reloadProperty(){
		return reloadProperty;
	}

}
