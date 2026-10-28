package com.stevennovak.llmmod.inference;

import com.llama4j.Qwen35Inference;
import com.stevennovak.llmmod.config.LlmConfig;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LlamaEngine {
    private final Qwen35Inference engine = new Qwen35Inference();

    public boolean isLoaded() { return engine.isLoaded(); }
    public String getLoadedModelName() { return engine.getLoadedName(); }

    public synchronized void load(Path modelFile) throws Exception {
        engine.load(modelFile, LlmConfig.get().contextLength);
    }

    public synchronized void unload() {
        engine.unload();
    }

    public String complete(String prompt, List<Map<String, String>> messages, String systemPrompt, int maxTokens, float temperature) {
        String sys = (systemPrompt != null && !systemPrompt.isEmpty()) ? systemPrompt : LlmConfig.get().systemPrompt;
        List<Map<String, String>> msgs = messages;
        if ((msgs == null || msgs.isEmpty()) && prompt != null && !prompt.isEmpty()) {
            msgs = new java.util.ArrayList<>();
            java.util.Map<String, String> m = new java.util.HashMap<>();
            m.put("role", "user");
            m.put("content", prompt);
            msgs.add(m);
        }
        return engine.complete(sys, msgs, maxTokens, temperature);
    }

    public static String buildChatML(String systemPrompt, List<Map<String, String>> messages) {
        return "";
    }
}
