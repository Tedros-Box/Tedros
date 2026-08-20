package org.tedros.integration.redmine.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.tedros.integration.redmine.api.model.TIssueEvidenceInfo;

import com.taskadapter.redmineapi.bean.CustomField;
import com.taskadapter.redmineapi.bean.CustomFieldFactory;
import com.taskadapter.redmineapi.bean.Issue;
import com.taskadapter.redmineapi.bean.IssueFactory;

public class RedmineMapperEvidenceInfoTest {

	private static CustomField createCustomField(int id, String name, String value) {
		CustomField cf = CustomFieldFactory.create(id);
		cf.setName(name);
		cf.setValue(value);
		return cf;
	}

	@Test
	public void convertsSemadCustomFieldsCorrectly() {
		Issue issue = IssueFactory.create(43632);
		issue.setSubject("Análise exploratória de processos de licenciamento ambiental do IPE via agentes de IA");
		issue.setDescription("Análise exploratória");
		issue.setStatusName("Pronta p/ revisão (fechada)");
		issue.setEstimatedHours(40.0f);
		issue.setSpentHours(15.35f);

		String serviceVal = "53 - [IA/Machine Learning] Entendimento dos Dados - Complexidade Única - Por evento/demanda. - HPA: 40 - Dicionário de dados; Relatório de Análise Exploratória;Relatório de qualidade; Códigos. - Cientista de Dados; Arquiteto de Machine Learning Junior,Pleno ou Sênior.";
		String deliverableVal = " Dicionário de dados; Relatório de Análise Exploratória;Relatório de qualidade; Códigos. ";
		String osVal = "OS11 - IA SEMAD - 08/2026 => 64.74";
		String glpiVal = "804";
		String qtdVal = "1";

		issue.addCustomFields(List.of(
				createCustomField(1, "Área", "GETEC"),
				createCustomField(4, "Nº SEI!", "202600017000000"),
				createCustomField(12, "Quantidade", qtdVal),
				createCustomField(58, "Chamado GLPI / 4Biz / Nº SEI", glpiVal),
				createCustomField(83, "Fase", "Execução"),
				createCustomField(96, "Serviço (Memora)", serviceVal),
				createCustomField(100, "Entregável (Memora)", deliverableVal),
				createCustomField(109, "OS (Memora)", osVal),
				createCustomField(113, "Story Points", "5"),
				createCustomField(114, "Classificação da demanda", "Evolutiva")
		));

		TIssueEvidenceInfo info = RedmineMapper.convertForEvidenceInfo(issue);

		assertNotNull(info);
		assertEquals(Long.valueOf(43632), info.getId());
		assertEquals("Análise exploratória de processos de licenciamento ambiental do IPE via agentes de IA", info.getSubject());

		// SEMAD custom fields
		assertEquals("GETEC", info.getArea());
		assertEquals("202600017000000", info.getSeiNumber());
		assertEquals("1", info.getQuantity());
		assertEquals("804", info.getGlpiOrSei());
		assertEquals("Execução", info.getPhase());
		assertEquals("5", info.getStoryPoints());
		assertEquals("Evolutiva", info.getDemandClassification());
		assertEquals(osVal, info.getOs());
		assertEquals(serviceVal, info.getServiceType());
		assertEquals("Dicionário de dados; Relatório de Análise Exploratória;Relatório de qualidade; Códigos.", info.getDeliverable());

		// Extracted from serviceType
		assertEquals("40", info.getHpa());
		assertEquals("Cientista de Dados; Arquiteto de Machine Learning Junior,Pleno ou Sênior.", info.getRequiredProfile());

		// All custom fields list
		assertNotNull(info.getCustomFields());
		assertEquals(10, info.getCustomFields().size());
	}

	@Test
	public void parsesHpaWithDecimalAndProfileCorrectly() {
		Issue issue = IssueFactory.create(44213);
		String serviceVal = "45 - [Suporte e Desenvolvimento] Participação em Reuniões - Complexidade Única - Por evento de participação em reuniões, a cada 15 minutos. - HPA: 0.25 - Atas de reunião - Perfis Juniores, Plenos ou Seniores.";

		issue.addCustomFields(List.of(
				createCustomField(96, "Serviço (Memora)", serviceVal)
		));

		TIssueEvidenceInfo info = RedmineMapper.convertForEvidenceInfo(issue);

		assertNotNull(info);
		assertEquals(serviceVal, info.getServiceType());
		assertEquals("0.25", info.getHpa());
		assertEquals("Perfis Juniores, Plenos ou Seniores.", info.getRequiredProfile());
		assertEquals("Atas de reunião", info.getDeliverable());
	}
}
