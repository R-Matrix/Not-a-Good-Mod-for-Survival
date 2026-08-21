package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Hotkeys;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.hud.MaterialHudController;

/** Registers the client integration whose implementation depends on Litematica. */
public final class LitematicaClientIntegration {
    private static boolean registered;

    private LitematicaClientIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(MaterialHudController::tick);
        Hotkeys.EDIT_HUD.getKeybind().setCallback(new EditHudCallback());
    }

    private static final class EditHudCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            return MaterialHudController.openEditor();
        }
    }
}
