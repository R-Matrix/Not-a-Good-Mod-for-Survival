package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.malilib.interfaces.IRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

import fi.dy.masa.malilib.util.IntBoundingBox;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;

/** Draws the active projection range using Litematica's established world-box renderer. */
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
        if (!RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()
                && !SchematicRenderRangeManager.getInstance().isEditing()) {
            return;
        }

        SchematicPlacement placement = fi.dy.masa.litematica.data.DataManager.getSchematicPlacementManager()
                .getSelectedSchematicPlacement();
        IntBoundingBox range = SchematicRenderRangeManager.getWorldBox(placement);
        MinecraftClient client = MinecraftClient.getInstance();
        if (range == null || client.player == null) {
            return;
        }

        BlockPos pos1 = new BlockPos(range.minX, range.minY, range.minZ);
        BlockPos pos2 = new BlockPos(range.maxX, range.maxY, range.maxZ);
        fi.dy.masa.litematica.render.RenderUtils.renderAreaSides(
                pos1,
                pos2,
                RenderConfigs.SchematicRenderRange.SURFACE_COLOR.getColor(),
                posMatrix,
                client);
        fi.dy.masa.litematica.render.RenderUtils.renderAreaOutline(
                pos1,
                pos2,
                (float) RenderConfigs.SchematicRenderRange.OUTLINE_LINE_WIDTH.getDoubleValue(),
                RenderConfigs.SchematicRenderRange.OUTLINE_COLOR.getColor(),
                RenderConfigs.SchematicRenderRange.OUTLINE_COLOR.getColor(),
                RenderConfigs.SchematicRenderRange.OUTLINE_COLOR.getColor(),
                client);
    }
}
