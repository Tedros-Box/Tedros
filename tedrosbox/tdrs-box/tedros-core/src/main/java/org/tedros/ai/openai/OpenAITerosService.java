package org.tedros.ai.openai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.tedros.ai.TFunctionHelper;
import org.tedros.ai.openai.model.ToolCallResult;
import org.tedros.core.TLanguage;
import org.tedros.util.TDateUtil;
import org.tedros.util.TLoggerUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputMessage.Content;
import com.openai.models.responses.ResponseReasoningItem;

import javafx.beans.property.SimpleListProperty;

/**
 * Versão adaptada do TerosService usando o SDK oficial.
 */
public class OpenAITerosService {

    private static final Logger LOGGER = TLoggerUtil.getLogger(OpenAITerosService.class);
    
    private static String GPT_MODEL;
    private static String PROMPT_ASSISTANT;
    private static OpenAITerosService instance;
    
    private final OpenAIServiceAdapter adapter;    
    private final ObjectMapper mapper = new ObjectMapper();    
    private final List<ResponseInputItem> messages = new ArrayList<>();
    
    private OpenAIFunctionExecutor functionExecutor;
    private SimpleListProperty<String> reasoningsMessageProperty = new SimpleListProperty<>();
    
    private OpenAITerosService(String token) {
        this.adapter = new OpenAIServiceAdapter(token);
        createSystemMessage();
        LOGGER.info("OpenAI Teros Service iniciado!");
    }

    public static OpenAITerosService create(String token) {
        if (instance == null)
            instance = new OpenAITerosService(token);
        return instance;
    }

    public static OpenAITerosService getInstance() {
        if (instance == null)
            throw new IllegalStateException("Instância não criada!");
        return instance;
    }

    private void createSystemMessage() {
        String date = TDateUtil.formatFullgDate(new Date(), TLanguage.getLocale());
        //String user = TedrosContext.getLoggedUser().getName();
        String user = "Davis";
        String header = "Today is %s. You are Teros, a smart and helpful assistant for the Tedros desktop system. Engage intelligently with user %s."
                .formatted(date, user);
        
        if (PROMPT_ASSISTANT != null)
            header += " " + PROMPT_ASSISTANT;

        messages.add(adapter.buildSysMessage(header));
    }

    public void createFunctionExecutor(org.tedros.ai.function.TFunction<?>... functions) {
    	this.adapter.functions(Arrays.asList(functions));
    	this.functionExecutor = new OpenAIFunctionExecutor(functions);
    }

    public String call(String userPrompt, String sysPrompt) {
        if (sysPrompt != null)
            messages.add(adapter.buildSysMessage(sysPrompt));

        messages.add(adapter.buildUserMessage(userPrompt));

        List<ResponseOutputItem> response = adapter.sendChatRequest(GPT_MODEL, messages);

        return processChatCompletion(response);
    }

    private String processChatCompletion(List<ResponseOutputItem> responseItems) {
        StringBuilder finalContent = new StringBuilder();
        
        ResponseReasoningItem lastResponseReasoningItem = null;
        
        for (ResponseOutputItem item : responseItems) {
            if (!item.isValid()) {
                LOGGER.warn("Item inválido na resposta.");
                continue;
            }

            if (item.isMessage()) {
                // Processa mensagem normal
                Optional<ResponseOutputMessage> msgOpt = item.message();
                if (msgOpt.isPresent()) {
                    ResponseOutputMessage msg = msgOpt.get();
                    for (Content content : msg.content()) {
                        if (content.isOutputText() && content.outputText().isPresent()) {
                            String text = content.outputText().get().text();
                            finalContent.append(text).append("\n");
                            messages.add(adapter.buildAssistantMessage(text));
                        } else if (content.isRefusal() && content.refusal().isPresent()) {
                            String refusal = content.refusal().get().refusal();
                            LOGGER.warn("Recusa: {}", refusal);
                            finalContent.append("Recusa: ").append(refusal);
                        }
                    }
                }
            }
            
            else if (item.isReasoning()) {
            	lastResponseReasoningItem = item.asReasoning();
            	lastResponseReasoningItem.summary().stream().forEach(s -> {
					reasoningsMessageProperty.add(s.text());
				});
            	
                LOGGER.info("Reasoning {} ", lastResponseReasoningItem);
                //messages.add(adapter.buildReasoningMessage(responseReasoningItem));
            }

            else if (item.isFunctionCall()) {
            	
            	// Se havia um reasoning imediatamente antes, inclua junto
                if (lastResponseReasoningItem != null) {
                    messages.add(ResponseInputItem.ofReasoning(lastResponseReasoningItem));
                    lastResponseReasoningItem = null;
                }
            	
                ResponseFunctionToolCall functionCall = item.asFunctionCall();                

                Optional<ToolCallResult> resultOpt = functionExecutor.callFunction(functionCall);
                if (resultOpt.isPresent()) {
                    ToolCallResult result = resultOpt.get();

                    // Adiciona a chamada de função
                    messages.add(ResponseInputItem.ofFunctionCall(functionCall));
                    
                    try {
						// Adiciona o resultado
						messages.add(ResponseInputItem.ofFunctionCallOutput(
						    ResponseInputItem.FunctionCallOutput.builder()
						        .callId(functionCall.callId())
						        .output(mapper.writeValueAsString(result))
						        .build()
						));
					} catch (JsonProcessingException e) {
		                LOGGER.error("Erro inesperado.", e);
					}

                    // Chama novamente com os resultados
                    List<ResponseOutputItem> nextResponse = adapter.sendToolCallResult(
                        GPT_MODEL, messages, functionCall, result
                    );
                    String recursiveContent = processChatCompletion(nextResponse);
                    if (recursiveContent != null && !recursiveContent.equals("[no response]")) {
                        finalContent.append(recursiveContent);
                    }
                } else {
                    LOGGER.warn("Função {} não encontrada!", functionCall.name());
                }
            }
        }

        String result = finalContent.toString().trim();
        return result.isEmpty() ? "[no response]" : result;
    }

    public static void setGptModel(String model) {
        GPT_MODEL = model;
        LOGGER.info("Chat model em uso: {}", model);
    }

    public static void setPromptAssistant(String prompt) {
        PROMPT_ASSISTANT = prompt;
        LOGGER.info("Assistant prompt em uso: {}", prompt);
    }
            
    public SimpleListProperty<String> reasoningsMessageProperty() {
		return reasoningsMessageProperty;
	}
}