package org.tedros.ai.observability;

import java.time.Duration;
import java.util.function.IntSupplier;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Registro central de metricas do Tool Relay. Detem o {@link MeterRegistry}
 * compartilhado por todo o modulo de IA e expoe fachadas de baixa cardinalidade.
 * <p>
 * REGRA DE OURO: nenhuma metrica exposta ao Prometheus pode ter {@code userId},
 * {@code conversationId} ou {@code apiKey} como label (alta cardinalidade). O
 * consumo por usuario e persistido no banco (ver {@code TAiUsageEventSink}).
 * <p>
 * Em producao o registro e um {@link PrometheusMeterRegistry} e e publicado no
 * {@link TAiMetricsHolder} para que o WAR de exposicao ({@code /ai/metrics})
 * alcance o mesmo registro por JVM. Nos testes de unidade, instancie com o
 * construtor {@link #TAiMetrics(MeterRegistry)} passando um
 * {@code SimpleMeterRegistry} — o holder nao e tocado.
 *
 * @author Davis Gordon
 */
@ApplicationScoped
public class TAiMetrics {

	static final String METRIC_TOKENS = "tedros_ai_tokens_total";
	static final String METRIC_LLM_SECONDS = "tedros_ai_llm_request_seconds";
	static final String METRIC_TOOL_CALLS = "tedros_ai_tool_calls_total";
	static final String METRIC_TOOL_SECONDS = "tedros_ai_tool_execution_seconds";
	static final String METRIC_TURNS = "tedros_ai_turns_total";
	static final String METRIC_TURN_SECONDS = "tedros_ai_turn_seconds";
	static final String METRIC_TURN_DEPTH = "tedros_ai_turn_depth";
	static final String METRIC_ERRORS = "tedros_ai_errors_total";
	static final String METRIC_CONVERSATIONS_ACTIVE = "tedros_ai_conversations_active";
	static final String METRIC_CONVERSATIONS_MAX = "tedros_ai_conversations_max";
	static final String METRIC_PENDING_TURNS = "tedros_ai_pending_turns_active";
	static final String METRIC_EVICTIONS = "tedros_ai_conversation_evictions_total";
	static final String METRIC_CACHE_SIZE = "tedros_ai_chatmodel_cache_size";
	static final String METRIC_CACHE_CLEARS = "tedros_ai_chatmodel_cache_clears_total";

	private MeterRegistry registry;
	private PrometheusMeterRegistry prometheusRegistry;

	/** Construtor usado pelo container CDI. */
	public TAiMetrics() {
	}

	/** Construtor de teste — recebe um registro arbitrario (ex.: SimpleMeterRegistry). */
	public TAiMetrics(MeterRegistry registry) {
		this.registry = registry;
		if (registry instanceof PrometheusMeterRegistry p)
			this.prometheusRegistry = p;
	}

	@PostConstruct
	void init() {
		prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
		registry = prometheusRegistry;
		registry.config().commonTags("app", "tedros-ai", "node", nodeName());

		new JvmMemoryMetrics().bindTo(registry);
		new JvmGcMetrics().bindTo(registry);
		new JvmThreadMetrics().bindTo(registry);
		new ProcessorMetrics().bindTo(registry);
		new UptimeMetrics().bindTo(registry);
		new FileDescriptorMetrics().bindTo(registry);

		// publica o registro para o endpoint HTTP (WAR) do mesmo JVM
		TAiMetricsHolder.register(prometheusRegistry);
	}

	// ---------------------------------------------------------------- tokens

	/** Tokens creditados por resposta do LLM (chamado no loop, por iteracao). */
	public void recordTokens(String provider, String model, Long input, Long output) {
		if (registry == null)
			return;
		String p = safe(provider);
		String m = safe(model);
		if (input != null && input > 0)
			Counter.builder(METRIC_TOKENS).tag("provider", p).tag("model", m).tag("type", "input")
					.register(registry).increment(input);
		if (output != null && output > 0)
			Counter.builder(METRIC_TOKENS).tag("provider", p).tag("model", m).tag("type", "output")
					.register(registry).increment(output);
	}

	// ------------------------------------------------------------------- llm

	/** Inicia a medicao de uma chamada ao LLM. */
	public Timer.Sample startLlm() {
		return registry != null ? Timer.start(registry) : null;
	}

	/** Encerra a medicao de uma chamada ao LLM. */
	public void stopLlm(Timer.Sample sample, String provider, String model, String outcome) {
		if (registry == null || sample == null)
			return;
		sample.stop(Timer.builder(METRIC_LLM_SECONDS)
				.tag("provider", safe(provider)).tag("model", safe(model)).tag("outcome", safe(outcome))
				.publishPercentileHistogram()
				.register(registry));
	}

	// ----------------------------------------------------------------- tools

	/** Uma chamada de tool (backend/frontend/not_found). {@code millis < 0} pula o timer. */
	public void recordTool(String tool, String outcome, long millis) {
		if (registry == null)
			return;
		Counter.builder(METRIC_TOOL_CALLS).tag("tool", safe(tool)).tag("outcome", safe(outcome))
				.register(registry).increment();
		if (millis >= 0)
			Timer.builder(METRIC_TOOL_SECONDS).tag("tool", safe(tool))
					.publishPercentileHistogram()
					.register(registry).record(Duration.ofMillis(millis));
	}

	// ----------------------------------------------------------------- turno

	/** Fechamento do turno. */
	public void recordTurn(String type, String outcome, long millis, int depth) {
		if (registry == null)
			return;
		Counter.builder(METRIC_TURNS).tag("type", safe(type)).tag("outcome", safe(outcome))
				.register(registry).increment();
		Timer.builder(METRIC_TURN_SECONDS).tag("type", safe(type))
				.publishPercentileHistogram()
				.register(registry).record(Duration.ofMillis(Math.max(0, millis)));
		registry.summary(METRIC_TURN_DEPTH).record(depth);
	}

	public void recordError(String code) {
		if (registry == null)
			return;
		Counter.builder(METRIC_ERRORS).tag("code", code == null ? "UNKNOWN" : code)
				.register(registry).increment();
	}

	// ---------------------------------------------------------------- gauges

	/** Gauge do numero de conversas ativas ligado por referencia viva ao store. */
	public void bindConversationsActive(IntSupplier active) {
		if (registry != null && active != null)
			Gauge.builder(METRIC_CONVERSATIONS_ACTIVE, active, IntSupplier::getAsInt).register(registry);
	}

	/** Gauge do numero de turnos suspensos aguardando o cliente. */
	public void bindPendingTurns(IntSupplier pending) {
		if (registry != null && pending != null)
			Gauge.builder(METRIC_PENDING_TURNS, pending, IntSupplier::getAsInt).register(registry);
	}

	/** Gauge do limite configurado de conversas (para o alerta {@code active/max}). */
	public void bindConversationsMax(IntSupplier max) {
		if (registry != null && max != null)
			Gauge.builder(METRIC_CONVERSATIONS_MAX, max, IntSupplier::getAsInt).register(registry);
	}

	/** Gauge do tamanho do cache de ChatModel do adapter. */
	public void bindChatModelCacheSize(IntSupplier size) {
		if (registry != null && size != null)
			Gauge.builder(METRIC_CACHE_SIZE, size, IntSupplier::getAsInt).register(registry);
	}

	public void recordEviction(String reason) {
		recordEvictions(reason, 1);
	}

	public void recordEvictions(String reason, long count) {
		if (registry == null || count <= 0)
			return;
		Counter.builder(METRIC_EVICTIONS).tag("reason", safe(reason)).register(registry).increment(count);
	}

	public void recordChatModelCacheClear() {
		if (registry == null)
			return;
		registry.counter(METRIC_CACHE_CLEARS).increment();
	}

	// ---------------------------------------------------------------- scrape

	/** Texto no formato de exposicao do Prometheus (chamado pelo endpoint HTTP). */
	public String scrape() {
		return prometheusRegistry != null ? prometheusRegistry.scrape() : "";
	}

	/** Exposto para testes. */
	MeterRegistry registry() {
		return registry;
	}

	private static String safe(String v) {
		return (v == null || v.isBlank()) ? "unknown" : v;
	}

	private static String nodeName() {
		String n = System.getenv("TEDROS_NODE_NAME");
		if (n != null && !n.isBlank())
			return n;
		return System.getProperty("tedros.node.name", "unknown");
	}
}
