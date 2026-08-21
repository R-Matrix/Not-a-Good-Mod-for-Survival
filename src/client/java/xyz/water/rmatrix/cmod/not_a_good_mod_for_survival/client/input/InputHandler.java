package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.input;

import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Hotkeys;

/** Exposes this project's malilib hotkeys to the global keybind manager. */
public final class InputHandler implements IKeybindProvider {
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler() {
    }

    public static InputHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager) {
        for (IHotkey hotkey : Hotkeys.getAvailableHotkeys()) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }

        for (IHotkey hotkey : Configs.BOOLEAN_HOTKEY_LIST) {
            manager.addKeybindToMap(hotkey.getKeybind());
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager) {
        manager.addHotkeysForCategory(
                NotAGoodModForSurvival.MOD_NAME,
                NotAGoodModForSurvival.MOD_ID + ".hotkeys.category",
                Hotkeys.getAvailableHotkeys());
        manager.addHotkeysForCategory(
                NotAGoodModForSurvival.MOD_NAME,
                NotAGoodModForSurvival.MOD_ID + ".hotkeys.category.booleanConfigs",
                Configs.BOOLEAN_HOTKEY_LIST);
    }
}
