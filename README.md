# LLM Mod (Fabric)

Turns a Minecraft server into a self-contained, local LLM inference engine. Runs GGUF models inside the JVM via `java-llama.cpp` and exposes an OpenAI-compatible REST API (`/v1/chat/completions`).

## Commands (OP level 2+)

- `/llm setapiKey <token>` — set/rotate the API secret token.
- `/llm download <hf-repo> <file>` — download a GGUF from Hugging Face into `config/llm-mod/models/`.
- `/llm load <model_name>` — load a model into memory.
- `/llm status` — show loaded model, context size, threads, port.
- `/llm stop` — unload the model and free memory.

## Config

`config/llm-mod/config.json`:

```json
{
  "apiKey": "change-me-to-a-secret-token",
  "modelName": "",
  "systemPrompt": "You are a helpful coding assistant.",
  "gpuLayers": 0,
  "threads": 3,
  "contextLength": 4096,
  "port": 8080,
  "maxTokens": 512,
  "temperature": 0.7
}
```

## API

`POST /v1/chat/completions` with `Authorization: Bearer <token>`.

Compatible with Continue, Cline, Roo, etc. Set base URL to `http://<server>:<port>/v1`.

## Build

Built via GitHub Actions (`.github/workflows/build.yml`). Native `java-llama.cpp` libs are compiled with `-DLLAMA_CURL=ON` in the `build-native-llama` job.
