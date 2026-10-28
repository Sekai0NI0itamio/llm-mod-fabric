package com.stevennovak.llmmod.inference;

import com.stevennovak.llmmod.config.LlmConfig;
import de.kherud.llama.InferenceParameters;
import de.kherud.llama.LlamaIterable;
import de.kherud.llama.LlamaModel;
import de.kherud.llama.LlamaOutput;
import de.kherud.llama.ModelParameters;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LlamaEngine {
    private volatile LlamaModel model;
    private volatile String loadedModelName = "";
    private final Object lock = new Object();

    public boolean isLoaded() {
        return model != null;
    }

    public String getLoadedModelName() {
        return loadedModelName;
    }

    public synchronized void load(Path modelFile) throws Exception {
        unload();
        LlmConfig cfg = LlmConfig.get();
        ModelParameters params = new ModelParameters()
                .setModel(modelFile.toAbsolutePath().toString())
                .setThreads(cfg.threads)
                .setCtxSize(cfg.contextLength)
                .setGpuLayers(cfg.gpuLayers);
        model = new LlamaModel(params);
        loadedModelName = modelFile.getFileName().toString();
    }

    public synchronized void unload() {
        if (model != null) {
            try { model.close(); } catch (Exception ignored) {}
            model = null;
        }
        loadedModelName = "";
    }

    public String complete(String prompt, int maxTokens, float temperature) {
        LlamaModel m = model;
        if (m == null) throw new IllegalStateException("No model loaded");
        synchronized (lock) {
            InferenceParameters inf = new InferenceParameters(prompt)
                    .setNPredict(maxTokens)
                    .setTemperature(temperature)
                    .setTopP(0.95f)
                    .setTopK(40)
                    .setRepeatPenalty(1.1f);
            StringBuilder sb = new StringBuilder();
            LlamaIterable it = m.generate(inf);
            for (LlamaOutput out : it) {
                sb.append(out.text);
            }
            return sb.toString();
        }
    }

    public static String buildChatML(String systemPrompt, List<Map<String, String>> messages) {
        StringBuilder p = new StringBuilder();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            p.append("<|im_start|>system\n").append(systemPrompt).append("<|im_end|>\n");
        }
        for (Map<String, String> m : messages) {
            String role = m.getOrDefault("role", "user");
            String content = m.getOrDefault("content", "");
            p.append("<|im_start|>").append(role).append("\n").append(content).append("<|im_end|>\n");
        }
        p.append("<|im_start|>assistant\n");
        return p.toString();
    }
}
">
path">
/Users/stevennovak/Desktop/Code Projects/GithubBasedEngineering/Mods/llm-mod-fabric/src/main/java/com/stevennovak/llmmod/inference/LlamaEngine.java