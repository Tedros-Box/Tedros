package org.tedros.integration.redmine.gateway;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.tedros.common.model.TFileContentInfo;
import org.tedros.integration.redmine.ai.model.CustomFieldMetadata;
import org.tedros.integration.redmine.ai.model.FilterCondition;
import org.tedros.integration.redmine.ai.model.FilterType;
import org.tedros.integration.redmine.api.model.TAttachment;
import org.tedros.integration.redmine.api.model.TIssue;
import org.tedros.integration.redmine.api.model.TIssueEvidenceInfo;
import org.tedros.integration.redmine.api.model.TIssueStatus;
import org.tedros.integration.redmine.api.model.TMembership;
import org.tedros.integration.redmine.api.model.TProject;
import org.tedros.integration.redmine.api.model.TRedmineUser;
import org.tedros.integration.redmine.api.model.TTimeEntry;

import com.taskadapter.redmineapi.Include;
import com.taskadapter.redmineapi.Params;
import com.taskadapter.redmineapi.RedmineException;
import com.taskadapter.redmineapi.RedmineManager;
import com.taskadapter.redmineapi.RedmineManagerFactory;
import com.taskadapter.redmineapi.bean.Attachment;
import com.taskadapter.redmineapi.bean.CustomFieldDefinition;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueFactory;
import com.taskadapter.redmineapi.bean.IssueStatus;
import com.taskadapter.redmineapi.bean.Membership;
import com.taskadapter.redmineapi.bean.Project;
import com.taskadapter.redmineapi.bean.TimeEntry;
import com.taskadapter.redmineapi.bean.TimeEntryFactory;
import com.taskadapter.redmineapi.bean.User;
import com.taskadapter.redmineapi.internal.ResultsWrapper;

public class RedmineApiGateway {

	private String uri;
	private RedmineManager manager;
	private Map<String, CustomFieldMetadata> customFieldCache = new HashMap<>();

	public RedmineApiGateway(String uri, String apiAccessKey) {
		this.uri = uri;
		this.manager = RedmineManagerFactory.createWithApiKey(uri, apiAccessKey);
		this.manager.setObjectsPerPage(100);
	}

	public void loadCustomFieldMetadata() {
		try {
			List<CustomFieldDefinition> fields = manager.getCustomFieldManager().getCustomFieldDefinitions();
			for (CustomFieldDefinition field : fields) {
				FilterType type;
				switch (field.getFieldFormat()) {
					case "int", "float":
						type = FilterType.NUMBER;
						break;
					case "date":
						type = FilterType.DATE;
						break;
					case "bool":
						type = FilterType.BOOLEAN;
						break;
					default:
						type = FilterType.TEXT;
						break;
				}
				customFieldCache.put("cf_" + field.getId(),
						new CustomFieldMetadata(field.getId(), field.getName(), type));
			}
		} catch (Exception e) {
			throw new RuntimeException("Erro ao carregar metadados de campos personalizados: " + e.getMessage(), e);
		}
	}

