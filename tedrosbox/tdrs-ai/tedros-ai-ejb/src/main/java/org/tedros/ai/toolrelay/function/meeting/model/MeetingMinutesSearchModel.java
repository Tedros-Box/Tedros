package org.tedros.ai.toolrelay.function.meeting.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

@JsonClassDescription("Search past meeting minutes analyzed by Teros")
public class MeetingMinutesSearchModel {

	@JsonPropertyDescription("Natural language question about past meetings")
	private String query;

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
}
