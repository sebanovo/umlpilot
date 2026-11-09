# Rotación de proveedores y modelos de IA (asistente)

Este documento describe cómo el asistente de IA del editor elige y **rota automáticamente**
entre proveedores (DeepSeek, OpenRouter, Google Gemini), API keys y modelos cuando uno falla, se
satura o agota su cuota. Aplica al chat con *function calling* y a la construcción del
diagrama desde una imagen.

## 1. Resumen

- Cada posible combinación **(proveedor, api key, modelo)** se registra como una *entrada*
  independiente.
- En cada petición se recorre la lista de entradas desde un **cursor** (la última que
  funcionó) y se usa la primera que **no esté en enfriamiento** (*cooldown*).
- Si una entrada falla, se **enfría** (por un tiempo) y se pasa a la siguiente.
- El enfriamiento es **por combinación** (key+modelo) para límites de cuota/rate-limit, y
  **por proveedor completo** cuando se agotan los créditos de la cuenta (p. ej. 402).
- El flujo de imagen usa un **registro distinto** (solo modelos con visión).

## 2. Componentes

| Componente | Ruta | Responsabilidad |
|---|---|---|
| `AiChatConfiguration` | `infrastructure/config/AiChatConfiguration.java` | Construye los modelos y los dos registros (chat y visión) |
| `AiChatProviderRegistry` | `infrastructure/ai/AiChatProviderRegistry.java` | Guarda las entradas, el cursor y los cooldowns |
| `AiChatService` | `infrastructure/ai/AiChatService.java` | Ejecuta el bucle de rotación y las tools |
| `QuotaErrors` | `infrastructure/ai/QuotaErrors.java` | Clasifica los errores (cuota / créditos / puntual) |
| `AiProperties` | `infrastructure/config/AiProperties.java` | Config `app.ai.*` (keys, modelos, cooldowns) |
| `AiChatProperties` | `infrastructure/config/AiChatProperties.java` | Config `app.ai.chat.*` (proveedor, URLs, límites) |

> El antiguo `AiOrchestrator` (usado por `/api/v1/ai/chat` para generación de código) es
> un sistema aparte; también soporta DeepSeek mediante `DeepSeekProvider`.

## 3. Construcción del registro

Al arrancar, `AiChatConfiguration` construye **dos** registros:

1. `aiChatProviderRegistry` → chat de texto.
2. `aiChatVisionRegistry` → construir desde imagen (solo modelos con visión).

Para cada proveedor en el orden efectivo (ver §4):

```text
para cada providerId en providerOrder:
    config   = app.ai.<providerId>            # deepseek | openrouter | gemini
    keys     = config.api-keys                # lista separada por comas (se filtran vacíos)
    models   = modoVisión ? visionModelsFor(providerId, config)
                          : config.models
    baseUrl  = providerId == "gemini"   ? chat.gemini-base-url
             : providerId == "deepseek" ? chat.deepseek-base-url
                                        : chat.openrouter-base-url

    para cada key en keys:
        keyRef = hex(key.hashCode())
        para cada model en models:
            registro += Entrada(
                id = providerId,
                modelName = model,
                keyRef = keyRef,
                model = OpenAiChatModel(baseUrl, key, model),
                quotaCooldownSeconds = config.quota-cooldown-seconds,
                errorCooldownSeconds = config.error-cooldown-seconds
            )
```

`visionModelsFor(providerId, config)`:

- Si `config.vision-models` tiene valores → se usan esos.
- Si no, y el proveedor es **gemini** → se usan `config.models` (todos aceptan imagen).
- Si no, y el proveedor es **openrouter** o **deepseek** -> **ninguno** (solo los declarados en `vision-models` aceptan imagen).
  imagen; así se evita el error `No endpoints found that support image input`).

### 3.1 Modelo por entrada

Cada entrada tiene su propio `OpenAiChatModel` creado con
`OpenAiSetup.setupSyncClient(baseUrl, apiKey, ...)` y opciones:

- `model` = nombre del modelo.
- `ToolCallingManager` con `maxTotalToolCalls = app.ai.chat.max-tool-calls` y
  `onLimitExceeded = RETURN_ERROR_RESPONSE` (cota del bucle de tool calling; no rompe la
  conversación, devuelve el error al modelo).
- `ObservationRegistry.NOOP`.

Las autoconfiguraciones de OpenAI están deshabilitadas con `spring.ai.model.*: none`, por
lo que estas son las únicas instancias de modelo.

## 4. Orden de proveedores

El orden efectivo lo define `app.ai.provider-order` (`AI_PROVIDER_ORDER`, por defecto
`openrouter,gemini,deepseek`), sin duplicados. El primer proveedor configurado con keys y
modelos es el que se usa inicialmente; el cursor recuerda el último que funcionó.

Dentro de cada proveedor, el orden es **key1xmodelo1, key1xmodelo2, ..., key2xmodelo1, ...**.

## 5. Algoritmo de rotación

`AiChatService.execute(...)` (compartido por chat e imagen):

