package org.tedros.ai.toolrelay.function.meeting;

import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ACTION;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.ERROR_MESSAGE;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.STATUS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SUCCESS;
import static org.tedros.ai.toolrelay.function.TServerFunctionConstants.SYSTEM_INSTRUCTION;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.tedros.ai.meeting.MeetingMinutesRagService;
import org.tedros.ai.toolrelay.function.TAiToolContext;
import org.tedros.ai.toolrelay.function.TServerAiFunction;
import org.tedros.ai.toolrelay.function.meeting.model.MeetingMinutesSearchModel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SearchMeetingMinutesAiFunction implements TServerAiFunction {

	public static final String NAME = "search_meeting_minutes";
	public static final String DESCRIPTION = "Search historical meeting minutes analyzed by Teros using semantic similarity. "
			+ "Use when the user asks about topics discussed in past meetings. "
			+ "Only returns meetings the logged user is authorized to see.";

	@Inject
	MeetingMinutesRagService ragService;

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
		return MeetingMinutesSearchModel.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		MeetingMinutesSearchModel model = (MeetingMinutesSearchModel) arg;
		if (model == null || StringUtils.isBlank(model.getQuery())) {
			return Map.of(STATUS, ERROR, ACTION, "meeting_minutes_search_error", ERROR_MESSAGE,
					"Query is required");
		}
		try {
			Long ownerId = ctx != null && ctx.getUser() != null ? ctx.getUser().getId() : null;
			List<Map<String, Object>> hits = searchMeetings(model.getQuery(), ownerId);
			if (hits == null || hits.isEmpty()) {
				return Map.of(STATUS, SUCCESS, ACTION, "meeting_minutes_not_found", SYSTEM_INSTRUCTION,
						"Não encontrei informações sobre isso nas reuniões. Do not invent content.",
						"matches", List.of());
			}
			return Map.of(STATUS, SUCCESS, ACTION, "meeting_minutes_found", SYSTEM_INSTRUCTION,
					"Answer exclusively from the provided meeting context. "
							+ "If insufficient, say: Não encontrei informações sobre isso nas reuniões.",
					"matches", hits);
		} catch (Exception e) {
			return Map.of(STATUS, ERROR, ACTION, "meeting_minutes_search_error", ERROR_MESSAGE, e.getMessage());
		}
	}

	/** Hook para testes unitarios (stub sem EJB/pgvector). */
	protected List<Map<String, Object>> searchMeetings(String query, Long ownerId) {
		return ragService.search(query, ownerId, 5, 0.55);
	}
}
