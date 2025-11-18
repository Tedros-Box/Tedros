package org.tedros.ai.openai;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.tedros.ai.openai.model.ToolCallResult;
import org.tedros.common.model.TFileContentInfo;
import org.tedros.core.TCoreKeys;
import org.tedros.core.TLanguage;
import org.tedros.core.context.TedrosContext;
import org.tedros.util.TDateUtil;
import org.tedros.util.TLoggerUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseInputContent;
import com.openai.models.responses.ResponseInputFile;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputMessage.Content;
import com.openai.models.responses.ResponseOutputRefusal;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseReasoningItem;
import com.openai.models.responses.ResponseReasoningItem.Summary;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Versão adaptada do TerosService usando o SDK oficial da openai
 */
public class OpenAITerosService {

    private static final int MAX_INPUT_TOKENS = 85_000;
	private static final Logger LOGGER = TLoggerUtil.getLogger(OpenAITerosService.class);
    private static final String NO_RESPONSE = TLanguage.getInstance().getString(TCoreKeys.AI_NO_RESPONSE);
    
    private static final Predicate<ResponseInputItem> IS_USER_MESSAGE = item ->
    	(item.isMessage() && item.asMessage().role() == ResponseInputItem.Message.Role.USER) ||
    	(item.isEasyInputMessage() && item.asEasyInputMessage().role() == EasyInputMessage.Role.USER);
    
    private static String GPT_MODEL;
    private static String PROMPT_ASSISTANT;
    private static OpenAITerosService instance;
    
    private final OpenAIServiceAdapter adapter;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ResponseInputItem> messages = new ArrayList<>();
    
    private OpenAIFunctionExecutor functionExecutor;
    
    private ObservableList<String> reasoningsMessageProperty = FXCollections.observableArrayList();
    
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

    public void createFunctionExecutor(org.tedros.ai.function.TFunction<?>... functions) {
    	this.adapter.functions(Arrays.asList(functions));
    	this.functionExecutor = new OpenAIFunctionExecutor(functions);
    }
    
    public static void setGptModel(String model) {
        GPT_MODEL = model;
        LOGGER.info("Chat model em uso: {}", model);
    }

    public static void setPromptAssistant(String prompt) {
        PROMPT_ASSISTANT = prompt;
        LOGGER.info("Assistant prompt em uso: {}", prompt);
    }
            
    public ObservableList<String> reasoningsMessageProperty() {
		return reasoningsMessageProperty;
	}

    public String call(String userPrompt, String sysPrompt) {
        if (sysPrompt != null)
            messages.add(adapter.buildSysMessage(sysPrompt));

        messages.add(adapter.buildUserMessage(userPrompt));

        List<ResponseOutputItem> response = adapter.sendChatRequest(GPT_MODEL, messages);

        String output = processAiResponseMessage(response);
        
        if(adapter.totalInputTokenProperty().longValue()>MAX_INPUT_TOKENS) {
    		summarizeMessages();
    	}
        
        return output;
    }

    private String processAiResponseMessage(List<ResponseOutputItem> responseItems) {
        StringBuilder finalContent = new StringBuilder();
        
        ResponseReasoningItem lastResponseReasoningItem = null;
        
        for (ResponseOutputItem item : responseItems) {
            if (!item.isValid()) {
                LOGGER.warn("Item inválido na resposta.");
                continue;
            }

            if (item.isMessage()) {
                // Process text message
                processTextMessageResponse(finalContent, item);
            }
            
            else if (item.isReasoning()) {
            	// Process reasoning message 
            	lastResponseReasoningItem = processReasoningResponse(item);
            }

            else if (item.isFunctionCall()) {
            	// Process function call message
            	processFunctionCallResponse(finalContent, lastResponseReasoningItem, item);
            }
        }

        String result = finalContent.toString().trim();
        return result.isEmpty() ? NO_RESPONSE : result;
    }
    