```text
inicio = registry.cursor()          # índice de la última entrada que funcionó
para i = 0 .. n-1 (n = nº de entradas del registro):
    idx = (inicio + i) mod n

    si registry.isCoolingDown(entrada[idx].cooldownKey):
        continuar                    # en enfriamiento, se salta

    intentar:
        respuesta = chatClient(entrada[idx]).prompt()...call().content()
        registry.moveCursorTo(idx)   # el cursor se queda aquí
        devolver respuesta

    capturar error:
        clasificar y enfriar (ver §6)
        registrar el error en la lista

si ninguna tuvo éxito:
    lanzar LlmException("Ningún proveedor de IA disponible: <detalles>" | "todos en enfriamiento (Ns)")
    # el controlador lo traduce a HTTP 503
```

Características:

- **Round-robin con memoria**: cada petición empieza por la entrada que funcionó la última
  vez (cursor); no siempre por el primero de la lista.
- **Salta enfriados** sin intentar.
- Si **todas** las entradas están en enfriamiento, no se llama a ningún proveedor y se
  devuelve 503 indicando el enfriamiento restante.

## 6. Clasificación de errores y cooldowns

`QuotaErrors` inspecciona el mensaje del error y toda su cadena de causas (en minúsculas):

| Categoría | Detección (subcadena) | Alcance del enfriamiento | Duración |
|---|---|---|---|
| Créditos de cuenta agotados | `insufficient credit`, `insufficient_quota`, `no credits`, `402` | **Todo el proveedor** (todas sus entradas) | `quota-cooldown-seconds` |
| Cuota / límite de uso | `quota`, `rate limit`, `too many requests`, `429` (+ los anteriores) | **Solo la entrada** (key+modelo) | `quota-cooldown-seconds` |
| Fallo puntual | cualquier otro (`503` *high demand*, `404` modelo no disponible, `Error reading response`, …) | **Solo la entrada** | `error-cooldown-seconds` |

Observaciones:

- **429 de Gemini** es cuota *por modelo* → enfría solo esa combinación; el siguiente
  modelo del mismo proveedor sigue disponible.
- **503 “high demand”** es transitorio → cooldown corto solo de ese modelo; no desactiva
  todo el proveedor.
- **402 de OpenRouter** (créditos de la cuenta) → enfría todo OpenRouter y rota a Gemini.
- Los cooldowns son **en memoria y por instancia** de la aplicación; se reinician al
  reiniciar el backend.

Los segundos se toman de `app.ai.deepseek.*` / `app.ai.openrouter.*` / `app.ai.gemini.*`:
`quota-cooldown-seconds` (`AI_QUOTA_COOLDOWN_SECONDS`, por defecto 300) y
`error-cooldown-seconds` (`AI_ERROR_COOLDOWN_SECONDS`, por defecto 30).

## 7. Configuración (variables de entorno)

| Variable | Propiedad | Por defecto | Descripción |
|---|---|---|---|
| `AI_PROVIDER_ORDER` | `app.ai.provider-order` | `openrouter,gemini,deepseek` | Orden base de proveedores |
| `OPENROUTER_API_KEYS` | `app.ai.openrouter.api-keys` | — | Keys de OpenRouter (coma). Rota entre ellas |
| `OPENROUTER_MODELS` | `app.ai.openrouter.models` | — | Modelos de texto (coma) |
| `OPENROUTER_VISION_MODELS` | `app.ai.openrouter.vision-models` | — | Modelos con imagen+tools (coma) |
| `GEMINI_API_KEYS` | `app.ai.gemini.api-keys` | — | Keys de Gemini (coma). Rota entre ellas |
| `GEMINI_MODELS` | `app.ai.gemini.models` | — | Modelos de texto e imagen (coma) |
| `GEMINI_VISION_MODELS` | `app.ai.gemini.vision-models` | — | Si vacío, usa `GEMINI_MODELS` |
| `DEEPSEEK_API_KEY` | `app.ai.deepseek.api-keys` | - | Keys de DeepSeek (coma). Rota entre ellas |
| `DEEPSEEK_MODELS` | `app.ai.deepseek.models` | - | Modelos de texto e imagen (coma) |
| `DEEPSEEK_VISION_MODELS` | `app.ai.deepseek.vision-models` | - | Modelos con imagen+tools (coma); si vacio no se usa para vision |
| `DEEPSEEK_BASE_URL` | `app.ai.deepseek.base-url` / `app.ai.chat.deepseek-base-url` | `https://api.deepseek.com` | Endpoint OpenAI-compatible de DeepSeek (chat y orquestador) |
| `OPENROUTER_BASE_URL` | `app.ai.chat.openrouter-base-url` | `https://openrouter.ai/api/v1` | Endpoint OpenAI-compatible de OpenRouter |
| `GEMINI_BASE_URL` | `app.ai.chat.gemini-base-url` | `https://generativelanguage.googleapis.com/v1beta/openai` | Endpoint OpenAI-compatible de Gemini |
| `AI_CHAT_MAX_TOOL_CALLS` | `app.ai.chat.max-tool-calls` | `12` | Cota de llamadas a tools por turno |
| `AI_CHAT_TIMEOUT_SECONDS` | `app.ai.chat.timeout-seconds` | `60` | Timeout por llamada |
| `AI_QUOTA_COOLDOWN_SECONDS` | `app.ai.*.quota-cooldown-seconds` | `300` | Cooldown por cuota/créditos |
| `AI_ERROR_COOLDOWN_SECONDS` | `app.ai.*.error-cooldown-seconds` | `30` | Cooldown por fallo puntual |

