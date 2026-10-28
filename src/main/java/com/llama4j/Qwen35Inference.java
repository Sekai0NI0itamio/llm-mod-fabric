package com.llama4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Qwen35Inference {
    private Qwen35 model;
    private String loadedName = "";

    public boolean isLoaded() { return model != null; }
    public String getLoadedName() { return loadedName; }

    public synchronized void load(java.nio.file.Path path, int contextLength) throws java.io.IOException {
        unload();
        model = Qwen35.loadModel(path, contextLength);
        loadedName = path.getFileName().toString();
    }

    public synchronized void unload() {
        model = null;
        loadedName = "";
    }

    public String complete(String systemPrompt, List<Map<String, String>> messages, int maxTokens, float temperature) {
        if (model == null) throw new IllegalStateException("No model loaded");
        Qwen35 m = model;
        Qwen35.State state = m.createNewState();
        QwenChatFormat chatFormat = new QwenChatFormat(m.tokenizer);
        List<Integer> promptTokens = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            promptTokens.addAll(chatFormat.encodeMessage("system", systemPrompt));
        }
        if (messages != null) {
            for (Map<String, String> msg : messages) {
                String role = msg.getOrDefault("role", "user");
                String content = msg.getOrDefault("content", "");
                if ("system".equals(role)) continue;
                promptTokens.addAll(chatFormat.encodeMessage(role, content));
            }
        }
        promptTokens.addAll(chatFormat.encodeHeader("assistant", false));
        Set<Integer> stopTokens = chatFormat.getStopTokens();
        com.llama4j.Sampler sampler = Qwen35.selectSampler(m.configuration.vocabularySize, temperature, 0.95f, System.nanoTime());
        List<Integer> responseTokens = Qwen35.generateTokens(m, state, 0, promptTokens, stopTokens, maxTokens, sampler, false, false, null);
        if (!responseTokens.isEmpty() && stopTokens.contains(responseTokens.getLast())) {
            responseTokens.removeLast();
        }
        List<Integer> visible = Qwen35.visibleTokens(m.tokenizer, responseTokens, false);
        return m.tokenizer.decode(visible);
    }
}
