package com.stevennovak.llmmod.preset;

public class Preset {
    public final String name;
    public final String displayName;
    public final String repo;
    public final String file;
    public final int threads;
    public final int contextLength;
    public final int maxTokens;
    public final float temperature;
    public final String systemPrompt;

    public Preset(String name, String displayName, String repo, String file, int threads, int contextLength, int maxTokens, float temperature, String systemPrompt) {
        this.name = name;
        this.displayName = displayName;
        this.repo = repo;
        this.file = file;
        this.threads = threads;
        this.contextLength = contextLength;
        this.maxTokens = maxTokens;
        this.temperature = temperature;
        this.systemPrompt = systemPrompt;
    }
}
