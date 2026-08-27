package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.options.ConfigHotkey;
import fi.dy.masa.malilib.hotkeys.IHotkey;

import java.util.List;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.config.ConditionalConfigHotkey;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.config.ConfigAvailabilityResolver;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.ModEnvironment;

/** Project-owned malilib hotkeys. */
public final class Hotkeys {
    private static final String HOTKEYS_KEY = NotAGoodModForSurvival.MOD_ID + ".config.hotkeys";

    public static final ConfigHotkey OPEN_GUI_SETTINGS = new ConfigHotkey(
            "openGuiSettings", "H,C").apply(HOTKEYS_KEY);
    public static final ConditionalConfigHotkey EDIT_HUD = new ConditionalConfigHotkey(
            "editHudKey",
            "H,J",
            "Open the material HUD editor.",
            ModEnvironment::isLitematicaLoaded,
            ModEnvironment.LITEMATICA_MOD_ID,
            "Litematica").apply(HOTKEYS_KEY);
    public static final ConditionalConfigHotkey EDIT_PROJECTION_RENDER_RANGE = new ConditionalConfigHotkey(
            "editProjectionRenderRangeKey",
            "",
            "Edit the display range of the selected Litematica projection.",
            ModEnvironment::isLitematicaLoaded,
            ModEnvironment.LITEMATICA_MOD_ID,
            "Litematica").apply(HOTKEYS_KEY);
    public static final ConditionalConfigHotkey CYCLE_RANGE_CORNER_MODE = new ConditionalConfigHotkey(
            "cycleProjectionRangeCornerModeKey",
            "",
            "Cycle the projection range corner edit mode between corner picking and expand-to-contain.",
            ModEnvironment::isLitematicaLoaded,
            ModEnvironment.LITEMATICA_MOD_ID,
            "Litematica").apply(HOTKEYS_KEY);
    public static final ConditionalConfigHotkey RESET_PROJECTION_RENDER_RANGE = new ConditionalConfigHotkey(
            "resetProjectionRenderRangeKey",
            "",
            "Reset the display range of the selected Litematica projection to its full extent.",
            ModEnvironment::isLitematicaLoaded,
            ModEnvironment.LITEMATICA_MOD_ID,
            "Litematica").apply(HOTKEYS_KEY);

    /** All hotkeys, including options that are currently unavailable. */
    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_GUI_SETTINGS,
            EDIT_HUD,
            EDIT_PROJECTION_RENDER_RANGE,
            CYCLE_RANGE_CORNER_MODE,
            RESET_PROJECTION_RENDER_RANGE
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
