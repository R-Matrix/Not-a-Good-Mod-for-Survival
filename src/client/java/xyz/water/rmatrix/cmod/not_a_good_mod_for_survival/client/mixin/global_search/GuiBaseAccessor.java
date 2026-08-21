package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global_search;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** Exposes Malilib's screen buttons for automatic category selection after a jump. */
@Mixin(GuiBase.class)
public interface GuiBaseAccessor {
    @Accessor("buttons")
    List<ButtonBase> notAGoodModForSurvival$getButtons();
}
