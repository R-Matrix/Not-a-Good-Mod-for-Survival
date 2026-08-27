package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import fi.dy.masa.litematica.render.RenderUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.interfaces.IRenderer;
import fi.dy.masa.malilib.util.IntBoundingBox;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;

/** Draws projection display-range boxes during the pre-weather world pass. */
public final class SchematicRenderRangeRenderer implements IRenderer {
    @Override
    public void onRenderWorldPreWeather(
            Matrix4f posMatrix,
            Matrix4f projMatrix,
            net.minecraft.client.render.Frustum frustum,
            net.minecraft.client.render.Camera camera,
            net.minecraft.client.render.Fog fog,
            net.minecraft.util.profiler.Profiler profiler
    ) {
        boolean enabled = RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue();
        boolean editing = SchematicRenderRangeManager.getInstance().isEditing();
        if (!enabled && !editing) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager manager =
                fi.dy.masa.litematica.data.DataManager.getSchematicPlacementManager();
        SchematicPlacement selected = manager.getSelectedSchematicPlacement();

        for (SchematicPlacement placement : manager.getAllSchematicsPlacements()) {
            if (!placement.isEnabled() || !placement.isRenderingEnabled()) {
                continue;
            }
            if (!editing && SchematicRenderRangeManager.isFullProjectionRange(placement)) {
                continue;
            }

            IntBoundingBox range = SchematicRenderRangeManager.getWorldBox(placement);
            if (range == null) {
                continue;
            }

            float lineWidth = (float) RenderConfigs.SchematicRenderRange.OUTLINE_LINE_WIDTH.getDoubleValue();
            RenderUtils.renderAreaOutlineNoCorners(
                    new BlockPos(range.minX, range.minY, range.minZ),
                    new BlockPos(range.maxX, range.maxY, range.maxZ),
                    lineWidth,
                    RenderConfigs.SchematicRenderRange.OUTLINE_COLOR.getColor(),
                    RenderConfigs.SchematicRenderRange.OUTLINE_COLOR.getColor(),
                    RenderConfigs.SchematicRenderRange.OUTLINE_COLOR.getColor(),
                    client);

            if (placement.equals(selected)) {
                RenderUtils.renderAreaSides(
                        new BlockPos(range.minX, range.minY, range.minZ),
                        new BlockPos(range.maxX, range.maxY, range.maxZ),
                        RenderConfigs.SchematicRenderRange.SURFACE_COLOR.getColor(),
                        posMatrix,
                        client);
            }
        }
    }
}