Keys/modelos son listas separadas por comas; los valores vacíos se filtran. Si un
proveedor no tiene keys **o** no tiene modelos, no se generan entradas para él.

## 8. Diagrama de flujo

```mermaid
flowchart TD
    A[Petición de chat / imagen] --> B{Registro vacío?}
    B -- sí --> Z[LlmException 503: no configurado]
    B -- no --> C[inicio = cursor]
    C --> D{Recorridas todas?}
    D -- sí --> Y[LlmException 503 con errores/enfriamiento]
    D -- no --> E{Entrada en cooldown?}
    E -- sí --> D
    E -- no --> F[Llamar al modelo]
    F -- éxito --> G[cursor = índice; devolver respuesta + acciones]
    F -- error --> H{Clasificar error}
    H -- créditos cuenta --> I[Enfriar TODO el proveedor]
    H -- cuota -> J[Enfriar la entrada]
    H -- puntual --> K[Enfriar la entrada - cooldown corto]
    I --> D
    J --> D
    K --> D
```

## 9. Escenarios de ejemplo

**A. Gemini primero (segun `AI_PROVIDER_ORDER`) y saturado, OpenRouter disponible**

1. `gemini-3.8-flash` → `503 high demand` → fallo puntual → enfría esa entrada 30s.
2. `gemini-3.7-flash` → `503` → enfría 30s.
3. `gemini-3.6-flash` → `429 quota` → enfría esa entrada 300s.
4. `gemini-3.5-flash` → responde ✅. El cursor queda en `gemini-3.5-flash`.

**B. Créditos de OpenRouter agotados**

1. `openrouter [key …/modelo …]` → `402 insufficient credits` → **enfría todo OpenRouter**.
2. Rota a `gemini […/…]` → responde ✅. OpenRouter queda fuera 300s.

**C. Se agotan las keys de OpenRouter pero hay 2 keys**

- Se agota `key₁` (402 o 429) → se enfrían sus entradas.
- Se prueba `key₂×modelo₁`, … → si tiene cuota, responde ✅ (la rotación multiplica la
  cuota disponible por el número de keys).

**D. Construir desde imagen**

- Usa **solo** el registro de visión.
- Si `GEMINI_MODELS` están saturados y `OPENROUTER_VISION_MODELS` está vacío → 503
  indicando que no hay modelos de visión disponibles.

## 10. Límites y consideraciones

- Los **cooldowns son en memoria**: por instancia y se pierden al reiniciar; no se
  comparten entre réplicas.
- El cursor y los cooldowns **no distinguen usuarios**: son globales para el proceso.
- Los modelos gratuitos (p. ej. `:free` de OpenRouter) tienen cuotas muy bajas y pueden
  devolver `429`/`503` con frecuencia; la rotación los va saltando.
- Un error HTTP `503` se devuelve al cliente cuando **ninguna** entrada pudo responder,
  con el detalle de los errores o los segundos de enfriamiento restantes.
- Las tools y el contexto (`diagramId`, `userId`, `projectId`, registro de acciones) son
  los mismos en todas las entradas; solo cambia el proveedor/modelo que responde.

## 11. Cómo extender

- **Añadir modelos**: agrega slugs separados por comas en `OPENROUTER_MODELS` /
  `GEMINI_MODELS`. Para imagen, en `OPENROUTER_VISION_MODELS` (deben soportar `image` y
  `tools`).
- **Añadir keys**: agrega keys separadas por comas en `OPENROUTER_API_KEYS` /
  `GEMINI_API_KEYS`. Se crean entradas por cada key.
- **Cambiar prioridad**: cambia el orden en `AI_PROVIDER_ORDER` y reinicia el backend.
  backend para que `dotenv` recargue `.env`.
- **Añadir otro proveedor OpenAI-compatible**: agrega un campo en `AiProperties`, un caso
  en `AiProperties.providerConfig`, su base URL en `AiChatConfiguration.buildRegistry` y
  súmalo a `AI_PROVIDER_ORDER`.
- **Ajustar cooldowns**: `AI_QUOTA_COOLDOWN_SECONDS` y `AI_ERROR_COOLDOWN_SECONDS`.

## 12. Relación con la configuración del proveedor de chat

- `spring.ai.model.*: none` desactiva la autoconfiguración de Spring AI; el modelo se crea
  manualmente en `AiChatConfiguration`.
- El chat y la imagen comparten proveedores/keys, pero **difieren en el conjunto de
  modelos** (texto vs visión) y en las **tools** disponibles:
  - Chat: todas las tools (crear, listar, actualizar, eliminar).
  - Imagen: solo lectura + creación (`BUILD_TOOLS`), nunca edita ni elimina.
