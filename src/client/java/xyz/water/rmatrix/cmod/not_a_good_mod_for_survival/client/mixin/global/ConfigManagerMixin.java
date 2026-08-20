package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.IConfigHandler;

/** Exposes Malilib's registered config handlers to the global config index. */
@Mixin(ConfigManager.class)
public interface ConfigManagerMixin {
    @Accessor("configHandlers")
    Map<String, IConfigHandler> notAGoodModForSurvival$getConfigHandlers();
}
