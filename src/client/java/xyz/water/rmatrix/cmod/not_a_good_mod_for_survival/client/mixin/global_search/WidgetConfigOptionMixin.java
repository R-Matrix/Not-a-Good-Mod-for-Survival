package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global_search;

import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global_search.GlobalSearchNavigation;

/** Draws a temporary accent behind the rule reached from global search. */
@Mixin(WidgetConfigOption.class)
public abstract class WidgetConfigOptionMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void notAGoodModForSurvival$renderGlobalTarget(
            int mouseX,
            int mouseY,
            boolean selected,
            DrawContext drawContext,
            CallbackInfo ci
    ) {
        WidgetConfigOption widget = (WidgetConfigOption) (Object) this;
        ConfigOptionWrapper entry = widget.getEntry();

        if (entry == null || entry.getConfig() == null ||
                !GlobalSearchNavigation.shouldHighlight(entry.getConfig())) {
            return;
        }

        int x = widget.getX();
        int y = widget.getY();
        int width = widget.getWidth();
        int height = widget.getHeight();

        drawContext.fill(x, y, x + width, y + height, 0x5030A000);
        drawContext.fill(x, y, x + 3, y + height, 0xE0D99000);
    }
}
