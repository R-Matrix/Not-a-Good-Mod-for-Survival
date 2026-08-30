package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.malilib.util.Color4f;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay.GameplayConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.ModEnvironment;

/**
 * Highlights the inventory slots holding the item a projected item frame contains,
 * while a container screen is open and the crosshair is aiming at that frame.
 *
 * <p>Every Litematica reference lives here; when Litematica is absent the highlight is
 * skipped silently by the LinkageError fallback, so the container mixin that calls
 * this stays resident without a hard dependency.
 */
public final class ProjectionContentHighlightRenderer {
    private ProjectionContentHighlightRenderer() {
    }

    /** Draws the Litematica pick-block highlight for the aimed projected frame content. */
    public static void render(MinecraftClient client, HandledScreen<?> gui) {
        if (!GameplayConfigs.ProjectionAids.ENABLE_PROJECTION_CONTENT_HIGHLIGHT.getBooleanValue()
                || !ModEnvironment.isLitematicaLoaded()
                || client == null || gui == null) {
            return;
        }

        try {
            ProjectionAimScanner.Target target = ProjectionAimScanner.findTarget(client);

            if (target == null || target.kind() != ProjectionAimScanner.Kind.ITEM_FRAME) {
                return;
            }

            ItemFrameEntity frame = target.frame();

            if (frame == null) {
                return;
            }

            ItemStack content = frame.getHeldItemStack();

            if (content.isEmpty()) {
                return;
            }

            Color4f color = Configs.Colors.HIGHTLIGHT_BLOCK_IN_INV_COLOR.getColor();
            MaterialListHudRenderer.highlightSlotsWithItem(content, gui, color, client);
        } catch (LinkageError | RuntimeException exception) {
            // Litematica is absent or its API changed; the highlight is best-effort.
        }
    }
}
