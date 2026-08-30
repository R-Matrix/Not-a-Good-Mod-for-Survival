package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import org.jetbrains.annotations.Nullable;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.config.Hotkeys;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.minecraft.util.math.BlockPos;

/**
 * Decides whether a projected item frame may be easy placed, mirroring Litematica's
 * own block easy place semantics.
 *
 * <p>Litematica's easy place only acts on schematic content its generic trace still
 * sees: schematic rendering must be on (unless the invert hotkey is held), the target
 * must lie inside the render layer range, and the schematic chunk must be loaded. The
 * frame gate applies the same three tests to the frame's own cell and adds Litematica's
 * entity rendering toggle, because a frame whose entity rendering is off is not on
 * screen and must not be built through. The cell is the block the frame hangs in,
 * which is its support block offset by the support face, exactly the cell the aiming
 * code already resolves.
 */
public final class ProjectionEasyPlaceGate {
    private ProjectionEasyPlaceGate() {
    }

    /** Returns whether Litematica can be showing the projected frame in {@code cell} right now. */
    public static boolean isFramePlaceable(@Nullable WorldSchematic schematicWorld, BlockPos cell) {
        if (schematicWorld == null || cell == null) {
            return false;
        }

        boolean invert = Hotkeys.INVERT_GHOST_BLOCK_RENDER_STATE.getKeybind().isKeybindHeld();

        if (!Configs.Visuals.RENDER_SCHEMATIC_ENTITIES.getBooleanValue()
                || Configs.Visuals.ENABLE_SCHEMATIC_RENDERING.getBooleanValue() == invert) {
            return false;
        }

        if (!DataManager.getRenderLayerRange().isPositionWithinRange(cell)) {
            return false;
        }

        return schematicWorld.getChunkProvider().getChunkIfExists(cell.getX() >> 4, cell.getZ() >> 4) != null;
    }
}
