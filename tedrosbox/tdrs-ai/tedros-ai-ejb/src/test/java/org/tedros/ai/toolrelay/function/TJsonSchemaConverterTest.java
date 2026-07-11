package org.tedros.ai.toolrelay.function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.tedros.integration.annotation.TRequiredProperty;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.internal.JsonSchemaElementUtils;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;

/**
 * Conversao de schema ida-e-volta: o JSON produzido a partir de um
 * {@code JsonSchemaElement} (mesmo formato exportado pelo
 * {@code ClientToolSpecExporter} do FE) deve ser reconstruido de forma
 * equivalente pelo {@link TJsonSchemaConverter}.
 */
public class TJsonSchemaConverterTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	enum Color {
		RED, GREEN
	}

	static class Nested {
		@TRequiredProperty
		String code;
		Integer amount;
	}

	static class SampleModel {
		@TRequiredProperty
		String name;
		Color color;
		LocalDate date;
		int count;
		Long total;
		double rate;
		boolean active;
		List<String> tags;
		List<Nested> items;
		Nested nested;
	}

	@Test
	public void modelClassGeneratesExpectedTypes() {
		JsonObjectSchema schema = TJsonSchemaConverter.fromModelClass(SampleModel.class);

		assertTrue(schema.properties().get("name") instanceof JsonStringSchema);
		assertTrue("enum mapeia para string (mesmo comportamento do FE)",
				schema.properties().get("color") instanceof JsonStringSchema);
		assertTrue(schema.properties().get("date") instanceof JsonStringSchema);
		assertTrue(schema.properties().get("count") instanceof JsonIntegerSchema);
		assertTrue(schema.properties().get("total") instanceof JsonIntegerSchema);
		assertTrue(schema.properties().get("rate") instanceof JsonNumberSchema);
		assertTrue(schema.properties().get("active") instanceof JsonBooleanSchema);
		assertTrue(schema.properties().get("tags") instanceof JsonArraySchema);
		assertTrue(((JsonArraySchema) schema.properties().get("tags")).items() instanceof JsonStringSchema);
		assertTrue(schema.properties().get("items") instanceof JsonArraySchema);
		assertTrue(((JsonArraySchema) schema.properties().get("items")).items() instanceof JsonObjectSchema);
		assertTrue(schema.properties().get("nested") instanceof JsonObjectSchema);
		assertEquals(List.of("name"), schema.required());

		JsonObjectSchema nested = (JsonObjectSchema) schema.properties().get("nested");
		assertEquals(List.of("code"), nested.required());
	}

	@Test
	public void roundTripFromModelClass() throws Exception {
		JsonObjectSchema original = TJsonSchemaConverter.fromModelClass(SampleModel.class);

		String json = TJsonSchemaConverter.toJson(original);
		JsonObjectSchema rebuilt = TJsonSchemaConverter.fromJson(json);

		assertEquals(JsonSchemaElementUtils.toMap(original), JsonSchemaElementUtils.toMap(rebuilt));
	}

	@Test
	public void roundTripFromJsonString() throws Exception {
		String json = """
				{"type":"object","description":"params",
				 "properties":{
				   "viewPath":{"type":"string","description":"path of the view"},
				   "limit":{"type":"integer"},
				   "ratio":{"type":"number"},
				   "flag":{"type":"boolean"},
				   "mode":{"type":"string","enum":["A","B"]},
				   "names":{"type":"array","items":{"type":"string"}},
				   "child":{"type":"object","properties":{"id":{"type":"integer"}},"required":["id"]}
				 },
				 "required":["viewPath","mode"]}
				""";

		JsonObjectSchema schema = TJsonSchemaConverter.fromJson(json);
		String out = TJsonSchemaConverter.toJson(schema);

		Map<?, ?> expected = MAPPER.readValue(json, Map.class);
		Map<?, ?> actual = MAPPER.readValue(out, Map.class);
		assertEquals(expected, actual);
	}

	@Test
	public void invalidOrEmptySchemaFallsBackToEmptyObject() {
		assertNotNull(TJsonSchemaConverter.fromJson(null));
		assertNotNull(TJsonSchemaConverter.fromJson(" "));
		assertNotNull(TJsonSchemaConverter.fromJson("not a json"));
		JsonObjectSchema empty = TJsonSchemaConverter.fromJson("{}");
		assertTrue(empty.properties() == null || empty.properties().isEmpty());
	}
}
