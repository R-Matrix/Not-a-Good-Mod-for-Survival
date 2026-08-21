package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.bridging;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay.GameplayConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.interaction.ForwardBridgePlacement;

/** Temporarily replaces an air miss with a valid support-face hit for forward bridging. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Unique
    private HitResult notAGoodModForSurvival$originalCrosshairTarget;
    @Unique
    private boolean notAGoodModForSurvival$bridgingTargetActive;

    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void notAGoodModForSurvival$prepareForwardBridgeTarget(CallbackInfo info) {
        this.notAGoodModForSurvival$bridgingTargetActive = false;
        this.notAGoodModForSurvival$originalCrosshairTarget = null;

        MinecraftClient client = MinecraftClient.getInstance();
        if (!GameplayConfigs.Bridging.ENABLE_FORWARD_BRIDGING.getBooleanValue()) {
            return;
        }

        BlockHitResult placementHit = ForwardBridgePlacement.findPlacementHit(client);
        if (placementHit != null) {
            this.notAGoodModForSurvival$originalCrosshairTarget = client.crosshairTarget;
            client.crosshairTarget = placementHit;
            this.notAGoodModForSurvival$bridgingTargetActive = true;
        }
    }

    @Inject(method = "doItemUse", at = @At("RETURN"))
    private void notAGoodModForSurvival$restoreForwardBridgeTarget(CallbackInfo info) {
        if (!this.notAGoodModForSurvival$bridgingTargetActive) {
            return;
        }

        MinecraftClient.getInstance().crosshairTarget = this.notAGoodModForSurvival$originalCrosshairTarget;
        this.notAGoodModForSurvival$originalCrosshairTarget = null;
        this.notAGoodModForSurvival$bridgingTargetActive = false;
    }
}
