# umlpilot_mobile

A new Flutter project.

## Configuración

El asistente usa [Ollama](https://ollama.com). Crea tu `.env` a partir de `.env.sample`:

```bash
cp .env.sample .env
```

- `OLLAMA_URL`: `http://localhost:11434` (escritorio/web) o `http://10.0.2.2:11434` (emulador Android).
- `OLLAMA_MODEL`: cualquier modelo instalado en `ollama list`.

> ⚠️ `.env` debe existir para compilar: se declara como asset de la app.
