package org.tedros.ai.ejb.controller;

import java.util.List;
import java.util.Map;

import org.tedros.ai.ejb.client.IMeetingMinutesRagController;
import org.tedros.ai.meeting.MeetingMinutesRagService;
import org.tedros.core.security.model.TUser;
import org.tedros.it.tools.entity.MeetingMinutes;
import org.tedros.server.ejb.controller.ITSecurityController;
import org.tedros.server.ejb.controller.TSecureEjbController;
import org.tedros.server.entity.ITUser;
import org.tedros.server.result.TResult;
import org.tedros.server.result.TResult.TState;
import org.tedros.server.security.ITSecurity;
import org.tedros.server.security.TAccessToken;
import org.tedros.server.security.TSecurityInterceptor;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

@TSecurityInterceptor
@Stateless(name = "IMeetingMinutesRagController")
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MeetingMinutesRagController extends TSecureEjbController<MeetingMinutes> implements IMeetingMinutesRagController, ITSecurity  {

	@EJB
	MeetingMinutesRagService ragService;

	@EJB
	ITSecurityController securityController;

	@Override
	public TResult<Void> ingest(TAccessToken token, String meetingId, String text, Map<String, String> metadata) {
		try {
			Long ownerId = resolveUserId(token);
			if (ownerId != null && metadata != null) {
				metadata.putIfAbsent("owner_user_id", String.valueOf(ownerId));
			}
			ragService.ingestAsync(meetingId, text, metadata);
			return new TResult<>(TState.SUCCESS);
		} catch (Exception e) {
			return new TResult<>(TState.ERROR, e.getMessage());
		}
	}

	@Override
	public TResult<Void> purge(TAccessToken token, String meetingId) {
		try {
			ragService.purge(meetingId);
			return new TResult<>(TState.SUCCESS);
		} catch (Exception e) {
			return new TResult<>(TState.ERROR, e.getMessage());
		}
	}

	@Override
	public TResult<List<Map<String, Object>>> search(TAccessToken token, String query, int maxResults, double minScore) {
		try {
			Long ownerId = resolveUserId(token);
			List<Map<String, Object>> rows = ragService.search(query, ownerId, maxResults, minScore);
			return new TResult<>(TState.SUCCESS, rows);
		} catch (Exception e) {
			return new TResult<>(TState.ERROR, e.getMessage());
		}
	}
	
	@Override
	public MeetingMinutesRagService getService() {
		return ragService;
	}
	
	@Override
	public ITSecurityController getSecurityController() {
		return securityController;
	}

	private Long resolveUserId(TAccessToken token) {
		if (token == null || securityController == null) {
			return null;
		}
		ITUser user = securityController.getUser(token);
		if (user instanceof TUser tu) {
			return tu.getId();
		}
		return user != null ? user.getId() : null;
	}
}
