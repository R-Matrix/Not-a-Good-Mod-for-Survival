package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkey;

import java.util.List;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.config.ConfigAvailabilityResolver;

/** Project-owned malilib hotkeys. */
public final class Hotkeys {
    private static final String HOTKEYS_KEY = NotAGoodModForSurvival.MOD_ID + ".config.hotkeys";

    public static final ConfigHotkey OPEN_GUI_SETTINGS = new ConfigHotkey(
            "openGuiSettings", "H,C").apply(HOTKEYS_KEY);
    /** All hotkeys, including options that are currently unavailable. */
    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_GUI_SETTINGS
    );

    /** Hotkeys that can actually be registered in the current environment. */
    public static List<ConfigHotkey> getAvailableHotkeys() {
        return HOTKEY_LIST.stream()
                .filter(config -> ConfigAvailabilityResolver.isAvailable(config))
                .toList();
    }

    private Hotkeys() {
    }
}
