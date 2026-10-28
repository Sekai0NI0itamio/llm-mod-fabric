package com.stevennovak.llmmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class LlmConfig {
    public String apiKey = "";
    public String modelName = "";
    public String systemPrompt = "You are a helpful coding assistant.";
    public int gpuLayers = 0;
    public int threads = 3;
    public int contextLength = 4096;
    public int port = 8080;
    public int maxTokens = 512;
    public float temperature = 0.7f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path configPath;
    private static LlmConfig instance;

    public static LlmConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("llm-mod");
    }

    public static Path getModelsDir() {
        return getConfigDir().resolve("models");
    }

    private static LlmConfig load() {
        try {
            configPath = getConfigDir().resolve("config.json");
            Files.createDirectories(getConfigDir());
            Files.createDirectories(getModelsDir());
            if (Files.exists(configPath)) {
                try (Reader r = Files.newBufferedReader(configPath)) {
                    LlmConfig c = GSON.fromJson(r, LlmConfig.class);
                    if (c == null) c = new LlmConfig();
                    instance = c;
                    return c;
                }
            }
        } catch (Exception e) {
            System.err.println("[LLMMod] Failed to load config: " + e.getMessage());
        }
        LlmConfig c = new LlmConfig();
        instance = c;
        save(c);
        return c;
    }

    public static void save(LlmConfig c) {
        try {
            if (configPath == null) {
                configPath = getConfigDir().resolve("config.json");
                Files.createDirectories(getConfigDir());
            }
            try (Writer w = Files.newBufferedWriter(configPath)) {
                GSON.toJson(c, w);
            }
            instance = c;
        } catch (IOException e) {
            System.err.println("[LLMMod] Failed to save config: " + e.getMessage());
        }
    }

    public static void reload() {
        instance = null;
        get();
    }
}
">
path">
/Users/stevennovak/Desktop/Code Projects/GithubBasedEngineering/Mods/llm-mod-fabric/src/main/java/com/stevennovak/llmmod/config/LlmConfig.java