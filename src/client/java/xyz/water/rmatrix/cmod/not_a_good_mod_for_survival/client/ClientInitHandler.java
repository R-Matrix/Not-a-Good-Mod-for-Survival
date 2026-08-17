package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Hotkeys;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.GuiConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.hud.MaterialHudController;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.input.InputHandler;

/** Registers all project integrations after Minecraft has finished initializing. */
public final class ClientInitHandler implements IInitializationHandler {
    @Override
    public void registerModHandlers() {
        ConfigManager.getInstance().registerConfigHandler(NotAGoodModForSurvival.MOD_ID, Configs.INSTANCE);
        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(NotAGoodModForSurvival.MOD_ID, NotAGoodModForSurvival.MOD_NAME, GuiConfigs::new));

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());
        Hotkeys.OPEN_GUI_SETTINGS.getKeybind().setCallback(new OpenSettingsCallback());
        Hotkeys.EDIT_HUD.getKeybind().setCallback(new EditHudCallback());
    }

    private static final class OpenSettingsCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            GuiBase.openGui(new GuiConfigs());
            return true;
        }
    }

    private static final class EditHudCallback implements IHotkeyCallback {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key) {
            return MaterialHudController.openEditor();
        }
    }
}
