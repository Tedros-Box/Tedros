package org.tedros.integration.redmine.ai.model;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Enum que representa todos os campos conhecidos que podem ser utilizados
 * como parâmetros de filtragem em consultas de issues do Redmine.
 *
 * <p>Inclui campos padrão, campos de data, numéricos, relacionais e suporte a
 * campos personalizados (cf_X) em níveis de Tarefa, Projeto e Usuário.</p>
 *
 * <p>Baseado nos filtros disponíveis na interface web do Redmine.</p>
 *
 * @author
 *   Davis Gordon Dun
 */
public enum RedmineFilterField {

    // ===============================================================
    // 🔹 CAMPOS DE STATUS E FLUXO
    // ===============================================================

    /** ID do status da issue (ex: Novo, Em andamento, Resolvido, etc.) */
    STATUS_ID("status_id", FilterType.NUMBER),
    ISSUE_STATUS_ID("issue.status_id", FilterType.NUMBER),

    /** ID da prioridade (ex: Alta, Normal, Baixa) */
    PRIORITY_ID("priority_id", FilterType.NUMBER),

    /** ID do rastreador (ex: Bug, Tarefa, Suporte, etc.) */
    TRACKER_ID("tracker_id", FilterType.NUMBER),
    ISSUE_TRACKER_ID("issue.tracker_id", FilterType.NUMBER),

    /** ID da versão (milestone) associada à issue */
    FIXED_VERSION_ID("fixed_version_id", FilterType.NUMBER),
    ISSUE_FIXED_VERSION_ID("issue.fixed_version_id", FilterType.NUMBER),

    /** ID da categoria da issue (se houver) */
    CATEGORY_ID("category_id", FilterType.NUMBER),
    ISSUE_CATEGORY_ID("issue.category_id", FilterType.NUMBER),

    /** Progresso da issue (0–100%) */
    DONE_RATIO("done_ratio", FilterType.NUMBER),

    // ===============================================================
    // 🔹 CAMPOS DE RELACIONAMENTO (USUÁRIOS / AUTORES)
    // ===============================================================

    /** ID do usuário a quem a issue está atribuída */
    ASSIGNED_TO_ID("assigned_to_id", FilterType.NUMBER),

    /** ID do autor (quem criou a issue) */
    AUTHOR_ID("author_id", FilterType.NUMBER),

    /** ID do usuário genérico */
    USER_ID("user_id", FilterType.NUMBER),

    /** ID do usuário observador (watcher) */
    WATCHER_ID("watcher_id", FilterType.NUMBER),

    /** ID do usuário que atualizou por último a issue */
    UPDATED_BY_ID("updated_by_id", FilterType.NUMBER),

    // ===============================================================
    // 🔹 CAMPOS DE PROJETO E ORGANIZAÇÃO
    // ===============================================================

    /** ID do projeto ao qual a issue pertence */
    PROJECT_ID("project_id", FilterType.NUMBER),

    /** ID do subprojeto */
    SUBPROJECT_ID("subproject_id", FilterType.NUMBER),

    /** Situação do projeto */
    PROJECT_STATUS("project.status", FilterType.NUMBER),

    /** Nome do projeto */
    PROJECT("project", FilterType.TEXT),

    /** Nome do rastreador (Tracker) */
    TRACKER("tracker", FilterType.TEXT),

    /** Nome do status da issue */
    STATUS("status", FilterType.TEXT),

    /** Nome do usuário atribuído */
    ASSIGNED_TO("assigned_to", FilterType.TEXT),

    /** Identificador do autor */
    AUTHOR("author", FilterType.TEXT),

    // ===============================================================
    // 🔹 CAMPOS DE DATAS
    // ===============================================================

    /** Data de início planejada */
    START_DATE("start_date", FilterType.DATE),

    /** Data de vencimento */
    DUE_DATE("due_date", FilterType.DATE),

    /** Data de criação da issue */
    CREATED_ON("created_on", FilterType.DATE),

    /** Data da última atualização */
    UPDATED_ON("updated_on", FilterType.DATE),

    /** Data de fechamento da issue */
    CLOSED_ON("closed_on", FilterType.DATE),

    /** Data do registro de tempo / horas gastas */
    SPENT_ON("spent_on", FilterType.DATE),

    // ===============================================================
    // 🔹 CAMPOS DE DESCRIÇÃO E CONTEÚDO
    // ===============================================================