	public List<TTimeEntry> getTimeEntriesForIssue(Integer issueId) {
		try {
			List<TimeEntry> entries = this.manager.getTimeEntryManager().getTimeEntriesForIssue(issueId);
			if (entries != null)
				return RedmineMapper.convertTimeEntryList(entries);
			return List.of();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public List<TIssueStatus> listIssueStatuses() {

		try {
			List<IssueStatus> statuses = this.manager.getIssueManager().getStatuses();

			if (statuses != null)
				return RedmineMapper.convertIssueStatusList(statuses);

			return List.of();

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void addIssueJournal(Integer userId, Integer issueId) {

		try {
			User user = manager.getUserManager().getUserById(userId);
			String userApiKey = user.getApiKey();
			Issue issue = manager.getIssueManager().getIssueById(issueId, Include.journals);
			// Journal journal = JournalFactory.create(null, "Automated journal entry",
			// user, new Date());
			issue.setNotes("Test");

			RedmineManagerFactory.createWithApiKey(uri, userApiKey).getIssueManager().update(issue);

		} catch (RedmineException e) {
			throw new RuntimeException("Erro ao atualuzar issue: " + e.getMessage(), e);
		}
	}

	public List<TMembership> getProjectMembers(String projectKey) {
		List<Membership> members;
		try {
			members = manager.getProjectManager().getProjectMembers(projectKey);
			return RedmineMapper.convertMembershipList(members);
		} catch (RedmineException e) {
			throw new RuntimeException("Erro ao carregar os membros do projeto: " + e.getMessage(), e);
		}
	}

	public List<TIssueEvidenceInfo> getIssuesByFilters(Map<String, FilterCondition> filters) {
		List<TIssueEvidenceInfo> todasIssues = new ArrayList<>();
		int limit = 100; // Máximo permitido pelo Redmine geralmente é 100
		int offset = 0;
		int totalEncontrado = 0;

		try {
			// 1. Configuração inicial dos Parâmetros (Filtros fixos e dinâmicos)
			Params params = new Params()
					.add("set_filter", "1")
					.add("sort", "id:desc")
					.add("limit", String.valueOf(limit)); // Define o tamanho da página

			// Aplica os filtros do mapa
			for (Map.Entry<String, FilterCondition> entry : filters.entrySet()) {
				String field = entry.getKey();
				FilterCondition condition = entry.getValue();

				if (field.startsWith("cf_") && customFieldCache.containsKey(field)) {
					FilterType detectedType = customFieldCache.get(field).getType();
					condition = FilterCondition.auto(detectedType, condition.getOperator(), condition.getValues());
				}

				params.add("f[]", field);
				params.add("op[" + field + "]", condition.getOperator());

				if (condition.getValues() != null) {
					for (String value : condition.getValues()) {
						params.add("v[" + field + "][]", value);
					}
				}
			}

			// 2. Loop de Paginação
			do {
				// Atualiza o offset para a próxima página
				// Nota: Certifique-se que sua classe Params atualiza o valor se a chave já
				// existir,
				// ou use um método .replace() se disponível.
				params.add("offset", String.valueOf(offset));

				// Chamada à API
				ResultsWrapper<Issue> wrapper = manager.getIssueManager().getIssues(params);

				// Se não houver resultados ou wrapper for nulo, encerra
				if (wrapper == null || !wrapper.hasSomeResults()) {
					break;
				}

				// Pega o total disponível no servidor (metadado da resposta)
				totalEncontrado = wrapper.getTotalFoundOnServer();

				// Converte e adiciona à lista acumulada
				List<Issue> paginaAtual = wrapper.getResults();
				if (!paginaAtual.isEmpty()) {
					for (Issue issueResumida : paginaAtual) {
		                // Faz uma nova chamada para cada issue para pegar TUDO (Journals, etc)
		                // CUIDADO: Isso fará 100 chamadas HTTP por página. Pode demorar muito.
		                Issue issueCompleta = manager.getIssueManager()
		                    .getIssueById(issueResumida.getId(), Include.values());
		                todasIssues.add(RedmineMapper.convertForEvidenceInfo(issueCompleta));
		            }
				}

				// Incrementa o offset com a quantidade real retornada nesta página
				offset += paginaAtual.size();

			} while (offset < totalEncontrado);

			return todasIssues;

		} catch (Exception e) {
			throw new RuntimeException("Erro ao buscar issues paginadas: " + e.getMessage(), e);
		}
	}

	public TIssue getIssuesById(Integer issueId) {

		try {
			Issue issue = this.manager.getIssueManager().getIssueById(issueId, Include.values());

			if (issue != null)
				return RedmineMapper.convert(issue);

			return null;

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public TIssueEvidenceInfo getTIssueEvidenceInfo(Integer issueId) {

		try {
			Issue issue = this.manager.getIssueManager().getIssueById(issueId, Include.values());
			return RedmineMapper.convertForEvidenceInfo(issue);

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public List<TIssue> getIssuesBySummary(String projectId, String summary) {

		try {
			List<Issue> issues = this.manager.getIssueManager().getIssuesBySummary(projectId, summary);

			if (issues != null)
				return RedmineMapper.convertIssueList(issues);

			return null;

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public TProject getProjectByIdentifier(String identifier) {

		try {
			Project project = this.manager.getProjectManager().getProjectByKey(identifier);

			if (project != null)
				return RedmineMapper.convert(project);

			return null;

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public TProject getProjectById(int idProject) {

		try {
			Project project = this.manager.getProjectManager().getProjectById(idProject);

			if (project != null)
				return RedmineMapper.convert(project);

			return null;

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public Long countAllProjects() {

		try {
			List<Project> projects = this.manager.getProjectManager().getProjects();
			if (projects != null)
				return Long.valueOf(projects.size());
			return 0L;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}

	}

	public List<TProject> listAllProjects() {

		try {
			List<Project> projects = this.manager.getProjectManager().getProjects();

			if (projects != null)
				return RedmineMapper.convertProjectList(projects);

			return List.of();

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public List<TRedmineUser> findUser(String name) {

		try {
			ResultsWrapper<User> wrapper = this.manager.getUserManager().getUsers(Map.of("name", name));

			if (wrapper != null)
				return RedmineMapper.convertUserList(wrapper.getResults());

			return List.of();

		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public List<TRedmineUser> listUsers() {
		try {
			// Fetching default page of users.
			// WARNING: fetching all users in a large instance might be slow.
			// Currently fetching 100 as per manager configuration.
			List<User> users = this.manager.getUserManager().getUsers();
			if (users != null)
				return RedmineMapper.convertUserList(users);
			return List.of();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	//TODO: Comentado porque não identifiquei sua real utilização usando a pasta TedrosFolder.EXPORT_FOLDER.getFullPath();
	
	/*public List<String> getAttachments(Collection<Attachment> attachments) {
		return attachments.stream()
				.map(this::getAttachment)
				.toList();
	}

	public String getAttachment(Attachment attachment) {

		String dir = TedrosFolder.EXPORT_FOLDER.getFullPath();
		String path = dir + attachment.getFileName();
		File f = new File(path);
		try (OutputStream out = new FileOutputStream(f)) {
			this.manager.getAttachmentManager().downloadAttachmentContent(attachment, out);
			return path;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}*/

	public List<TFileContentInfo> dowloadAttachments(Collection<Attachment> attachments) {
		return attachments.stream()
				.map(this::getAttachmentInfo)
				.toList();
	}

	public List<TFileContentInfo> dowloadTAttachments(Collection<TAttachment> attachments) {
		return attachments.stream()
				.map(this::getAttachmentInfo)
				.toList();
	}

	public TFileContentInfo getAttachmentInfo(TAttachment attachment) {
		Attachment att = RedmineMapper.convert(attachment);
		return getAttachmentInfo(att);
	}

	public TFileContentInfo getAttachmentInfo(Attachment attachment) {
		String fileName = attachment.getFileName();
		String contentType = attachment.getContentType();
		try {
			byte[] bytes = this.manager.getAttachmentManager().downloadAttachmentContent(attachment);
			return new TFileContentInfo(fileName, contentType, bytes);
		} catch (RedmineException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Uploads a file and attaches it to the given issue.
	 * <p>
	 * Some Redmine servers answer {@code 204 No Content} on issue update. The
	 * redmine-java-api 3.x stack then fails with {@code Entity may not be null}
	 * while decoding an empty body even when the attach succeeded — we tolerate
	 * that and confirm via a fresh issue read.
	 */
	public TAttachment uploadIssueAttachment(Integer issueId, byte[] content, String fileName, String contentType) {
		try {
			Attachment uploaded = manager.getAttachmentManager().uploadAttachment(fileName, contentType, content);
			Issue issue = IssueFactory.create(issueId);
			issue.addAttachment(uploaded);
			try {
				manager.getIssueManager().update(issue);
			} catch (Exception updateEx) {
				if (!isEmptyHttpEntityError(updateEx)) {
					throw updateEx;
				}
			}
			TAttachment attached = findIssueAttachmentByFileName(issueId, fileName);
			if (attached != null) {
				return attached;
			}
			// Token was uploaded but issue link could not be confirmed
			return RedmineMapper.convert(uploaded);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/** Hook for unit tests / subclasses. */
	protected TAttachment findIssueAttachmentByFileName(Integer issueId, String fileName) throws RedmineException {
		Issue refreshed = manager.getIssueManager().getIssueById(issueId, Include.attachments);
		if (refreshed == null || refreshed.getAttachments() == null || fileName == null) {
			return null;
		}
		for (Attachment a : refreshed.getAttachments()) {
			if (fileName.equals(a.getFileName())) {
				return RedmineMapper.convert(a);
			}
		}
		return null;
	}

	/**
	 * True when redmine-java-api fails because Redmine returned an empty HTTP
	 * entity (typical for 204 No Content on PUT /issues/:id.json).
	 */
	static boolean isEmptyHttpEntityError(Throwable error) {
		Throwable t = error;
		while (t != null) {
			if (t instanceof IllegalArgumentException
					&& "Entity may not be null".equals(t.getMessage())) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Creates a time entry on the issue.
	 */
	public TTimeEntry createTimeEntry(Integer issueId, Integer userId, Integer activityId, Float hours, Date spentOn,
			String comment) {
		try {
			TimeEntry entry = TimeEntryFactory.create();
			entry.setIssueId(issueId);
			if (userId != null) {
				entry.setUserId(userId);
			}
			entry.setActivityId(activityId);
			entry.setHours(hours);
			entry.setSpentOn(spentOn);
			entry.setComment(comment);
			TimeEntry created = manager.getTimeEntryManager().createTimeEntry(entry);
			return RedmineMapper.convert(created);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * Idempotent meeting-minutes update: skips attachment/time entry when already
	 * present (by stored ids or matching filename/comment/spent_on/activity/hours).
	 */
	public MeetingMinutesRedmineUpdateResult updateIssueWithMeetingMinutes(Integer issueId, Integer userId,
			Integer activityId, byte[] pdfContent, String fileName, String contentType, Float hours, Date spentOn,
			String comment, String existingAttachmentId, String existingTimeEntryId) {
		MeetingMinutesRedmineUpdateResult result = new MeetingMinutesRedmineUpdateResult();
		try {
			Integer attachmentId = parseIntOrNull(existingAttachmentId);
			boolean attachmentExists = attachmentId != null;
			if (!attachmentExists) {
				Collection<Attachment> attachments = findIssueAttachments(issueId);
				if (attachments != null) {
					for (Attachment a : attachments) {
						if (fileName != null && fileName.equals(a.getFileName())) {
							attachmentId = a.getId();
							attachmentExists = true;
							break;
						}
					}
				}
			}
			if (attachmentExists) {
				result.setAttachmentId(attachmentId);
				result.setAttachmentSkipped(true);
			} else {
				TAttachment uploaded = uploadIssueAttachment(issueId, pdfContent, fileName, contentType);
				result.setAttachmentId(uploaded != null ? uploaded.getId() : null);
				result.setAttachmentSkipped(false);
			}

			Integer timeEntryId = parseIntOrNull(existingTimeEntryId);
			boolean timeExists = timeEntryId != null;
			List<TTimeEntry> entries = getTimeEntriesForIssue(issueId);
			SimpleDateFormat day = new SimpleDateFormat("yyyy-MM-dd");
			String spentKey = spentOn != null ? day.format(spentOn) : null;
			if (!timeExists) {
				for (TTimeEntry te : entries) {
					boolean sameComment = Objects.equals(comment, te.getComment());
					boolean sameActivity = Objects.equals(activityId, te.getActivityId());
					boolean sameHours = te.getHours() != null && hours != null
							&& Math.abs(te.getHours() - hours) < 0.01f;
					boolean sameDay = te.getSpentOn() != null && spentKey != null
							&& spentKey.equals(day.format(te.getSpentOn()));
					boolean sameUser = userId == null || Objects.equals(userId, te.getUserId());
					if (sameComment && sameActivity && sameHours && sameDay && sameUser) {
						timeEntryId = te.getId();
						timeExists = true;
						break;
					}
				}
			}
			if (timeExists) {
				result.setTimeEntryId(timeEntryId);
				result.setTimeEntrySkipped(true);
			} else {
				Integer effectiveUserId = userId != null ? userId : resolveCurrentRedmineUserId();
				TTimeEntry created = createTimeEntry(issueId, effectiveUserId, activityId, hours, spentOn, comment);
				result.setTimeEntryId(created != null ? created.getId() : null);
				result.setTimeEntrySkipped(false);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/** Hook for unit tests / subclasses. */
	protected Collection<Attachment> findIssueAttachments(Integer issueId) throws RedmineException {
		Issue issue = manager.getIssueManager().getIssueById(issueId, Include.attachments);
		return issue != null ? issue.getAttachments() : List.of();
	}

	/** Hook for unit tests / subclasses. */
	protected Integer resolveCurrentRedmineUserId() throws RedmineException {
		User current = manager.getUserManager().getCurrentUser();
		return current != null ? current.getId() : null;
	}

	static Integer parseIntOrNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Integer.valueOf(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
