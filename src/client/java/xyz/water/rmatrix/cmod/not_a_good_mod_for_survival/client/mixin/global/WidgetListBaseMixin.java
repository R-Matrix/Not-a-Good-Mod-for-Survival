package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global;

import fi.dy.masa.malilib.gui.widgets.WidgetListBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global.GlobalConfigNavigation;

/** Applies a pending global-search target after a Malilib list has populated. */
@Mixin(WidgetListBase.class)
public abstract class WidgetListBaseMixin {
    @Inject(method = "initGui", at = @At("TAIL"))
    private void notAGoodModForSurvival$applyGlobalTarget(CallbackInfo ci) {
        if ((Object) this instanceof WidgetListConfigOptions list) {
            GlobalConfigNavigation.onListInitialized(list);
        }
    }
}
