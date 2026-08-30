package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.projection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookEditScreen;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.ProjectionBookWriter;

/**
 * Treats pre-filled book pages as an edit. The vanilla editor only sends its pages to
 * the server when something was typed, so without this a copied projection book would
 * be discarded when the player signs it.
 */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin {
    @Shadow
    private boolean dirty;

    @Shadow
    private String title;

    @Inject(method = "init", at = @At("HEAD"))
    private void notAGoodModForSurvival$markCopiedPagesAsEdited(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client == null || !ProjectionBookWriter.hasPendingRequest(client)) {
            return;
        }

        this.dirty = true;
        this.title = ProjectionBookWriter.consumePendingTitle();
    }
}
