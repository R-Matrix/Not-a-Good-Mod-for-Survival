package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.List;
import java.util.Locale;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global.ButtonBaseAccessor;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global.GuiBaseAccessor;

/** Coordinates source-screen navigation and the temporary target-row highlight. */
public final class GlobalConfigNavigation {
    private static GuiBase targetScreen;
    private static IConfigBase targetConfig;
    private static String targetConfigName;
    private static String targetCategory;
    private static boolean active;
    private static boolean targetFound;

    private GlobalConfigNavigation() {
    }

    public static synchronized void begin(
            GuiBase screen,
            GlobalConfigMetadata metadata,
            IConfigBase config
    ) {
        targetScreen = screen;
        targetConfig = config;
        targetConfigName = config.getName();
        targetCategory = metadata.getCategory();
        targetFound = false;
        active = true;
    }

    /** Called after a Malilib config list has built its visible entries. */
    public static synchronized void onListInitialized(WidgetListConfigOptions list) {
        if (!isActiveForCurrentScreen()) {
            return;
        }

        List<ConfigOptionWrapper> entries = list.getCurrentEntries();

        for (int index = 0; index < entries.size(); index++) {
            ConfigOptionWrapper entry = entries.get(index);

            if (entry.getConfig() != null && matchesTarget(entry.getConfig())) {
                targetFound = true;

                // Leave one or two entries of context above the target when possible.
                list.getScrollbar().setValue(Math.max(0, index - 2));
                list.refreshEntries();
                return;
            }
        }
    }

    /** Called after setScreen() so subclass-created category buttons are available. */
    public static synchronized void afterScreenOpened(GuiBase screen) {
        if (!active || targetScreen != screen || targetFound || !(screen instanceof GuiConfigsBase)) {
            return;
        }

        String wantedCategory = normalize(targetCategory);

        if (wantedCategory.isEmpty() || "configs".equals(wantedCategory)) {
            return;
        }

        List<ButtonBase> buttons = ((GuiBaseAccessor) (Object) screen)
                .notAGoodModForSurvival$getButtons();

        for (ButtonBase button : buttons) {
            String displayString = ((ButtonBaseAccessor) (Object) button)
                    .notAGoodModForSurvival$getDisplayString();

            if (wantedCategory.equals(normalize(displayString))) {
                button.onMouseClicked(button.getX() + 1, button.getY() + 1, 0);
                return;
            }
        }
    }

    public static synchronized boolean shouldHighlight(IConfigBase config) {
        return isActiveForCurrentScreen() && matchesTarget(config);
    }

    public static synchronized void onUserAction(GuiBase screen) {
        if (active && targetScreen == screen) {
            clear();
        }
    }

    private static boolean isActiveForCurrentScreen() {
        return active && targetScreen != null &&
                targetScreen == MinecraftClient.getInstance().currentScreen;
    }

    private static boolean matchesTarget(IConfigBase config) {
        if (config == targetConfig) {
            return true;
        }

        return targetConfigName != null && targetConfigName.equals(config.getName());
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private static void clear() {
        targetScreen = null;
        targetConfig = null;
        targetConfigName = null;
        targetCategory = null;
        targetFound = false;
        active = false;
    }
}
