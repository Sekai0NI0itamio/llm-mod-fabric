package com.stevennovak.llmmod;

import com.stevennovak.llmmod.command.LlmCommand;
import com.stevennovak.llmmod.config.LlmConfig;
import com.stevennovak.llmmod.http.LlmHttpServer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class LlmMod implements ModInitializer {
    @Override
    public void onInitialize() {
        LlmConfig.get();
        LlmCommand.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> LlmHttpServer.start());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> LlmHttpServer.stop());
    }
}
">
path">
/Users/stevennovak/Desktop/Code Projects/GithubBasedEngineering/Mods/llm-mod-fabric/src/main/java/com/stevennovak/llmmod/LlmMod.java