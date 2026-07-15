package org.tedros.integration.redmine.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MeetingMinutesRedmineUpdateResultTest {

	@Test
	public void holdsIdsAndSkipFlags() {
		MeetingMinutesRedmineUpdateResult r = new MeetingMinutesRedmineUpdateResult();
		r.setAttachmentId(1);
		r.setTimeEntryId(2);
		r.setAttachmentSkipped(true);
		r.setTimeEntrySkipped(false);

		assertEquals(Integer.valueOf(1), r.getAttachmentId());
		assertEquals(Integer.valueOf(2), r.getTimeEntryId());
		assertTrue(r.isAttachmentSkipped());
		assertFalse(r.isTimeEntrySkipped());
	}
}
