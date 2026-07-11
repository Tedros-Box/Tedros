package org.tedros.ai.toolrelay.function.extension;

import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ACTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR_MESSAGE;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.STATUS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SUCCESS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SYSTEM_INSTRUCTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionResults.safeMessage;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.extension.model.DocumentInfo;
import org.tedros.ai.toolrelay.function.extension.model.DocumentSearchParam;
import org.tedros.ai.toolrelay.function.extension.model.DocumentTypeInfo;
import org.tedros.ai.toolrelay.function.extension.model.DomainInfo;
import org.tedros.ai.toolrelay.function.extension.model.FileInfo;
import org.tedros.extension.ejb.controller.IDocumentController;
import org.tedros.extension.model.Document;
import org.tedros.server.query.TCompareOp;
import org.tedros.server.query.TJoinType;
import org.tedros.server.query.TLogicOp;
import org.tedros.server.query.TSelect;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.service.TServiceLocator;
import org.tedros.server.util.TLoggerUtil;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Versao de backend da {@code SearchForDocumentsFunction} do FE
 * (app-extensions-fx) — name, description, montagem do TSelect, conversao
 * para DocumentInfo (sem bytes do arquivo) e strings de resultado copiados
 * literalmente; controller seguro via lookup JNDI server-side com o token do
 * request corrente.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class SearchForDocumentsFunction implements TServerAiFunction {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(SearchForDocumentsFunction.class);

	public static final String NAME = "search_for_documents";
	public static final String DESCRIPTION = "Search for documents based on given parameters";

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public Class<?> getModel() {
		return DocumentSearchParam.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		DocumentSearchParam v = (DocumentSearchParam) arg;

		LOGGER.info("Searching for documents with parameters: {}", v);

		try {
			TSelect<Document> select = new TSelect<>(Document.class, "doc");
			select.addJoin(TJoinType.INNER, "doc", "type", "tp");
			select.addJoin(TJoinType.INNER, "doc", "status", "st");
			select.addJoin(TJoinType.INNER, "doc", "file", "f");

			if (v.getId() != null) {
				select.addCondition(TLogicOp.AND, "doc", "id", TCompareOp.EQUAL, v.getId());
			}

			if (StringUtils.isNotBlank(v.getCode())) {
				select.addCondition(TLogicOp.AND, "doc", "code", TCompareOp.EQUAL, v.getCode());
			}

			if (StringUtils.isNotBlank(v.getName())) {
				select.addCondition(TLogicOp.AND, "doc", "name", TCompareOp.LIKE, v.getName());
			}

			TResult<List<Document>> result = search(ctx.getToken(), select);

			if (result.getState().equals(TState.SUCCESS)) {
				List<Document> lst = result.getValue();
				if (lst != null && !lst.isEmpty()) {
					List<DocumentInfo> docs = lst.stream().map(SearchForDocumentsFunction::convert).toList();
					return Map.of(
							STATUS, SUCCESS,
							ACTION, "documents_found",
							SYSTEM_INSTRUCTION, "Documents found successfully. "
									+ "Do not retry again. Proceed with the user's request.",
							"documents", docs);
				}
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return Map.of(
					STATUS, ERROR,
					ACTION, "document_search_error",
					ERROR_MESSAGE, safeMessage(e));
		}

		return Map.of(
				STATUS, ERROR,
				ACTION, "no_documents_found",
				ERROR_MESSAGE, "No documents found matching the criteria.");
	}

	/** Protegido para override nos testes (sem container/JNDI). */
	protected TResult<List<Document>> search(TAccessToken token, TSelect<Document> select) throws Exception {
		TServiceLocator serv = TServiceLocator.getInstance();
		try {
			IDocumentController controller = serv.lookupWithRetry(IDocumentController.JNDI_NAME);
			return controller.search(token, select);
		} finally {
			serv.close();
		}
	}

	private static DocumentInfo convert(Document doc) {
		DocumentTypeInfo typeInfo = null;
		if (doc.getType() != null) {
			typeInfo = DocumentTypeInfo.builder()
					.documentType(doc.getType().getDocType())
					.build();
			typeInfo.setName(doc.getType().getName());
			typeInfo.setCode(doc.getType().getCode());
			typeInfo.setDescription(doc.getType().getDescription());
		}

		DomainInfo statusInfo = doc.getStatus() != null
				? new DomainInfo(doc.getStatus().getCode(),
						doc.getStatus().getName(),
						doc.getStatus().getDescription())
				: null;

		FileInfo fileInfo = null;
		if (doc.getFile() != null) {
			fileInfo = FileInfo.builder()
					.id(doc.getFile().getId())
					.fileName(doc.getFile().getFileName())
					.fileSize(doc.getFile().getFileSize())
					.fileExtension(doc.getFile().getFileExtension())
					.owner(doc.getFile().getOwner())
					.build();
		}

		return DocumentInfo.builder().id(doc.getId())
				.code(doc.getCode())
				.name(doc.getName())
				.type(typeInfo)
				.status(statusInfo)
				.observation(doc.getObservation())
				.file(fileInfo)
				.build();
	}

}
