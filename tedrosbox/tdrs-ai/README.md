# tdrs-ai — Tedros AI Tool Relay (Fase 1)

EAR isolado com o backend do **Tool Relay**: o servidor orquestra o loop de
tool calling do langchain4j; tools de backend executam inline e tools de
frontend são devolvidas ao cliente como `CLIENT_TOOL_CALLS` (protocolo
detalhado em `../TOOL_RELAY_IMPLEMENTATION_GUIDE.md`).

## Ativação

1. `sys.ai.enabled=true` e `sys.ai.provider` (`OPENAI` | `GROK` | `GEMINI`) com
   `sys.<provider>.key/model/prompt` configurados (properties do core).
2. `sys.ai.toolrelay.enabled=true` — switch lido pelo FE (`TerosSetting`);
   com `false` (default) o chatbot usa o caminho atual, inalterado.

Properties do módulo (seed automático no boot via `AppStartupService`):

| Chave | Default | Uso |
|---|---|---|
| `sys.ai.toolrelay.enabled` | `false` | Ativa o modo relay no cliente |
| `sys.ai.toolrelay.conversation.ttl.min` | `30` | Eviction de conversas inativas |
| `sys.ai.toolrelay.pendingturn.ttl.min` | `5` | Timeout de turno pendente |
| `sys.ai.toolrelay.max.conversations` | `2000` | Cap global de conversas em memória |
| `sys.ai.toolrelay.debug` | `false` | Liga logs de request/response do LLM |

> **Atenção:** a coluna `key` da tabela de properties (`TPropertie`) é
> `VARCHAR(20)` no DDL gerado; as chaves acima têm 24–41 caracteres. Em bancos
> criados pelo DDL do EclipseLink o seed falhará até a coluna ser alargada
> (ex.: `ALTER TABLE tedros_core.propertie ALTER COLUMN "key" TYPE VARCHAR(64)`).

## Performance / tuning do pool (TomEE)

Cada turno bloqueia uma thread do pool `@Stateless` por até 120s (timeout do
ChatModel) enquanto o LLM responde — o pool de EJB é o limite de turnos
simultâneos. No TomEE o pool é configurado por bean em `conf/system.properties`
(ou `resources.xml` do EAR):

```properties
# TAiToolRelayService e o controller: dimensionar para o pico de turnos simultâneos
ITAiToolRelayController.MaxSize = 100
TAiToolRelayService.MaxSize = 100
# fila em vez de rejeição quando o pool esgota (0 = espera indefinida)
ITAiToolRelayController.AccessTimeout = 30 seconds
```

Referência: `openejb.xml`/`system.properties` — propriedades `MaxSize`,
`MinSize`, `AccessTimeout` do `Stateless Container`. O default do TomEE é
`MaxSize=10`, insuficiente para centenas de usuários: dimensione
`MaxSize ≈ turnos simultâneos esperados` e monitore.

O cliente EJB remoto do FE (`http://host:8080/tomee/ejb`) precisa de read
timeout maior que o turno mais longo (>120s) — validar com uma chamada longa
real (item de aceite da seção 11 do guia).

## Memória e clustering

Conversas vivem em memória (`TAiConversationStore`, `ConcurrentHashMap` com
TTL/LRU) — em cluster exige sticky session por conversa. Implementação
distribuída (Redis/Mongo, padrão `SessionCacheProvider`) fica para a Fase 2,
atrás da interface `ITAiConversationStore`.
