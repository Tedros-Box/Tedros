package org.tedros.integration.redmine.ai.model;

import java.util.List;

import org.tedros.integration.redmine.api.model.TAttachment;
import org.tedros.integration.redmine.api.model.TIssueEvidenceInfo;

public record IssueRelevantInfo(TIssueEvidenceInfo info, List<TAttachment> attachments) {

}