    /** Título/assunto da issue */
    SUBJECT("subject", FilterType.TEXT),

    /** Descrição da issue */
    DESCRIPTION("description", FilterType.TEXT),

    /** Notas (comentários) da issue */
    NOTES("notes", FilterType.TEXT),

    /** Comentários em lançamentos de tempo / apontamentos */
    COMMENTS("comments", FilterType.TEXT),

    /** Tags associadas à issue */
    ISSUE_TAGS("issue_tags", FilterType.TEXT),

    // ===============================================================
    // 🔹 CAMPOS DE TEMPO / ESTIMATIVAS
    // ===============================================================

    /** Horas estimadas para conclusão */
    ESTIMATED_HOURS("estimated_hours", FilterType.NUMBER),

    /** Horas gastas (registradas) */
    SPENT_HOURS("spent_hours", FilterType.NUMBER),

    /** Horas apontadas */
    HOURS("hours", FilterType.NUMBER),

    /** Atividade relacionada ao apontamento de horas */
    ACTIVITY_ID("activity_id", FilterType.NUMBER),

    // ===============================================================
    // 🔹 CAMPOS DE RELAÇÕES ENTRE ISSUES
    // ===============================================================

    /** ID da issue pai */
    PARENT_ID("parent_id", FilterType.NUMBER),

    /** Issues filhas (subtasks) */
    CHILDREN("children", FilterType.TEXT),

    /** Relações gerais entre issues (ex: bloqueia, duplicado, relacionado) */
    RELATIONS("relations", FilterType.TEXT),

    // ===============================================================
    // 🔹 OUTROS CAMPOS ÚTEIS
    // ===============================================================

    /** Número da issue */
    ISSUE_ID("issue_id", FilterType.NUMBER),

    /** Nome da versão associada */
    FIXED_VERSION("fixed_version", FilterType.TEXT),

    /** Nome da categoria */
    CATEGORY("category", FilterType.TEXT),

    // ===============================================================
    // 🔹 CAMPOS PERSONALIZADOS - TAREFA (Issue Custom Fields)
    // ===============================================================

    ISSUE_CF_1("issue.cf_1", FilterType.TEXT),         // Tarefa Área
    ISSUE_CF_4("issue.cf_4", FilterType.TEXT),         // Tarefa Nº SEI!
    ISSUE_CF_12("issue.cf_12", FilterType.NUMBER),     // Tarefa Quantidade
    ISSUE_CF_114("issue.cf_114", FilterType.TEXT),     // Tarefa Classificação da demanda
    ISSUE_CF_58("issue.cf_58", FilterType.TEXT),       // Tarefa Chamado GLPI / 4Biz / Nº SEI
    ISSUE_CF_83("issue.cf_83", FilterType.TEXT),       // Tarefa Fase
    ISSUE_CF_109("issue.cf_109", FilterType.TEXT),     // Tarefa OS (Memora)
    ISSUE_CF_96("issue.cf_96", FilterType.TEXT),       // Tarefa Serviço (Memora)
    ISSUE_CF_100("issue.cf_100", FilterType.TEXT),     // Tarefa Entregável (Memora)
    ISSUE_CF_113("issue.cf_113", FilterType.NUMBER),   // Tarefa Story Points

    // ===============================================================
    // 🔹 CAMPOS PERSONALIZADOS - PROJETO (Project Custom Fields)
    // ===============================================================

    PROJECT_CF_78("project.cf_78", FilterType.TEXT),   // Projeto Área gestora
    PROJECT_CF_84("project.cf_84", FilterType.TEXT),   // Projeto Gestor / Dono do Produto
    PROJECT_CF_85("project.cf_85", FilterType.TEXT),   // Projeto Tipo de produto
    PROJECT_CF_87("project.cf_87", FilterType.TEXT),   // Projeto Status / Fase atual
    PROJECT_CF_88("project.cf_88", FilterType.TEXT),   // Projeto Bibliotecas Internas
    PROJECT_CF_89("project.cf_89", FilterType.TEXT),   // Projeto Integrações com API internas
    PROJECT_CF_91("project.cf_91", FilterType.TEXT),   // Projeto Criticidade
    PROJECT_CF_92("project.cf_92", FilterType.TEXT),   // Projeto Linguagem / Tecnologia
    PROJECT_CF_106("project.cf_106", FilterType.TEXT), // Projeto Gerente de Projeto
    PROJECT_CF_107("project.cf_107", FilterType.TEXT), // Projeto Áreas da

