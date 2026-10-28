package com.stevennovak.llmmod.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.stevennovak.llmmod.command.LlmCommand;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public abstract class CommandManagerMixin {
    @org.spongepowered.asm.mixin.gen.Accessor("dispatcher")
    public abstract CommandDispatcher<ServerCommandSource> llmmod$getDispatcher();

    @Inject(method = "<init>", at = @At("TAIL"))
    private void llmmod$registerCommands(CommandManager.RegistrationEnvironment environment, net.minecraft.command.CommandRegistryAccess registryAccess, CallbackInfo ci) {
        LlmCommand.register(llmmod$getDispatcher());
    }
}
">
path">
/Users/stevennovak/Desktop/Code Projects/GithubBasedEngineering/Mods/llm-mod-fabric/src/main/java/com/stevennovak/llmmod/mixin/CommandManagerMixin.java