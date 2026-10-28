package com.stevennovak.llmmod.preset;

import java.util.LinkedHashMap;
import java.util.Map;

public class Presets {
    private static final Map<String, Preset> MAP = new LinkedHashMap<>();

    static {
        MAP.put("qwen3.5-9b", new Preset(
            "qwen3.5-9b",
            "Qwen3.5-9B (Q4_K_M) - Recommended for reasoning",
            "unsloth/Qwen3.5-9B-GGUF",
            "Qwen3.5-9B-Q4_K_M.gguf",
            3, 4096, 512, 0.7f,
            "You are a helpful coding assistant."
        ));
        MAP.put("qwopus-4b-coder-mtp", new Preset(
            "qwopus-4b-coder-mtp",
            "Qwopus3.5-4B-Coder-MTP (Q4_K_M) - Fast agent loops",
            "Jackrong/Qwopus3.5-4B-Coder-MTP-GGUF",
            "Qwopus3.5-4B-Coder-MTP-Q4_K_M.gguf",
            3, 4096, 512, 0.7f,
            "You are a helpful coding assistant."
        ));
        MAP.put("qwen3.5-4b", new Preset(
            "qwen3.5-4b",
            "Qwen3.5-4B (Q4_K_M) - Low RAM",
            "unsloth/Qwen3.5-4B-GGUF",
            "Qwen3.5-4B-Q4_K_M.gguf",
            3, 4096, 512, 0.7f,
            "You are a helpful coding assistant."
        ));
        MAP.put("qwen2.5-coder-7b", new Preset(
            "qwen2.5-coder-7b",
            "Qwen2.5-Coder-7B-Instruct (Q4_K_M)",
            "Qwen/Qwen2.5-Coder-7B-Instruct-GGUF",
            "qwen2.5-coder-7b-instruct-q4_k_m.gguf",
            2, 4096, 512, 0.7f,
            "You are a helpful coding assistant."
        ));
    }

    public static Preset get(String name) {
        return MAP.get(name);
    }

    public static Map<String, Preset> all() {
        return MAP;
    }

    public static String defaultName() {
        return "qwen3.5-9b";
    }
}
