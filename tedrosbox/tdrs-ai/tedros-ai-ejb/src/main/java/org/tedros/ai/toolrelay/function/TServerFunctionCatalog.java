package org.tedros.ai.toolrelay.function;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.tedros.server.util.TLoggerUtil;

import dev.langchain4j.agent.tool.ToolSpecification;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Catalogo das tools de backend: descobre as implementacoes de
 * {@link TServerAiFunction} via CDI e as indexa por nome. As
 * {@link ToolSpecification}s sao geradas uma unica vez.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TServerFunctionCatalog {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TServerFunctionCatalog.class);

	@Inject
	private Instance<TServerAiFunction> discovered;

	private final Map<String, TServerAiFunction> functions = new ConcurrentHashMap<>();
	private volatile List<ToolSpecification> specifications = List.of();

	public TServerFunctionCatalog() {

	}

	/**
	 * Construtor para testes de unidade (sem container CDI).
	 */
	public TServerFunctionCatalog(Collection<TServerAiFunction> fns) {
		fns.forEach(this::register);
	}

	@PostConstruct
	void init() {
		if (discovered != null)
			discovered.forEach(this::register);
		LOGGER.info("AI relay backend tool catalog initialized with {} tool(s)", functions.size());
	}

	private void register(TServerAiFunction fn) {
		TServerAiFunction previous = functions.put(fn.getName(), fn);
		if (previous != null)
			LOGGER.warn("Duplicated backend tool name '{}' — keeping {}", fn.getName(),
					fn.getClass().getName());
		rebuildSpecifications();
	}

	private void rebuildSpecifications() {
		List<ToolSpecification> specs = new ArrayList<>();
		functions.values().forEach(fn -> specs.add(ToolSpecification.builder()
				.name(fn.getName())
				.description(fn.getDescription())
				.parameters(TJsonSchemaConverter.fromModelClass(fn.getModel()))
				.build()));
		this.specifications = List.copyOf(specs);
	}

	public boolean contains(String name) {
		return functions.containsKey(name);
	}

	public Optional<TServerAiFunction> byName(String name) {
		return Optional.ofNullable(functions.get(name));
	}

	public List<ToolSpecification> toolSpecifications() {
		return specifications;
	}
}
