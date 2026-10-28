package com.stevennovak.llmmod.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.stevennovak.llmmod.config.LlmConfig;
import com.stevennovak.llmmod.http.LlmHttpServer;
import com.stevennovak.llmmod.inference.LlamaEngine;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.nio.file.Path;

public class LlmCommand {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
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
                    .then(CommandManager.argument("file", StringArgumentType.string())
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
                .then(CommandManager.argument("model", StringArgumentType.string())
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
        );
    }
}
">
path">
/Users/stevennovak/Desktop/Code Projects/GithubBasedEngineering/Mods/llm-mod-fabric/src/main/java/com/stevennovak/llmmod/command/LlmCommand.java