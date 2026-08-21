package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.movement;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay.GameplayConfigs;

/** Prevents vanilla client-side movement checks from cancelling forced sprinting. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "setSprinting", at = @At("HEAD"), cancellable = true)
    private void notAGoodModForSurvival$keepForcedSprint(boolean sprinting, CallbackInfo info) {
        if (sprinting || !GameplayConfigs.Movement.MORE_AGGRESSIVE_SPRINT.getBooleanValue()) {
            return;
        }

        Object entity = this;
        if (entity instanceof ClientPlayerEntity player && player.input.hasForwardMovement()) {
            info.cancel();
        }
    }
}
