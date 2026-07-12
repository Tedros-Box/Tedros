-- =====================================================================
-- Seed de preços do Tool Relay — tabela TAI_PRICE
-- =====================================================================
-- Fonte (oficial, jul/2026):
--   OpenAI : https://developers.openai.com/api/docs/pricing        (tier "Standard")
--   xAI    : https://docs.x.ai/docs/models
--   Gemini : https://ai.google.dev/gemini-api/docs/pricing         (tier "Standard")
--
-- ESCOPO / PREMISSAS:
--   * Preços em USD por 1.000.000 (1M) de tokens.
--   * Tier de PROCESSAMENTO = Standard/síncrono (o relay NÃO usa Batch/Flex/Priority).
--   * PRICE_CACHE_USD_1M = leitura de CACHE (cached input read). NÃO modelamos
--     "cache writes" nem "storage/hora" (o relay usa caching implícito; cache
--     explícito do Gemini não é usado).
--   * CONTEXT_TIER: "standard" para todos; Gemini Pro adiciona "long_context"
--     (prompt > 200k tokens). Modelos sem tiering (Flash, OpenAI, Grok) têm só
--     "standard" — o PriceBook deve cair para "standard" se pedirem "long_context"
--     de um modelo sem essa linha (ver D.4 do plano).
--   * Gemini: o preço de SAÍDA já é "including thinking tokens" — confirma a regra
--     A.5 (thoughtsTokenCount cobrado à tarifa de saída).
--
-- ATENÇÃO (revisar antes de aplicar):
--   1) PROVIDER deve casar com o que providerLabel(cfg) grava (aqui: OPENAI/GROK/
--      GEMINI, maiúsculas = nomes do enum TAiProvider). Ajuste se o label diferir.
--   2) MODEL deve casar EXATAMENTE com cfg.getModel() (string enviada à API).
--      Mantenha só os modelos que você realmente usa; remova o resto.
--   3) Preços mudam — este seed é um ponto de partida datado (VERSION='2026-07').
--      A UI futura de TAI_PRICE deve criar novas linhas com EFFECTIVE_FROM nova
--      (nunca editar a linha antiga: preserva a auditoria retroativa).
-- =====================================================================

-- Opcional: limpar o seed anterior desta versão antes de reinserir
-- DELETE FROM TAI_PRICE WHERE VERSION = '2026-07';

-- ---------------------------------------------------------------------
-- OpenAI (tier Standard) — provider OPENAI, context_tier "standard"
-- ---------------------------------------------------------------------
INSERT INTO TAI_PRICE (PROVIDER, MODEL, TIER, PRICE_IN_USD_1M, PRICE_CACHE_USD_1M, PRICE_OUT_USD_1M, CURRENCY, EFFECTIVE_FROM, EFFECTIVE_TO, VERSION, ACTIVE) VALUES
 ('OPENAI', 'gpt-5.6-sol',   'standard', 5.000000, 0.500000, 30.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('OPENAI', 'gpt-5.6-terra', 'standard', 2.500000, 0.250000, 15.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('OPENAI', 'gpt-5.6-luna',  'standard', 1.000000, 0.100000,  6.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('OPENAI', 'gpt-5.5',       'standard', 5.000000, 0.500000, 30.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('OPENAI', 'gpt-5.4',       'standard', 2.500000, 0.250000, 15.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('OPENAI', 'gpt-5.4-mini',  'standard', 0.750000, 0.075000,  4.500000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('OPENAI', 'gpt-5.4-nano',  'standard', 0.200000, 0.020000,  1.250000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE);

-- ---------------------------------------------------------------------
-- xAI Grok (tier Standard) — provider GROK, context_tier "standard"
-- ---------------------------------------------------------------------
INSERT INTO TAI_PRICE (PROVIDER, MODEL, TIER, PRICE_IN_USD_1M, PRICE_CACHE_USD_1M, PRICE_OUT_USD_1M, CURRENCY, EFFECTIVE_FROM, EFFECTIVE_TO, VERSION, ACTIVE) VALUES
 ('GROK', 'grok-4.5',  'standard', 2.000000, 0.500000, 6.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('GROK', 'grok-4.3',  'standard', 1.250000, 0.200000, 2.500000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('GROK', 'grok-4.20', 'standard', 1.250000, 0.200000, 2.500000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE);

-- ---------------------------------------------------------------------
-- Google Gemini — provider GEMINI
--   Pro tem tiering por tamanho de prompt (>200k) → "standard" + "long_context".
--   Flash / Flash-Lite são flat → só "standard".
-- ---------------------------------------------------------------------
INSERT INTO TAI_PRICE (PROVIDER, MODEL, TIER, PRICE_IN_USD_1M, PRICE_CACHE_USD_1M, PRICE_OUT_USD_1M, CURRENCY, EFFECTIVE_FROM, EFFECTIVE_TO, VERSION, ACTIVE) VALUES
 ('GEMINI', 'gemini-3.1-pro-preview', 'standard',     2.000000, 0.200000, 12.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('GEMINI', 'gemini-3.1-pro-preview', 'long_context', 4.000000, 0.400000, 18.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('GEMINI', 'gemini-3.5-flash',       'standard',     1.500000, 0.150000,  9.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('GEMINI', 'gemini-3.1-flash-lite',  'standard',     0.250000, 0.025000,  1.500000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE);

-- =====================================================================
-- MODELOS ATUALMENTE CONFIGURADOS NO SISTEMA (jul/2026)
-- Mapeamento provider -> model em produção hoje (pode mudar com o tempo):
--   GROK   -> grok-4-fast-reasoning
--   GEMINI -> gemini-2.5-pro
--   OPENAI -> gpt-5.1-2025-11-13
-- Preços oficiais confirmados nas fontes de cada provedor.
-- =====================================================================
INSERT INTO TAI_PRICE (PROVIDER, MODEL, TIER, PRICE_IN_USD_1M, PRICE_CACHE_USD_1M, PRICE_OUT_USD_1M, CURRENCY, EFFECTIVE_FROM, EFFECTIVE_TO, VERSION, ACTIVE) VALUES
 ('OPENAI', 'gpt-5.1-2025-11-13',    'standard',     1.250000, 0.125000, 10.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('GROK',   'grok-4-fast-reasoning', 'standard',     0.200000, 0.050000,  0.500000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('GEMINI', 'gemini-2.5-pro',        'standard',     1.250000, 0.125000, 10.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE),
 ('GEMINI', 'gemini-2.5-pro',        'long_context', 2.500000, 0.250000, 15.000000, 'USD', TIMESTAMP '2026-07-01 00:00:00', NULL, '2026-07', TRUE);

-- =====================================================================
-- Fim do seed. Total: 7 OpenAI + 3 Grok + 4 Gemini (3.x)
--                   + 3 em uso (gpt-5.1 / grok-4-fast-reasoning / gemini-2.5-pro[std+long]) = 18 linhas.
-- =====================================================================
