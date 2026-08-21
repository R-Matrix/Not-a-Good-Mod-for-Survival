package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global_search;

import fi.dy.masa.malilib.gui.button.ButtonBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes a Malilib button's translated display text for category matching. */
@Mixin(ButtonBase.class)
public interface ButtonBaseAccessor {
    @Accessor("displayString")
    String notAGoodModForSurvival$getDisplayString();
}
