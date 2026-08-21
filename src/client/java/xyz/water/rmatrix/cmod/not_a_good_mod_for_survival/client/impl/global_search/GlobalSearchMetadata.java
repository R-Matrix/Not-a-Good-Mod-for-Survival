package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.util.KeyCodes;
import fi.dy.masa.malilib.util.StringUtils;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import fi.dy.masa.malilib.gui.GuiBase;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.api.global_search.GlobalSearchTabTarget;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search.GlobalSearchSettings;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search.GlobalSearchText;

/** Source and searchable metadata attached to a globally collected Malilib option. */
public final class GlobalSearchMetadata {
    private final String modId;
    private final String modName;
    private final Set<String> categories = new LinkedHashSet<>();
    private Supplier<GuiBase> configScreenSupplier;
    private final boolean showSource;
    private IKeybind keybind;
    private GlobalSearchTabTarget configTabTarget;

    public GlobalSearchMetadata(
            String modId,
            String modName,
            String category,
            Supplier<GuiBase> configScreenSupplier,
            boolean showSource,
            IKeybind keybind,
            GlobalSearchTabTarget configTabTarget
    ) {
        this.modId = modId;
        this.modName = modName;
        this.categories.add(category);
        this.configScreenSupplier = configScreenSupplier;
        this.showSource = showSource;
        this.keybind = keybind;
        this.configTabTarget = configTabTarget;
    }

    public String getModId() {
        return this.modId;
    }

    public String getModName() {
        return this.modName;
    }

    public String getCategory() {
        return this.categories.stream()
                .filter(category -> !"Configs".equalsIgnoreCase(category))
                .findFirst()
                .orElseGet(() -> this.categories.stream().findFirst().orElse("Configs"));
    }

    public void addCategory(String category) {
        if (category != null && !category.isBlank()) {
            this.categories.add(category.trim());
        }
    }

    public boolean shouldShowSource() {
        return this.showSource && GlobalSearchSettings.isSourceDisplayEnabled();
    }

    public boolean hasExternalSource() {
        return this.showSource;
    }

    public Supplier<GuiBase> getConfigScreenSupplier() {
        return this.configScreenSupplier;
    }

    public void setConfigScreenSupplier(Supplier<GuiBase> supplier) {
        if (this.configScreenSupplier == null && supplier != null) {
            this.configScreenSupplier = supplier;
        }
    }

    public IKeybind getKeybind() {
        return this.keybind;
    }

    public void setKeybind(IKeybind keybind) {
        if (this.keybind == null && keybind != null) {
            this.keybind = keybind;
        }
    }

    public GlobalSearchTabTarget getConfigTabTarget() {
        return this.configTabTarget;
    }

    public void setConfigTabTarget(GlobalSearchTabTarget configTabTarget) {
        if (this.configTabTarget == null && configTabTarget != null) {
            this.configTabTarget = configTabTarget;
        }
    }

    public String getDisplayName(IConfigBase config) {
        return config.getConfigGuiDisplayName();
    }

    public String getSourceDisplayName() {
        return StringUtils.translate("not-a-good-mod-for-survival.gui.global_search.from") +
                " " + this.modName + " - " + this.getCategory();
    }

    public boolean matchesMod(String query) {
        return GlobalSearchText.contains(this.modId, query) ||
                GlobalSearchText.contains(this.modName, query);
    }

    public boolean matchesCategory(String query) {
        return this.categories.stream().anyMatch(category -> GlobalSearchText.contains(category, query));
    }

    public boolean matchesKey(String query) {
        if (this.keybind == null) {
            return false;
        }

        String wanted = normalizeKeyName(query);

        for (Integer key : this.keybind.getKeys()) {
            String keyName = KeyCodes.getNameForKey(key);

            if (keyName != null && normalizeKeyName(keyName).equals(wanted)) {
                return true;
            }
        }

        return false;
    }

    public boolean matchesText(IConfigBase config, String query) {
        boolean matchesVisibleName = GlobalSearchText.contains(config.getConfigGuiDisplayName(), query);
        boolean matchesEnglishName = GlobalSearchSettings.isEnglishConfigNameSearchEnabled() &&
                (GlobalSearchText.contains(config.getName(), query) ||
                        GlobalSearchText.contains(config.getTranslatedName(), query));
        boolean matchesComment = GlobalSearchSettings.isCommentSearchEnabled() &&
                GlobalSearchText.contains(config.getComment(), query);
        boolean matchesKeybind = this.keybind != null &&
                GlobalSearchText.contains(this.keybind.getKeysDisplayString(), query);

        return matchesVisibleName || matchesEnglishName || matchesComment || matchesKeybind;
    }

    public GuiBase createConfigScreen() {
        if (this.configScreenSupplier == null) {
            return null;
        }

        GuiBase screen;

        try {
            screen = this.configScreenSupplier.get();
        } catch (RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Could not create the config screen for {}.", this.modId, exception);
            return null;
        }
        Screen currentScreen = MinecraftClient.getInstance().currentScreen;

        if (screen != null && screen != currentScreen && currentScreen instanceof GuiBase currentGui) {
            screen.setParent(currentGui);
        }

        return screen;
    }

    private static String normalizeKeyName(String keyName) {
        return keyName.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

}
