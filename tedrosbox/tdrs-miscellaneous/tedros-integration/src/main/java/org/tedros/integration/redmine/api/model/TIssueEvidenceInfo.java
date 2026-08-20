package org.tedros.integration.redmine.api.model;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import lombok.ToString;

import java.util.List;

@ToString
@JsonClassDescription("Redmine issue summary for evidence/reporting")
public class TIssueEvidenceInfo {

    @JsonPropertyDescription("Issue ID")
    private Long id;

    @JsonPropertyDescription("Title")
    private String subject;

    @JsonPropertyDescription("Start date (ISO)")
    private String startDate;

    @JsonPropertyDescription("Due date (ISO)")
    private String dueDate;

    @JsonPropertyDescription("Creation date (ISO)")
    private String createdOn;

    @JsonPropertyDescription("Last update (ISO)")
    private String updatedOn;

    @JsonPropertyDescription("Done ratio % (0-100)")
    private Integer doneRatio;

    @JsonPropertyDescription("Closed date (ISO or null)")
    private String closedOn;

    @JsonPropertyDescription("Estimated hours")
    private Float estimatedHours;

    @JsonPropertyDescription("Spent hours")
    private Float spentHours;

    @JsonPropertyDescription("Assignee user ID")
    private Integer assigneeId;

    @JsonPropertyDescription("Assignee full name")
    private String assigneeName;

    @JsonPropertyDescription("Priority name")
    private String priorityText;

    @JsonPropertyDescription("Project name")
    private String projectName;

    @JsonPropertyDescription("Author user ID")
    private Integer authorId;

    @JsonPropertyDescription("Author full name")
    private String authorName;

    @JsonPropertyDescription("Tracker/Type name")
    private String trackerName;

    @JsonPropertyDescription("Full description")
    private String description;

    @JsonPropertyDescription("Current status name")
    private String statusName;

    @JsonPropertyDescription("CF: Expected deliverable (Memora - cf_100)")
    private String deliverable;

    @JsonPropertyDescription("CF: HPA - Hours per Activity")
    private String hpa;

    @JsonPropertyDescription("CF: Required professional profile")
    private String requiredProfile;

    @JsonPropertyDescription("CF: Service type with expected hours (Memora - cf_96)")
    private String serviceType;

    @JsonPropertyDescription("CF: OS - Service Order (Memora - cf_109)")
    private String os;

    @JsonPropertyDescription("CF: Chamado GLPI / 4Biz / Nº SEI (cf_58)")
    private String glpiOrSei;

    @JsonPropertyDescription("CF: Quantidade (cf_12)")
    private String quantity;

    @JsonPropertyDescription("CF: Nº SEI! (cf_4)")
    private String seiNumber;

    @JsonPropertyDescription("CF: Área (cf_1)")
    private String area;

    @JsonPropertyDescription("CF: Fase (cf_83)")
    private String phase;

    @JsonPropertyDescription("CF: Story Points (cf_113)")
    private String storyPoints;

    @JsonPropertyDescription("CF: Classificação da demanda (cf_114)")
    private String demandClassification;

    @JsonPropertyDescription("All custom fields")
    private List<TCustomField> customFields;

    @JsonPropertyDescription("Chronological notes/comments list")
    private List<String> notes;

    @JsonPropertyDescription("File attachments")
    private List<TAttachment> attachments;

    // === GETTERS E SETTERS ===

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(String updatedOn) {
        this.updatedOn = updatedOn;
    }

    public Integer getDoneRatio() {
        return doneRatio;
    }

    public void setDoneRatio(Integer doneRatio) {
        this.doneRatio = doneRatio;
    }

    public String getClosedOn() {
        return closedOn;
    }

    public void setClosedOn(String closedOn) {
        this.closedOn = closedOn;
    }

    public Float getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(Float estimatedHours) {
        this.estimatedHours = estimatedHours;
    }

    public Float getSpentHours() {
        return spentHours;
    }

    public void setSpentHours(Float spentHours) {
        this.spentHours = spentHours;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public void setAssigneeName(String assigneeName) {
        this.assigneeName = assigneeName;
    }

    public String getPriorityText() {
        return priorityText;
    }

    public void setPriorityText(String priorityText) {
        this.priorityText = priorityText;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getTrackerName() {
        return trackerName;
    }

    public void setTrackerName(String trackerName) {
        this.trackerName = trackerName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getDeliverable() {
        return deliverable;
    }

    public void setDeliverable(String deliverable) {
        this.deliverable = deliverable;
    }

    public String getHpa() {
        return hpa;
    }

    public void setHpa(String hpa) {
        this.hpa = hpa;
    }

    public String getRequiredProfile() {
        return requiredProfile;
    }

    public void setRequiredProfile(String requiredProfile) {
        this.requiredProfile = requiredProfile;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getGlpiOrSei() {
        return glpiOrSei;
    }

    public void setGlpiOrSei(String glpiOrSei) {
        this.glpiOrSei = glpiOrSei;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getSeiNumber() {
        return seiNumber;
    }

    public void setSeiNumber(String seiNumber) {
        this.seiNumber = seiNumber;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(String storyPoints) {
        this.storyPoints = storyPoints;
    }

    public String getDemandClassification() {
        return demandClassification;
    }

    public void setDemandClassification(String demandClassification) {
        this.demandClassification = demandClassification;
    }

    public List<TCustomField> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(List<TCustomField> customFields) {
        this.customFields = customFields;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public List<TAttachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<TAttachment> attachments) {
        this.attachments = attachments;
    }
}