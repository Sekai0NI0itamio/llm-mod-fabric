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

## Presets

Built-in presets (use `/llm presets` to list, `/llm preset <name>` to apply — downloads + loads):

- `qwopus-4b-coder-mtp` (default) — Qwopus3.5-4B-Coder-MTP Q4_K_M, 3 threads, 4096 ctx
- `qwen2.5-coder-7b` — Qwen2.5-Coder-7B-Instruct Q4_K_M, 2 threads, 4096 ctx
- `qwopus-9b-coder` — Qwopus3.5-9B-Coder Q4_K_M, 2 threads, 4096 ctx (needs more RAM)

Default API secret: `12y081g20808y01` (change with `/llm setapiKey`).
