package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Hotkeys;

/** Registers the projection display-range integration whose implementation depends on Litematica. */
public final class LitematicaSchematicRangeIntegration {
    private static boolean registered;

    private LitematicaSchematicRangeIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(SchematicRenderRangeManager::tick);
        fi.dy.masa.malilib.event.InputEventHandler.getInputManager()
                .registerMouseInputHandler(SchematicRenderRangeManager.getInstance());
        fi.dy.masa.malilib.event.RenderEventHandler.getInstance()
                .registerWorldPreWeatherRenderer(new SchematicRenderRangeRenderer());
        fi.dy.masa.malilib.event.RenderEventHandler.getInstance()
                .registerWorldLastRenderer(new SchematicRenderRangeCornerMarkerRenderer());
        Hotkeys.EDIT_PROJECTION_RENDER_RANGE.getKeybind().setCallback(new EditProjectionRenderRangeCallback());
        Hotkeys.CYCLE_RANGE_CORNER_MODE.getKeybind().setCallback(new CycleRangeCornerModeCallback());
        Hotkeys.RESET_PROJECTION_RENDER_RANGE.getKeybind().setCallback(new ResetProjectionRenderRangeCallback());
    }

    private static final class EditProjectionRenderRangeCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            return SchematicRenderRangeManager.getInstance().toggleEditor();
        }
    }

    private static final class CycleRangeCornerModeCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            return SchematicRenderRangeManager.getInstance().cycleCornerEditMode();
        }
    }

    private static final class ResetProjectionRenderRangeCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            return SchematicRenderRangeManager.getInstance().resetSelectedRangeToProjectionBox();
        }
    }
}
