/*
 * This file contains an adaptation of the Malilib config-option label hook
 * used by TweakerMore.
 *
 * Original project: TweakerMore
 * Copyright (C) 2023 Fallen_Breath and contributors
 * Source: https://github.com/Fallen-Breath/TweakerMore
 * Original license: GNU Lesser General Public License v3.0 (LGPL-3.0-only)
 *
 * The adapted portions of this file remain available under the same license.
 * See THIRD_PARTY_NOTICES.md and LICENSES/LGPL-3.0.txt for the attribution
 * and license reference.
 */
package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global_search;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOptionBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.config.IConfigBase;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.config.ConfigAvailabilityResolver;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global_search.GlobalSearchNavigation;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.config.ConditionalConfigLabel;

/** Draws a temporary accent behind the rule reached from global search. */
@Mixin(WidgetConfigOption.class)
public abstract class WidgetConfigOptionMixin extends WidgetConfigOptionBase<ConfigOptionWrapper> {
    protected WidgetConfigOptionMixin(
            int x,
            int y,
            int width,
            int height,
            WidgetListConfigOptionsBase<?, ?> parent,
            ConfigOptionWrapper entry,
            int listIndex
    ) {
        super(x, y, width, height, parent, entry, listIndex);
    }

    @WrapOperation(
            method = "addConfigOption",
            at = @At(
                    value = "INVOKE",
                    target = "Lfi/dy/masa/malilib/gui/widgets/WidgetConfigOption;addLabel(IIIII[Ljava/lang/String;)V",
                    remap = false),
            remap = false)
    private void notAGoodModForSurvival$createConfigLabel(
            WidgetConfigOption instance, int x, int y, int width, int height, int textColor, String[] lines, Operation<Void> original
    ) {
        IConfigBase config = this.entry == null ? null : this.entry.getConfig();

        if (config != null && ConfigAvailabilityResolver.isUnavailable(config)) {
            this.addWidget(new ConditionalConfigLabel(
                    x, y, width, height, textColor, lines));
        } else {
            original.call(instance, x, y, width, height, textColor, lines);
        }
    }

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
