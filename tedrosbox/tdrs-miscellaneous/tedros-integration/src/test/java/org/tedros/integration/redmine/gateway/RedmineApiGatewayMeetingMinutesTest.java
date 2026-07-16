package org.tedros.integration.redmine.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.tedros.integration.redmine.api.model.TAttachment;
import org.tedros.integration.redmine.api.model.TTimeEntry;

import com.taskadapter.redmineapi.bean.Attachment;
import com.taskadapter.redmineapi.bean.AttachmentFactory;

/**
 * Unit tests for idempotent meeting-minutes Redmine update (no network).
 */
public class RedmineApiGatewayMeetingMinutesTest {

	static class FakeGateway extends RedmineApiGateway {
		List<Attachment> attachments = new ArrayList<>();
		List<TTimeEntry> timeEntries = new ArrayList<>();
		int uploadCalls;
		int createTimeCalls;
		Integer currentUserId = 77;

		FakeGateway() {
			super("http://redmine.local", "key");
		}

		@Override
		protected Collection<Attachment> findIssueAttachments(Integer issueId) {
			return attachments;
		}

		@Override
		protected Integer resolveCurrentRedmineUserId() {
			return currentUserId;
		}

		@Override
		public List<TTimeEntry> getTimeEntriesForIssue(Integer issueId) {
			return timeEntries;
		}

		@Override
		public TAttachment uploadIssueAttachment(Integer issueId, byte[] content, String fileName, String contentType) {
			uploadCalls++;
			TAttachment a = new TAttachment();
			a.setId(501);
			a.setFileName(fileName);
			a.setContentType(contentType);
			return a;
		}

		@Override
		public TTimeEntry createTimeEntry(Integer issueId, Integer userId, Integer activityId, Float hours,
				Date spentOn, String comment) {
			createTimeCalls++;
			TTimeEntry te = new TTimeEntry();
			te.setId(901);
			te.setIssueId(issueId);
			te.setUserId(userId);
			te.setActivityId(activityId);
			te.setHours(hours);
			te.setSpentOn(spentOn);
			te.setComment(comment);
			return te;
		}
	}

	private static Date day(int y, int m, int d) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(y, m - 1, d);
		return c.getTime();
	}

	@Test
	public void parseIntOrNullHandlesBlankAndInvalid() {
		assertNull(RedmineApiGateway.parseIntOrNull(null));
		assertNull(RedmineApiGateway.parseIntOrNull(""));
		assertNull(RedmineApiGateway.parseIntOrNull("  "));
		assertNull(RedmineApiGateway.parseIntOrNull("abc"));
		assertEquals(Integer.valueOf(19), RedmineApiGateway.parseIntOrNull(" 19 "));
	}

	@Test
	public void isEmptyHttpEntityErrorDetectsNestedIllegalArgument() {
		assertFalse(RedmineApiGateway.isEmptyHttpEntityError(null));
		assertFalse(RedmineApiGateway.isEmptyHttpEntityError(new RuntimeException("other")));
		IllegalArgumentException cause = new IllegalArgumentException("Entity may not be null");
		assertTrue(RedmineApiGateway.isEmptyHttpEntityError(cause));
		assertTrue(RedmineApiGateway.isEmptyHttpEntityError(new RuntimeException("wrap", cause)));
	}

	@Test
	public void skipsUploadAndTimeWhenExistingIdsPresent() {
		FakeGateway g = new FakeGateway();
		MeetingMinutesRedmineUpdateResult r = g.updateIssueWithMeetingMinutes(10, 77, 19, new byte[] { 1 },
				"ata.pdf", "application/pdf", 0.5f, day(2026, 7, 1), "Participação em Reunião", "333", "444");
		assertTrue(r.isAttachmentSkipped());
		assertTrue(r.isTimeEntrySkipped());
		assertEquals(Integer.valueOf(333), r.getAttachmentId());
		assertEquals(Integer.valueOf(444), r.getTimeEntryId());
		assertEquals(0, g.uploadCalls);
		assertEquals(0, g.createTimeCalls);
	}

	@Test
	public void skipsUploadWhenAttachmentFilenameAlreadyOnIssue() {
		FakeGateway g = new FakeGateway();
		Attachment existing = AttachmentFactory.create(88);
		existing.setFileName("ata.pdf");
		g.attachments.add(existing);

		MeetingMinutesRedmineUpdateResult r = g.updateIssueWithMeetingMinutes(10, 77, 19, new byte[] { 1 },
				"ata.pdf", "application/pdf", 0.5f, day(2026, 7, 1), "Participação em Reunião", null, null);

		assertTrue(r.isAttachmentSkipped());
		assertEquals(Integer.valueOf(88), r.getAttachmentId());
		assertEquals(0, g.uploadCalls);
		assertEquals(1, g.createTimeCalls);
		assertFalse(r.isTimeEntrySkipped());
	}

	@Test
	public void skipsTimeEntryWhenMatchingEntryExists() {
		FakeGateway g = new FakeGateway();
		TTimeEntry te = new TTimeEntry();
		te.setId(55);
		te.setComment("Participação em Reunião");
		te.setActivityId(19);
		te.setHours(0.5f);
		te.setSpentOn(day(2026, 7, 1));
		te.setUserId(77);
		g.timeEntries.add(te);

		MeetingMinutesRedmineUpdateResult r = g.updateIssueWithMeetingMinutes(10, 77, 19, new byte[] { 1 },
				"ata.pdf", "application/pdf", 0.5f, day(2026, 7, 1), "Participação em Reunião", null, null);

		assertFalse(r.isAttachmentSkipped());
		assertEquals(1, g.uploadCalls);
		assertTrue(r.isTimeEntrySkipped());
		assertEquals(Integer.valueOf(55), r.getTimeEntryId());
		assertEquals(0, g.createTimeCalls);
	}

	@Test
	public void createsBothWhenNothingExistsAndUsesCurrentUserWhenUserIdNull() {
		FakeGateway g = new FakeGateway();
		MeetingMinutesRedmineUpdateResult r = g.updateIssueWithMeetingMinutes(10, null, 19, new byte[] { 1 },
				"ata.pdf", "application/pdf", 1.0f, day(2026, 7, 2), "Participação em Reunião", null, null);

		assertFalse(r.isAttachmentSkipped());
		assertFalse(r.isTimeEntrySkipped());
		assertEquals(Integer.valueOf(501), r.getAttachmentId());
		assertEquals(Integer.valueOf(901), r.getTimeEntryId());
		assertEquals(1, g.uploadCalls);
		assertEquals(1, g.createTimeCalls);
	}

	@Test
	public void doesNotSkipTimeEntryWhenHoursDiffer() {
		FakeGateway g = new FakeGateway();
		TTimeEntry te = new TTimeEntry();
		te.setId(55);
		te.setComment("Participação em Reunião");
		te.setActivityId(19);
		te.setHours(1.0f);
		te.setSpentOn(day(2026, 7, 1));
		te.setUserId(77);
		g.timeEntries.add(te);

		MeetingMinutesRedmineUpdateResult r = g.updateIssueWithMeetingMinutes(10, 77, 19, new byte[] { 1 },
				"ata.pdf", "application/pdf", 0.5f, day(2026, 7, 1), "Participação em Reunião", "10", null);

		assertTrue(r.isAttachmentSkipped());
		assertFalse(r.isTimeEntrySkipped());
		assertEquals(1, g.createTimeCalls);
	}
}