    private void summarizeMessages() {
        try {
            LOGGER.info("Token threshold exceeded. Summarizing messages...");

            // 1. Criar instrução de resumo
            ResponseInputItem sysSummaryInstruction = adapter.buildSysMessage(
                "Summarize the previous conversation as concisely as possible. " +
                "Preserve important context, decisions made, and unresolved tasks. " +
                "Do NOT include token usage stats or meta-information. " +
                "Your output MUST be only the summary text."
            );

            List<ResponseInputItem> tempMessages = new ArrayList<>();
            tempMessages.add(sysSummaryInstruction);
            tempMessages.addAll(messages);

            // 2. Fazer requisição ao modelo para gerar o resumo
            String summary = adapter.sendChatRequest(GPT_MODEL, tempMessages).stream()
            	    .filter(ResponseOutputItem::isMessage)   // só itens que são mensagens
            	    .map(ResponseOutputItem::message)        // Optional<ResponseOutputMessage>
            	    .flatMap(Optional::stream)               // transforma Optional em Stream (0 ou 1 elemento)
            	    .flatMap(msg -> msg.content().stream())  // todos os Content da mensagem
            	    .filter(Content::isOutputText)           // só conteúdos de texto
            	    .map(Content::outputText)                // Optional<ResponseOutputText>
            	    .flatMap(Optional::stream)               // novamente para lidar com o Optional
            	    .map(ResponseOutputText::text)           // pega o texto
            	    .collect(Collectors.joining("\n"));      // junta tudo com quebra de linha

            if (summary.isEmpty()) {
                LOGGER.warn("Summarization returned empty. Aborting summarize.");
                return;
            }

            LOGGER.info("Conversation summarized successfully.");

            // 3. Manter apenas:
            // - Mensagem SYSTEM original
            // - Resumo
            // - Última mensagem USER (para manter continuidade)
            ResponseInputItem originalSystemMessage = messages.get(0);

            ResponseInputItem lastUserMessage = Stream.iterate(messages.size() - 1, i -> i >= 0, i -> i - 1)
                .map(messages::get)
                .filter(IS_USER_MESSAGE)
                .findFirst()
                .orElse(null);

            List<ResponseInputItem> newMessages = new ArrayList<>();
            newMessages.add(originalSystemMessage);

            // inserir o resumo como SYSTEM
            newMessages.add(adapter.buildSysMessage("Summary of earlier conversation:\n" + summary));

            if (lastUserMessage != null) {
                newMessages.add(lastUserMessage);
            }

            // 4. Substituir histórico de mensagens
            messages.clear();
            messages.addAll(newMessages);

            LOGGER.info("Message history replaced with summarized context ({} messages).",
                    messages.size());

        } catch (Exception e) {
            LOGGER.error("Error during conversation summarization", e);
        }
    }
	
    private void processFunctionCallResponse(
            StringBuilder finalContent,
            ResponseReasoningItem lastResponseReasoningItem,
            ResponseOutputItem item) {

        ResponseFunctionToolCall functionCall = item.asFunctionCall();
        Optional<ToolCallResult> resultOpt = functionExecutor.callFunction(functionCall);
        if (resultOpt.isEmpty()) {
            LOGGER.warn("Função {} não encontrada!", functionCall.name());
            return;
        }

        ToolCallResult result = resultOpt.get();
        List<String> uploadedFileIds = new ArrayList<>(); // Para deletar depois

        try {
            // 1. Adiciona chamada da função e resultado (texto)
            ResponseInputItem functionCallInput = ResponseInputItem.ofFunctionCall(functionCall);
            ResponseInputItem functionCallOutput = ResponseInputItem.ofFunctionCallOutput(
                ResponseInputItem.FunctionCallOutput.builder()
                    .callId(functionCall.callId())
                    .output(mapper.writeValueAsString(result.getResult()))
                    .build()
            );

            // Payload temporário para enviar ao modelo
            List<ResponseInputItem> toolRequest = new ArrayList<>();

            if (lastResponseReasoningItem != null) {
                toolRequest.add(ResponseInputItem.ofReasoning(lastResponseReasoningItem));
            }

            toolRequest.add(functionCallInput);
            toolRequest.add(functionCallOutput);

            // 2. Processa arquivos retornados pela função (upload + file_id)
            if (result.getFilesContentInfo() != null && !result.getFilesContentInfo().isEmpty()) {
                StringBuilder fileInfoText = new StringBuilder();
                fileInfoText.append("The function call (id: ").append(functionCall.callId())
                            .append(") returned the following file(s) for analysis:\n");

                for (TFileContentInfo fileContentInfo : result.getFilesContentInfo()) {
                    uploadFile(uploadedFileIds, toolRequest, fileInfoText, fileContentInfo);
                }

                // Adiciona uma mensagem de resumo dos arquivos anexados
                toolRequest.add(ResponseInputItem.ofMessage(
                    ResponseInputItem.Message.builder()
                        .role(ResponseInputItem.Message.Role.SYSTEM)
                        .addInputTextContent(fileInfoText.toString().trim())
                        .build()
                ));
            }

            // 3. Envia tudo de volta ao modelo
            List<ResponseOutputItem> nextResponse = adapter.sendToolCallResult(GPT_MODEL, toolRequest);

            // Processa resposta recursivamente
            String recursiveContent = processAiResponseMessage(nextResponse);
            if (recursiveContent != null && !recursiveContent.equals(NO_RESPONSE)) {
                finalContent.append(recursiveContent);
            }

        } catch (Exception e) {
            LOGGER.error("Erro inesperado durante processamento de tool-call.", e);
            finalContent.append("\n[Erro interno ao processar função. Tente novamente.]");
        } finally {
            // SEMPRE deleta os arquivos temporários, mesmo em caso de erro
            uploadedFileIds.forEach(fileId -> {
                try {
                    adapter.getClient().files().delete(fileId);
                    LOGGER.info("Arquivo temporário deletado com sucesso: {}", fileId);
                } catch (Exception e) {
                    LOGGER.warn("Falha ao deletar arquivo temporário: {}", fileId, e);
                }
            });
        }
    }

