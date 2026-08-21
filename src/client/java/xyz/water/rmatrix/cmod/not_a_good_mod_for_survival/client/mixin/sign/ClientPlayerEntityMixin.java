package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.sign;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay.GameplayConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.sign.LongSignEditScreen;

/** Opens the material-independent long-sign editor when the feature is enabled. */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {
    @Inject(method = "openEditSignScreen", at = @At("HEAD"), cancellable = true)
    private void notAGoodModForSurvival$openLongSignEditor(
            SignBlockEntity sign,
            boolean front,
            CallbackInfo info
    ) {
        if (!GameplayConfigs.Signs.ENABLE_LONG_SIGN_TEXT.getBooleanValue()) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new LongSignEditScreen(sign, front, client.shouldFilterText()));
        info.cancel();
    }
}
