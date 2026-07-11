package org.tedros.ai.toolrelay.function;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.tedros.integration.annotation.TRequiredProperty;
import org.tedros.server.util.TLoggerUtil;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.json.JsonSanitizer;

import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

/**
 * Conversao entre JSON Schema (String, enviado pelo frontend em
 * {@code TAiClientToolSpec.parametersSchemaJson}) e os elementos de schema do
 * langchain4j ({@link JsonSchemaElement}).
 * <p>
 * O langchain4j 1.17.2 nao oferece um {@code fromMap} pronto (apenas
 * {@code JsonSchemaElementUtils.toMap}), portanto a reconstrucao e feita
 * recursivamente com os builders. Cobre: {@code type}, {@code description},
 * {@code properties}, {@code required}, {@code items} e {@code enum}.
 * <p>
 * Tambem replica a geracao de schema por reflexao usada no frontend
 * ({@code LangChainFunctionExecutor.generateJsonSchema}) para os modelos das
 * tools de backend.
 *
 * @author Davis Gordon
 */
public final class TJsonSchemaConverter {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TJsonSchemaConverter.class);

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TJsonSchemaConverter() {

	}

	/**
	 * Reconstroi um {@link JsonObjectSchema} a partir de um JSON Schema em
	 * String. O JSON e sanitizado antes do parse por vir do cliente.
	 */
	@SuppressWarnings("unchecked")
	public static JsonObjectSchema fromJson(String schemaJson) {
		if (schemaJson == null || schemaJson.isBlank())
			return JsonObjectSchema.builder().build();
		try {
			String sanitized = JsonSanitizer.sanitize(schemaJson);
			Map<String, Object> map = MAPPER.readValue(sanitized, Map.class);
			JsonSchemaElement el = elementFromMap(map);
			if (el instanceof JsonObjectSchema jos)
				return jos;
			LOGGER.warn("Schema root is not an object, returning empty object schema");
			return JsonObjectSchema.builder().build();
		} catch (Exception e) {
			LOGGER.error("Error parsing tool parameters schema, returning empty object schema", e);
			return JsonObjectSchema.builder().build();
		}
	}

	/**
	 * Serializa um {@link JsonSchemaElement} para JSON Schema em String
	 * (inverso de {@link #fromJson(String)}).
	 */
	public static String toJson(JsonSchemaElement element) {
		try {
			Map<String, Object> map = JsonSchemaElementUtils.toMap(element);
			return MAPPER.writeValueAsString(map);
		} catch (Exception e) {
			LOGGER.error("Error serializing schema element", e);
			return "{}";
		}
	}

	@SuppressWarnings("unchecked")
	static JsonSchemaElement elementFromMap(Map<String, Object> map) {
		if (map == null)
			return JsonObjectSchema.builder().build();

		String description = map.get("description") instanceof String d ? d : null;

		// enum tem precedencia sobre type (toMap emite {"type":"string","enum":[..]})
		if (map.get("enum") instanceof List<?> values) {
			List<String> enumValues = new ArrayList<>();
			values.forEach(v -> enumValues.add(String.valueOf(v)));
			return JsonEnumSchema.builder().description(description).enumValues(enumValues).build();
		}

		String type = map.get("type") instanceof String t ? t : "object";

		switch (type) {
		case "string":
			return JsonStringSchema.builder().description(description).build();
		case "integer":
			return JsonIntegerSchema.builder().description(description).build();
		case "number":
			return JsonNumberSchema.builder().description(description).build();
		case "boolean":
			return JsonBooleanSchema.builder().description(description).build();
		case "array": {
			JsonSchemaElement items = map.get("items") instanceof Map<?, ?> im
					? elementFromMap((Map<String, Object>) im)
					: JsonObjectSchema.builder().build();
			return JsonArraySchema.builder().description(description).items(items).build();
		}
		case "object":
		default: {
			JsonObjectSchema.Builder builder = JsonObjectSchema.builder().description(description);
			if (map.get("properties") instanceof Map<?, ?> props) {
				for (Map.Entry<?, ?> e : props.entrySet()) {
					if (e.getValue() instanceof Map<?, ?> pm)
						builder.addProperty(String.valueOf(e.getKey()),
								elementFromMap((Map<String, Object>) pm));
				}
			}
			if (map.get("required") instanceof List<?> req) {
				List<String> required = new ArrayList<>();
				req.forEach(r -> required.add(String.valueOf(r)));
				if (!required.isEmpty())
					builder.required(required);
			}
			return builder.build();
		}
		}
	}

	/**
	 * Gera o {@link JsonObjectSchema} do modelo de parametros de uma tool de
	 * backend por reflexao — mesma logica (e mesmos tipos suportados) do
	 * {@code LangChainFunctionExecutor} do frontend: String/enum/datas mapeiam
	 * para string; inteiros; decimais; boolean; List/array; objetos aninhados;
	 * campos {@link TRequiredProperty} entram em {@code required}.
	 */
	public static JsonObjectSchema fromModelClass(Class<?> modelClass) {
		if (modelClass == null)
			return JsonObjectSchema.builder().build();
		try {
			return buildObjectSchema(modelClass);
		} catch (Exception e) {
			LOGGER.error("Error generating schema for class " + modelClass.getName(), e);
			return JsonObjectSchema.builder().build();
		}
	}

	private static JsonObjectSchema buildObjectSchema(Class<?> targetClass) {
		JsonObjectSchema.Builder builder = JsonObjectSchema.builder();
		List<String> required = new ArrayList<>();
		for (java.lang.reflect.Field field : targetClass.getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()))
				continue;
			builder.addProperty(field.getName(), mapType(field.getGenericType()));
			if (field.isAnnotationPresent(TRequiredProperty.class))
				required.add(field.getName());
		}
		if (!required.isEmpty())
			builder.required(required);
		return builder.build();
	}

	private static JsonSchemaElement mapType(Type type) {
		Class<?> rawClass = null;
		if (type instanceof Class<?> c)
			rawClass = c;
		else if (type instanceof ParameterizedType pt)
			rawClass = (Class<?>) pt.getRawType();

		if (rawClass == null)
			return JsonObjectSchema.builder().build();

		if (String.class.isAssignableFrom(rawClass) || rawClass.isEnum()
				|| Date.class.isAssignableFrom(rawClass) || LocalDate.class.isAssignableFrom(rawClass)
				|| LocalDateTime.class.isAssignableFrom(rawClass) || LocalTime.class.isAssignableFrom(rawClass)) {
			return JsonStringSchema.builder().build();
		} else if (Integer.class.isAssignableFrom(rawClass) || int.class.isAssignableFrom(rawClass)
				|| Long.class.isAssignableFrom(rawClass) || long.class.isAssignableFrom(rawClass)) {
			return JsonIntegerSchema.builder().build();
		} else if (Double.class.isAssignableFrom(rawClass) || double.class.isAssignableFrom(rawClass)
				|| Float.class.isAssignableFrom(rawClass) || float.class.isAssignableFrom(rawClass)) {
			return JsonNumberSchema.builder().build();
		} else if (Boolean.class.isAssignableFrom(rawClass) || boolean.class.isAssignableFrom(rawClass)) {
			return JsonBooleanSchema.builder().build();
		} else if (List.class.isAssignableFrom(rawClass) || rawClass.isArray()) {
			JsonSchemaElement itemSchema = JsonObjectSchema.builder().build();
			if (type instanceof ParameterizedType pType) {
				Type[] args = pType.getActualTypeArguments();
				if (args != null && args.length > 0)
					itemSchema = mapType(args[0]);
			} else if (rawClass.isArray()) {
				itemSchema = mapType(rawClass.getComponentType());
			}
			return JsonArraySchema.builder().items(itemSchema).build();
		} else {
			if (rawClass.getName().startsWith("java."))
				return JsonObjectSchema.builder().build();
			return buildObjectSchema(rawClass);
		}
	}
}