	private void uploadFile(List<String> uploadedFileIds, List<ResponseInputItem> toolRequest,
			StringBuilder fileInfoText, TFileContentInfo fileContentInfo) {
		try {
		    // Upload do arquivo		    
			try(ByteArrayInputStream bais = new ByteArrayInputStream(fileContentInfo.bytes())){
				FileCreateParams uploadParams = FileCreateParams.builder()
				        .file(bais)
				        .purpose(FilePurpose.ASSISTANTS) // ou VISION se for imagem
				        .build();

				    FileObject uploadedFile = adapter.getClient().files().create(uploadParams);
				    String fileId = uploadedFile.id();
				    uploadedFileIds.add(fileId); // Marca para deleção

				    fileInfoText.append("- ").append(fileContentInfo.fileName())
				                .append(" (file_id: ").append(fileId).append(")\n");

				    // Adiciona referência ao arquivo como content (suportado no Responses API)
				    ResponseInputItem fileRefItem = ResponseInputItem.ofMessage(
				        ResponseInputItem.Message.builder()
				            .role(ResponseInputItem.Message.Role.SYSTEM)
				            .addInputTextContent("Attached file: " + fileContentInfo.fileName() + " (file_id: " + fileId + ")")
				            .addContent(ResponseInputContent.ofInputFile(
				            				ResponseInputFile.builder()
				            				.fileId(fileId)
				            				.build()))
				            .build()
				    );
				    toolRequest.add(fileRefItem);
			}

		} catch (Exception e) {
		    LOGGER.error("Erro ao fazer upload do arquivo retornado pela função: {}", fileContentInfo.fileName(), e);
		    fileInfoText.append("- [ERRO] Falha ao anexar: ").append(fileContentInfo.fileName()).append("\n");
		}
	}

	
	private ResponseReasoningItem processReasoningResponse(ResponseOutputItem item) {
		ResponseReasoningItem lastResponseReasoningItem;
		Platform.runLater(()-> {
			ResponseReasoningItem reasoning = item.asReasoning();
			
			List<String> summaryList = reasoning.summary().stream()
					.map(Summary::text)
					.toList();
			
			if(!summaryList.isEmpty()) {
				reasoningsMessageProperty.addAll(summaryList);
			}else {
				reasoningsMessageProperty.add(TLanguage.getInstance().getString(TCoreKeys.AI_THINKING));
			}	
		});
		
		lastResponseReasoningItem = item.asReasoning();
		LOGGER.info("Reasoning {} ", lastResponseReasoningItem);
		return lastResponseReasoningItem;
	}

	private void processTextMessageResponse(StringBuilder finalContent, ResponseOutputItem item) {
		Optional<ResponseOutputMessage> msgOpt = item.message();
		if (msgOpt.isPresent()) {
		    ResponseOutputMessage msg = msgOpt.get();
		    for (Content content : msg.content()) {
		        if (content.isOutputText() && content.outputText().isPresent()) {		            
		        	Optional<ResponseOutputText> opt = content.outputText();
		        	if(opt.isPresent()) {
		        		String text = opt.get().text();
			            finalContent.append(text).append("\n");
			            messages.add(adapter.buildAssistantMessage(text));
		        	}		            
		        } else if (content.isRefusal() && content.refusal().isPresent()) {
		        	Optional<ResponseOutputRefusal> opt = content.refusal();
		        	if(opt.isPresent()) {
		        		String refusal = opt.get().refusal();
			            LOGGER.warn("Recusa: {}", refusal);
			            finalContent.append("Recusa: ").append(refusal);
		        	}
		        }
		    }
		}
	}

	private void createSystemMessage() {    	
        String date = TDateUtil.formatFullgDate(new Date(), TLanguage.getLocale());
        String user = TedrosContext.getLoggedUser().getName();        
        String header = "Today is %s. You are Teros, a smart and helpful assistant for the "
        		+ "Tedros desktop system. Engage intelligently with user %s.".formatted(date, user);
        
        if (PROMPT_ASSISTANT != null)
            header += " " + PROMPT_ASSISTANT;

        messages.add(adapter.buildSysMessage(header));
    }
}