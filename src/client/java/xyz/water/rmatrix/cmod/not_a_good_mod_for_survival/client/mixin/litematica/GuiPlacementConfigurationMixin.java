package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica;

import fi.dy.masa.litematica.gui.widgets.WidgetListPlacementSubRegions;
import fi.dy.masa.litematica.gui.widgets.WidgetPlacementSubRegion;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.SchematicRenderRangeManager;

/**
 * Adds a "Reset Render Range" button to Litematica's placement configuration page,
 * enabled only while the partial render range feature is turned on.
 */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.gui.GuiPlacementConfiguration", remap = false)
public abstract class GuiPlacementConfigurationMixin extends GuiListBase<SubRegionPlacement, WidgetPlacementSubRegion, WidgetListPlacementSubRegions> {
    @Unique
    private static final String BUTTON_LABEL_KEY =
            NotAGoodModForSurvival.MOD_ID + ".gui.button.reset_projection_render_range";

    @Unique
    private static final String BUTTON_HOVER_KEY =
            NotAGoodModForSurvival.MOD_ID + ".gui.button.hover.reset_projection_render_range";


    @Shadow(remap = false) @Final
    public SchematicPlacement placement;

    protected GuiPlacementConfigurationMixin(int listX, int listY) {
        super(listX, listY);
    }

    @Inject(method = "initGui", at = @At("TAIL"), remap = false)
    private void notAGoodModForSurvival$addResetRenderRangeButton(CallbackInfo ci) {
        // The vanilla right-hand button column's last row ("Reset Sub-Regions")
        // sits at x = screenWidth - 130, y = 218; this row goes directly below it.
        int x = this.getScreenWidth() - 130;
        int y = 239;
        int width = 120;

        ButtonGeneric button = new ButtonGeneric(x, y, width, 20,
                StringUtils.translate(BUTTON_LABEL_KEY));
        button.setHoverStrings(StringUtils.translate(BUTTON_HOVER_KEY));
        button.setEnabled(RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue());

        this.addButton(button, (button1, mouseButton) -> SchematicRenderRangeManager.getInstance().resetRangeToProjectionBox(placement));
    }
}
