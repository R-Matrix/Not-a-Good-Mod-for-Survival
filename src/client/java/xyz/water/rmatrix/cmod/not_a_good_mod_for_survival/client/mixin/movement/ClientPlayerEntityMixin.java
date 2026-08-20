package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.movement;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;

/** Applies the configured sprint state after vanilla movement checks have run. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "tickMovement", at = @At("TAIL"))
    private void notAGoodModForSurvival$forceSprintWithLowHunger(CallbackInfo info) {
        if (!Configs.Movement.MORE_AGGRESSIVE_SPRINT.getBooleanValue()) {
            return;
        }

        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        player.setSprinting(player.input.hasForwardMovement());
    }
}
