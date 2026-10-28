package com.stevennovak.llmmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.stevennovak.llmmod.config.LlmConfig;
import com.stevennovak.llmmod.http.LlmHttpServer;
import com.stevennovak.llmmod.inference.LlamaEngine;
import com.stevennovak.llmmod.preset.Preset;
import com.stevennovak.llmmod.preset.Presets;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.nio.file.Path;

public class LlmCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("llm")
            .requires(src -> src.hasPermissionLevel(2))
            .then(CommandManager.literal("setapiKey")
                .then(CommandManager.argument("token", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String token = StringArgumentType.getString(ctx, "token");
                        LlmConfig cfg = LlmConfig.get();
                        cfg.apiKey = token;
                        LlmConfig.save(cfg);
                        ctx.getSource().sendFeedback(() -> Text.literal("API key set."), false);
                        return 1;
                    })))
            .then(CommandManager.literal("download")
                .then(CommandManager.argument("repo", StringArgumentType.string())
                    .then(CommandManager.argument("file", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String repo = StringArgumentType.getString(ctx, "repo");
                            String file = StringArgumentType.getString(ctx, "file");
                            ctx.getSource().sendFeedback(() -> Text.literal("Downloading " + file + " from " + repo + "..."), false);
                            net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
                            new Thread(() -> {
                                try {
                                    Path dest = ModelDownloader.download(repo, file);
                                    server.execute(() -> ctx.getSource().sendFeedback(() -> Text.literal("Downloaded to " + dest), false));
                                } catch (Exception e) {
                                    server.execute(() -> ctx.getSource().sendError(Text.literal("Download failed: " + e.getMessage())));
                                }
                            }, "llm-download").start();
                            return 1;
                        }))))
            .then(CommandManager.literal("load")
                .then(CommandManager.argument("model", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "model");
                        ctx.getSource().sendFeedback(() -> Text.literal("Loading model " + name + "..."), false);
                        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
                        new Thread(() -> {
                            try {
                                Path p = LlmConfig.getModelsDir().resolve(name);
                                LlmHttpServer.getEngine().load(p);
                                LlmConfig cfg = LlmConfig.get();
                                cfg.modelName = name;
                                LlmConfig.save(cfg);
                                server.execute(() -> ctx.getSource().sendFeedback(() -> Text.literal("Model loaded: " + name), false));
                            } catch (Exception e) {
                                server.execute(() -> ctx.getSource().sendError(Text.literal("Load failed: " + e.getMessage())));
                            }
                        }, "llm-load").start();
                        return 1;
                    })))
            .then(CommandManager.literal("status")
                .executes(ctx -> {
                    LlamaEngine eng = LlmHttpServer.getEngine();
                    String msg = "Loaded: " + (eng.isLoaded() ? eng.getLoadedModelName() : "none") +
                            "\nContext: " + LlmConfig.get().contextLength +
                            "\nThreads: " + LlmConfig.get().threads +
                            "\nGPU layers: " + LlmConfig.get().gpuLayers +
                            "\nPort: " + LlmConfig.get().port;
                    ctx.getSource().sendFeedback(() -> Text.literal(msg), false);
                    return 1;
                }))
            .then(CommandManager.literal("stop")
                .executes(ctx -> {
                    LlmHttpServer.getEngine().unload();
                    ctx.getSource().sendFeedback(() -> Text.literal("Model unloaded."), false);
                    return 1;
                }))
            .then(CommandManager.literal("presets")
                .executes(ctx -> {
                    StringBuilder sb = new StringBuilder("Available presets:\n");
                    for (Preset p : Presets.all().values()) {
                        sb.append(" - ").append(p.name).append(" : ").append(p.displayName).append("\n");
                    }
                    sb.append("Use /llm preset <name> to apply.");
                    ctx.getSource().sendFeedback(() -> Text.literal(sb.toString()), false);
                    return 1;
                }))
            .then(CommandManager.literal("preset")
                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name").trim();
                        Preset p = Presets.get(name);
                        if (p == null) {
                            ctx.getSource().sendError(Text.literal("Unknown preset: " + name));
                            return 0;
                        }
                        LlmConfig cfg = LlmConfig.get();
                        cfg.preset = p.name;
                        cfg.modelName = p.file;
                        cfg.systemPrompt = p.systemPrompt;
                        cfg.threads = p.threads;
                        cfg.contextLength = p.contextLength;
                        cfg.maxTokens = p.maxTokens;
                        cfg.temperature = p.temperature;
                        LlmConfig.save(cfg);
                        ctx.getSource().sendFeedback(() -> Text.literal("Preset applied: " + p.displayName), false);
                        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
                        new Thread(() -> {
                            try {
                                Path dest = LlmConfig.getModelsDir().resolve(p.file);
                                if (!java.nio.file.Files.exists(dest)) {
                                    server.execute(() -> ctx.getSource().sendFeedback(() -> Text.literal("Downloading " + p.file + "..."), false));
                                    ModelDownloader.download(p.repo, p.file);
                                }
                                server.execute(() -> ctx.getSource().sendFeedback(() -> Text.literal("Loading model..."), false));
                                LlmHttpServer.getEngine().load(dest);
                                server.execute(() -> ctx.getSource().sendFeedback(() -> Text.literal("Model loaded: " + p.file), false));
                            } catch (Exception e) {
                                server.execute(() -> ctx.getSource().sendError(Text.literal("Preset load failed: " + e.getMessage())));
                            }
                        }, "llm-preset").start();
                        return 1;
                    })))
            .then(CommandManager.literal("chat")
                .then(CommandManager.argument("prompt", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String prompt = StringArgumentType.getString(ctx, "prompt").trim();
                        ctx.getSource().sendFeedback(() -> Text.literal("Generating..."), false);
                        net.minecraft.server.MinecraftServer server = ctx.getSource().getServer();
                        new Thread(() -> {
                            try {
                                String reply = LlmHttpServer.getEngine().complete(prompt, null, LlmConfig.get().systemPrompt, LlmConfig.get().maxTokens, LlmConfig.get().temperature);
                                server.execute(() -> ctx.getSource().sendFeedback(() -> Text.literal("AI: " + reply), false));
                            } catch (Exception e) {
                                server.execute(() -> ctx.getSource().sendError(Text.literal("Generation failed: " + e.getMessage())));
                            }
                        }, "llm-chat").start();
                        return 1;
                    })))
        );
    }
}
