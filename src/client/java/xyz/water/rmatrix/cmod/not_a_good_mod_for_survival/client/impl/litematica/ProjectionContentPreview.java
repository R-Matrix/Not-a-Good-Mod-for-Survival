package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import org.jetbrains.annotations.Nullable;

import fi.dy.masa.malilib.util.GuiUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay.GameplayConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.projection.FrameContentPreviewScreen;

/**
 * Handles the projection content preview hotkey, which inspects projected content: the
 * item inside a projected item frame, and the book on a projected lectern.
 *
 * <p>Only projection data is ever read, so real item frames keep vanilla's rotate on
 * empty-hand click behaviour.
 */
public final class ProjectionContentPreview {
    private ProjectionContentPreview() {
    }

    /** Returns whether this click belongs to the projection aids and must not be used twice. */
    public static boolean handleUseClick(MinecraftClient client, boolean requireSneaking, boolean openScreens) {
        if (!GameplayConfigs.ProjectionAids.ENABLE_PROJECTION_CONTENT_PREVIEW.getBooleanValue()
                || GuiUtils.getCurrentScreen() != null) {
            return false;
        }

        if (client.player == null || client.world == null
                || (requireSneaking && !client.player.isSneaking())) {
            return false;
        }

        ItemStack mainHand = client.player.getMainHandStack();
        boolean copyTool = GameplayConfigs.ProjectionAids.ENABLE_PROJECTION_BOOK_COPY.getBooleanValue()
                && mainHand.isOf(Items.WRITABLE_BOOK);
        boolean emptyHands = mainHand.isEmpty() && client.player.getOffHandStack().isEmpty();

        if (!copyTool && !emptyHands) {
            return false;
        }

        ProjectionAimScanner.Target target = ProjectionAimScanner.findTarget(client);

        if (target == null) {
            return false;
        }

        if (!openScreens) {
            return true;
        }

        return switch (target.kind()) {
            case ITEM_FRAME -> openFramePreview(client, target.frame());
            case LECTERN -> openLecternContent(client, target, emptyHands);
        };
    }

    private static boolean openFramePreview(MinecraftClient client, @Nullable ItemFrameEntity frame) {
        if (frame == null) {
            return false;
        }

        ItemStack content = frame.getHeldItemStack();

        if (content.isEmpty()) {
            ProjectionAidMessages.print("frameNoContent");
            return true;
        }

        FrameContentPreviewScreen screen = FrameContentPreviewScreen.create(content, frame.getBlockPos(),
                frame.getType() == EntityType.GLOW_ITEM_FRAME, frame.getRotation());

        if (screen == null) {
            return false;
        }

        client.setScreen(screen);
        return true;
    }

    private static boolean openLecternContent(MinecraftClient client,
            ProjectionAimScanner.Target target, boolean emptyHands) {
        ItemStack book = target.book();

        if (book.isEmpty()) {
            if (emptyHands == false) {
                return false;
            }

            ProjectionAidMessages.print("lecternNoBook");
            return true;
        }

        if (emptyHands == false) {
            return ProjectionBookWriter.tryCopyBookText(client, book);
        }

        BookScreen.Contents contents = BookScreen.Contents.create(book);

        if (contents == null) {
            ProjectionAidMessages.print("lecternNoBook");
            return true;
        }

        client.setScreen(new BookScreen(contents));
        return true;
    }
}
