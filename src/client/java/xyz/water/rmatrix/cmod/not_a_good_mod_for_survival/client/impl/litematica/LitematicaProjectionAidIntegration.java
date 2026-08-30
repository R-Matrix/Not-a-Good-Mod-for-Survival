package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Hotkeys;

/**
 * Registers the projection building aids whose implementation depends on Litematica:
 * placing projected item frames, previewing projected content and copying projected
 * book text into a writable book.
 */
public final class LitematicaProjectionAidIntegration {
    private static boolean registered;

    private LitematicaProjectionAidIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(ItemFramePlacementSequence::tick);
        InputEventHandler.getInputManager().registerMouseInputHandler(ProjectionAidInputHandler.getInstance());
        Hotkeys.PROJECTION_CONTENT_PREVIEW.getKeybind().setCallback(new ContentPreviewCallback());
    }

    private static final class ContentPreviewCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            MinecraftClient client = MinecraftClient.getInstance();

            return client != null && ProjectionContentPreview.handleUseClick(client, false, true);
        }
    }
}