    // ===============================================================
    // 🔹 CAMPOS PERSONALIZADOS - USUÁRIO (User Custom Fields)
    // ===============================================================

    USER_CF_3("user.cf_3", FilterType.TEXT),           // Usuário Unidade Administrativa
    USER_CF_11("user.cf_11", FilterType.TEXT),         // Usuário CPF

    // ===============================================================
    // 🔹 CAMPO PERSONALIZADO GENÉRICO (Fallback)
    // ===============================================================

    /**
     * Campo genérico para outros campos personalizados não mapeados estaticamente.
     * <p>Exemplo: <code>cf_30</code>, <code>issue.cf_999</code>.</p>
     */
    CUSTOM_FIELD("cf_", FilterType.TEXT);

    // ===============================================================
    // 🔹 ATRIBUTOS INTERNOS
    // ===============================================================

    private final String fieldName;
    private final FilterType type;
    private static final ObjectMapper mapper = new ObjectMapper();

    RedmineFilterField(String fieldName, FilterType type) {
        this.fieldName = fieldName;
        this.type = type;
    }

    /** Retorna o nome do campo como reconhecido pela API do Redmine */
    public String getFieldName() {
        return fieldName;
    }

    /** Retorna o tipo de dado (texto, número, data, booleano) */
    public FilterType getType() {
        return type;
    }

    // ===============================================================
    // 🔹 MÉTODOS AUXILIARES
    // ===============================================================

    /**
     * Verifica se o nome representa um campo personalizado (cf_X, issue.cf_X, project.cf_X, user.cf_X).
     */
    public static boolean isCustomField(String fieldName) {
        if (fieldName == null) return false;
        return fieldName.startsWith("cf_") 
            || fieldName.startsWith("issue.cf_") 
            || fieldName.startsWith("project.cf_") 
            || fieldName.startsWith("user.cf_");
    }

    /** Tenta localizar um campo pelo nome interno. */
    public static RedmineFilterField fromFieldName(String name) {
        if (name == null) return null;
        for (RedmineFilterField f : values()) {
            if (f.fieldName.equalsIgnoreCase(name)) {
                return f;
            }
        }
        return null;
    }

    /**
     * Converte um mapa genérico de filtros em um mapa tipado com {@link FilterCondition}.
     */
    public static Map<String, FilterCondition> fromRawMap(Map<String, Object> rawFilters) {
        Map<String, FilterCondition> result = new HashMap<>();
        if (rawFilters == null || rawFilters.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, Object> entry : rawFilters.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            RedmineFilterField field = fromFieldName(fieldName);
            FilterType type = (field != null) ? field.getType() : FilterType.TEXT;

            if (value instanceof Map<?, ?> mapValue) {
                Object opObj = mapValue.get("op");
                String op = opObj != null ? opObj.toString() : "=";

                Object val = mapValue.get("value");
                FilterCondition condition = null;

                if (val instanceof Boolean bVal) {
                    condition = FilterCondition.to(op, bVal);
                } else if (val instanceof String strVal) {
                    condition = FilterCondition.auto(type, op, strVal);
                } else if (val instanceof Number numVal) {
                    condition = FilterCondition.auto(type, op, numVal.toString());
                } else if (val instanceof Map<?, ?> dateRange
                        && type == FilterType.DATE
                        && dateRange.containsKey("from")
                        && dateRange.containsKey("to")) {
                    LocalDate from = LocalDate.parse(dateRange.get("from").toString());
                    LocalDate to = LocalDate.parse(dateRange.get("to").toString());
                    condition = FilterCondition.betweenDates(from, to);
                }

                if (condition != null) {
                    result.put(fieldName, condition);
                }

            } else if (value != null) {
                result.put(fieldName, FilterCondition.equalsTo(String.valueOf(value)));
            }
        }

        return result;
    }

    public static Map<String, FilterCondition> fromJSON(String json) throws IOException {
        Map<String, Object> raw = mapper.readValue(json, new TypeReference<>() {});
        return RedmineFilterField.fromRawMap(raw);
    }

    public static Map<String, FilterCondition> fromObject(Object obj) {
        Map<String, FilterCondition> map = new HashMap<>();
        if (obj == null) return map;

        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof FilterCondition fc) {
                    map.put(field.getName(), fc);
                }
            } catch (Exception ignored) {}
        }
        return map;
    }
}