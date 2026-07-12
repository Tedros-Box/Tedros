package org.tedros.ai.observability.pricing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.tedros.server.util.TLoggerUtil;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;

/**
 * Popula {@code TAI_PRICE} no primeiro boot, a partir do recurso de classpath
 * {@code TAI_PRICE_seed.sql} (mesma pasta da entidade). Idempotente: so insere
 * quando a tabela esta vazia, portanto e seguro em restarts e em cluster (o
 * primeiro no ganha; os demais veem {@code count > 0}).
 * <p>
 * Roda apos o DDL-generation criar a tabela (a consulta {@code count()} forca a
 * inicializacao da PU). Nunca quebra o boot: qualquer falha e apenas logada.
 *
 * @author Davis Gordon
 */
@Startup
@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class TAiPriceSeed {

	private static final TLoggerUtil LOGGER = TLoggerUtil.create(TAiPriceSeed.class);

	/** Recurso no mesmo pacote da entidade {@code TAiPrice}. */
	private static final String SEED_RESOURCE = "/org/tedros/ai/observability/pricing/entity/TAI_PRICE_seed.sql";

	@EJB
	TAiPriceEAO priceEAO;

	@PostConstruct
	void seed() {
		try {
			long existing = priceEAO.count();
			if (existing > 0) {
				LOGGER.info("TAI_PRICE already has {} rows — skipping seed", existing);
				return;
			}
			List<String> statements = loadStatements();
			if (statements.isEmpty()) {
				LOGGER.warn("TAI_PRICE seed resource empty or not found ({})", SEED_RESOURCE);
				return;
			}
			int inserted = priceEAO.insertSeed(statements);
			LOGGER.info("TAI_PRICE seeded on first boot: {} rows from {} statements",
					inserted, statements.size());
		} catch (Exception e) {
			// seed nunca pode quebrar o boot do modulo de IA
			LOGGER.error("Failed to seed TAI_PRICE (prices will be missing until seeded)", e);
		}
	}

	/**
	 * Le o .sql, remove comentarios de linha ({@code --}) e devolve os statements
	 * (separados por {@code ;}), sem o {@code ;}. So statements que inserem em
	 * TAI_PRICE sobrevivem (o restante — DELETE comentado etc. — vira vazio).
	 */
	private List<String> loadStatements() throws IOException {
		List<String> statements = new ArrayList<>();
		try (InputStream in = getClass().getResourceAsStream(SEED_RESOURCE)) {
			if (in == null)
				return statements;
			StringBuilder buffer = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					int comment = line.indexOf("--");
					if (comment >= 0)
						line = line.substring(0, comment);
					buffer.append(line).append('\n');
				}
			}
			for (String raw : buffer.toString().split(";")) {
				String stmt = raw.trim();
				if (!stmt.isEmpty())
					statements.add(stmt);
			}
		}
		return statements;
	}
}
