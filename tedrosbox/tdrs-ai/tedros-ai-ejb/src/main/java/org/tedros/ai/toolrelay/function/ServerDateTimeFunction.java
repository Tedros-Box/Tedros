package org.tedros.ai.toolrelay.function;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tool de backend de validacao da Fase 1: retorna data/hora do servidor.
 * Serve para provar o caminho LLM → tool de BE → usuario.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class ServerDateTimeFunction implements TServerAiFunction {

	/** Modelo de parametros vazio — a tool nao recebe argumentos. */
	public static class Empty {

	}

	@Override
	public String getName() {
		return "get_server_datetime";
	}

	@Override
	public String getDescription() {
		return "Returns the current date and time on the Tedros server, with the server time zone.";
	}

	@Override
	public Class<?> getModel() {
		return Empty.class;
	}

	@Override
	public Object execute(Object arg, TAiToolContext ctx) {
		ZonedDateTime now = ZonedDateTime.now();
		return Map.of(
				"datetime", now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
				"zone", ZoneId.systemDefault().getId());
	}
}
