package org.tedros.integration.redmine.gateway;

public class MeetingMinutesRedmineUpdateResult {

	private Integer attachmentId;
	private Integer timeEntryId;
	private boolean attachmentSkipped;
	private boolean timeEntrySkipped;

	public Integer getAttachmentId() {
		return attachmentId;
	}

	public void setAttachmentId(Integer attachmentId) {
		this.attachmentId = attachmentId;
	}

	public Integer getTimeEntryId() {
		return timeEntryId;
	}

	public void setTimeEntryId(Integer timeEntryId) {
		this.timeEntryId = timeEntryId;
	}

	public boolean isAttachmentSkipped() {
		return attachmentSkipped;
	}

	public void setAttachmentSkipped(boolean attachmentSkipped) {
		this.attachmentSkipped = attachmentSkipped;
	}

	public boolean isTimeEntrySkipped() {
		return timeEntrySkipped;
	}

	public void setTimeEntrySkipped(boolean timeEntrySkipped) {
		this.timeEntrySkipped = timeEntrySkipped;
	}
}
