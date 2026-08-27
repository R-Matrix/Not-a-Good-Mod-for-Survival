package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.util.Color4f;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;

/**
 * Draws the editable range corners during the world-last pass so they stay visible
 * on top of terrain, mirroring Litematica's own selection-corner rendering.
 */
public final class SchematicRenderRangeCornerMarkerRenderer implements IRenderer {
    @Override
    public void onRenderWorldLast(Matrix4f posMatrix, Matrix4f projMatrix) {
        boolean enabled = RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue();
        boolean editing = SchematicRenderRangeManager.getInstance().isEditing();
        if (!enabled && !editing) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        SchematicPlacement selected =
                fi.dy.masa.litematica.data.DataManager.getSchematicPlacementManager()
                        .getSelectedSchematicPlacement();
        if (client.player == null || selected == null
                || !selected.isEnabled() || !selected.isRenderingEnabled()) {
            return;
        }

        if (!editing && SchematicRenderRangeManager.isFullProjectionRange(selected)) {
            return;
        }

        for (int cornerIndex = 0; cornerIndex <= 1; cornerIndex++) {
            BlockPos corner = SchematicRenderRangeManager.getCornerMarker(selected, cornerIndex);
            if (corner == null) {
                continue;
            }

            Color4f configuredColor = cornerIndex == 0
                    ? RenderConfigs.SchematicRenderRange.CORNER_1_COLOR.getColor()
                    : RenderConfigs.SchematicRenderRange.CORNER_2_COLOR.getColor();

            // Solid six-face cube in the per-corner configured color.
            RenderUtils.renderAreaSides(corner, corner, configuredColor, posMatrix, client);
        }
    }
}
