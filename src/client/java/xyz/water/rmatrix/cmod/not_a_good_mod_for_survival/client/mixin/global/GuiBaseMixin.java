package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import net.minecraft.client.MinecraftClient;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.GuiConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global.GlobalConfigNavigation;

/** Adds an explicit back affordance to source config pages opened from global search. */
@Mixin(GuiBase.class)
public abstract class GuiBaseMixin {
    @Inject(method = "onMouseClicked", at = @At("HEAD"))
    private void notAGoodModForSurvival$clearGlobalTargetOnClick(
            int mouseX, int mouseY, int mouseButton, CallbackInfoReturnable<Boolean> cir) {
        GlobalConfigNavigation.onUserAction((GuiBase) (Object) this);
    }

    @Inject(method = "onMouseScrolled", at = @At("HEAD"))
    private void notAGoodModForSurvival$clearGlobalTargetOnScroll(
            int mouseX, int mouseY, double horizontalAmount, double verticalAmount,
            CallbackInfoReturnable<Boolean> cir) {
        GlobalConfigNavigation.onUserAction((GuiBase) (Object) this);
    }

    @Inject(method = "onKeyTyped", at = @At("HEAD"))
    private void notAGoodModForSurvival$clearGlobalTargetOnKey(
            int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        GlobalConfigNavigation.onUserAction((GuiBase) (Object) this);
    }

    @Inject(method = "onCharTyped", at = @At("HEAD"))
    private void notAGoodModForSurvival$clearGlobalTargetOnChar(
            char charIn, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        GlobalConfigNavigation.onUserAction((GuiBase) (Object) this);
    }

    @Inject(method = "initGui", at = @At("TAIL"))
    private void notAGoodModForSurvival$addGlobalSearchBackButton(CallbackInfo ci) {
        GuiBase current = (GuiBase) (Object) this;

        if (!(current.getParent() instanceof GuiConfigs)) {
            return;
        }

        ButtonGeneric backButton = new ButtonGeneric(
                1, 5, 18, 18, "←",
                "not-a-good-mod-for-survival.gui.global_search.back");

        current.addButton(backButton, (ButtonBase button, int mouseButton) -> {
            if (mouseButton == 0) {
                MinecraftClient.getInstance().setScreen(current.getParent());
            }
        });
    }
}
