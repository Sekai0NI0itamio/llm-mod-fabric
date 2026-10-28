package com.stevennovak.llmmod;

import com.stevennovak.llmmod.command.LlmCommand;
import com.stevennovak.llmmod.config.LlmConfig;
import com.stevennovak.llmmod.http.LlmHttpServer;

import net.fabricmc.api.ModInitializer;

public class LlmMod implements ModInitializer {
    @Override
    public void onInitialize() {
        LlmConfig.get();
        LlmHttpServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(LlmHttpServer::stop));
    }
}
">
path">
/Users/stevennovak/Desktop/Code Projects/GithubBasedEngineering/Mods/llm-mod-fabric/src/main/java/com/stevennovak/llmmod/LlmMod.java