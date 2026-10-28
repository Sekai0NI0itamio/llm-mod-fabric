package com.stevennovak.llmmod.inference;

import com.llama4j.Qwen35;
import com.llama4j.QwenChatFormat;
import com.stevennovak.llmmod.config.LlmConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public class LlamaEngine {
    private volatile Qwen35 model;
    private volatile Qwen35.State state;
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
        int ctx = LlmConfig.get().contextLength;
        Qwen35 m = Qwen35.loadModel(modelFile, ctx);
        model = m;
        state = m.createNewState();
        loadedModelName = modelFile.getFileName().toString();
    }

    public synchronized void unload() {
        model = null;
        state = null;
        loadedModelName = "";
    }

    public String complete(String prompt, List<Map<String, String>> messages, String systemPrompt, int maxTokens, float temperature) {
        Qwen35 m = model;
        if (m == null) throw new IllegalStateException("No model loaded");
        Qwen35.State st = m.createNewState();
        QwenChatFormat chatFormat = new QwenChatFormat(m.tokenizer);
        List<Integer> promptTokens = new ArrayList<>();
        String sys = (systemPrompt != null && !systemPrompt.isEmpty()) ? systemPrompt : LlmConfig.get().systemPrompt;
        if (sys != null && !sys.isEmpty()) {
            promptTokens.addAll(chatFormat.encodeMessage("system", sys));
        }
        // Use provided messages (OpenAI format) -> encode
        if (messages != null) {
            for (Map<String, String> msg : messages) {
                String role = msg.getOrDefault("role", "user");
                String content = msg.getOrDefault("content", "");
                if ("system".equals(role)) {
                    // already handled via sys; skip duplicate
                    continue;
                }
                promptTokens.addAll(chatFormat.encodeMessage(role, content));
            }
        } else if (prompt != null && !prompt.isEmpty()) {
            promptTokens.addAll(chatFormat.encodeMessage("user", prompt));
        }
        promptTokens.addAll(chatFormat.encodeHeader("assistant", false));
        Set<Integer> stopTokens = chatFormat.getStopTokens();
        com.llama4j.Sampler sampler = Qwen35.selectSampler(m.configuration.vocabularySize, temperature, 0.95f, System.nanoTime());
        List<Integer> responseTokens = Qwen35.generateTokens(m, st, 0, promptTokens, stopTokens, maxTokens, sampler, false, false, null);
        if (!responseTokens.isEmpty() && stopTokens.contains(responseTokens.getLast())) {
            responseTokens.removeLast();
        }
        List<Integer> visible = Qwen35.visibleTokens(m.tokenizer, responseTokens, false);
        StringBuilder sb = new StringBuilder();
        sb.append(m.tokenizer.decode(visible));
        return sb.toString();
    }

    public static String buildChatML(String systemPrompt, List<Map<String, String>> messages) {
        // kept for compat; not used
        return "";
    }
}
