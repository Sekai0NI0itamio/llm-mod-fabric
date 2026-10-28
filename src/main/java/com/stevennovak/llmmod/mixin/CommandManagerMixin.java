package com.stevennovak.llmmod.mixin;

import com.stevennovak.llmmod.command.LlmCommand;

import net.minecraft.server.command.CommandManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void llmmod$registerCommands(CommandManager.RegistrationEnvironment environment, net.minecraft.command.CommandRegistryAccess registryAccess, CallbackInfo ci) {
        LlmCommand.register(((CommandManager) (Object) this).dispatcher);
    }
}