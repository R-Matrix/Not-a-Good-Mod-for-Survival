package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.projection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.ProjectionContentHighlightRenderer;

/**
 * Draws the projected item frame content highlight while a container screen is open.
 *
 * <p>The injected method only calls the projection renderer, which gates on this mod's
 * own config and Litematica presence, so this mixin stays resident even without
 * Litematica and never duplicates Litematica's own block-in-inventory highlight.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenProjectionHighlightMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void notAGoodModForSurvival$renderProjectionContentHighlight(
            DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client != null) {
            ProjectionContentHighlightRenderer.render(client, (HandledScreen<?>) (Object) this);
        }
    }
}
