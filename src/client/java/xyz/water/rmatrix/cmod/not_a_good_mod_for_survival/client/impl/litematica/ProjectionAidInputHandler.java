package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;

import net.minecraft.client.MinecraftClient;

/**
 * Captures the middle mouse click that picks a projected item frame or the framed
 * item into the current hotbar slot. Malilib delivers this before vanilla reads the
 * click, so a consumed click never also opens the creative inventory or picks a real
 * block.
 */
public final class ProjectionAidInputHandler implements IMouseInputHandler {
    private static final ProjectionAidInputHandler INSTANCE = new ProjectionAidInputHandler();

    private ProjectionAidInputHandler() {
    }

    public static ProjectionAidInputHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int eventButton, boolean eventButtonState) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!eventButtonState || client == null || client.options == null) {
            return false;
        }

        if (eventButton == 2) {
            return ProjectionFramePicker.tryPick(client);
        }

        return false;
    }
}
