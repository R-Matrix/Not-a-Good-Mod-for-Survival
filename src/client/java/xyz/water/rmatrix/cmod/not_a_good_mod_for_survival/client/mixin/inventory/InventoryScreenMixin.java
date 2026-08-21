package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;

/** Prevents F3+B hitboxes from being drawn around the player model in the inventory screen. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Unique
    private boolean notAGoodModForSurvival$previousRenderHitboxes;

    @Unique
    private boolean notAGoodModForSurvival$hitboxesSuppressed;

    @Inject(method = "drawBackground", at = @At("HEAD"))
    private void notAGoodModForSurvival$disablePlayerModelHitbox(
            DrawContext context,
            float delta,
            int mouseX,
            int mouseY,
            CallbackInfo info
    ) {
        this.notAGoodModForSurvival$hitboxesSuppressed = false;
        if (!RenderConfigs.DebugRender.HIDE_INVENTORY_PLAYER_MODEL_HITBOX.getBooleanValue()) {
            return;
        }

        EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
        this.notAGoodModForSurvival$previousRenderHitboxes = dispatcher.shouldRenderHitboxes();
        if (this.notAGoodModForSurvival$previousRenderHitboxes) {
            dispatcher.setRenderHitboxes(false);
            this.notAGoodModForSurvival$hitboxesSuppressed = true;
        }
    }

    @Inject(method = "drawBackground", at = @At("RETURN"))
    private void notAGoodModForSurvival$restoreHitboxRendering(
            DrawContext context,
            float delta,
            int mouseX,
            int mouseY,
            CallbackInfo info
    ) {
        if (this.notAGoodModForSurvival$hitboxesSuppressed) {
            MinecraftClient.getInstance().getEntityRenderDispatcher().setRenderHitboxes(true);
            this.notAGoodModForSurvival$hitboxesSuppressed = false;
        }
    }
}
