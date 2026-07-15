package org.tedros.ai.ejb.client;

import java.util.List;
import java.util.Map;

import org.tedros.server.result.TResult;
import org.tedros.server.security.TAccessToken;

import jakarta.ejb.Remote;

@Remote
public interface IMeetingMinutesRagController {

	static final String JNDI_NAME = "IMeetingMinutesRagControllerRemote";

	TResult<Void> ingest(TAccessToken token, String meetingId, String text, Map<String, String> metadata);

	TResult<Void> purge(TAccessToken token, String meetingId);

	TResult<List<Map<String, Object>>> search(TAccessToken token, String query, int maxResults, double minScore);
}
