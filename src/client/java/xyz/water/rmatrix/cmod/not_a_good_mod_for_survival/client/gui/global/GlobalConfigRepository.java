package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.fabricmc.loader.api.FabricLoader;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.BooleanHotkeyGuiWrapper;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeybindCategory;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Builds a searchable snapshot of the Malilib configuration and hotkey registries. */
public final class GlobalConfigRepository {
    private static final String LOCAL_MOD_ID = NotAGoodModForSurvival.MOD_ID;
    private static List<GlobalConfigOptionWrapper> entries = List.of();
    private static Set<String> sourceModIds = Set.of();

    private GlobalConfigRepository() {
    }

    public static synchronized void rebuild() {
        Map<String, GlobalConfigOptionWrapper> byConfig = new LinkedHashMap<>();
        Map<String, ModInfo> registeredScreens = getRegisteredScreens();

        collectRegisteredScreenConfigs(byConfig);
        collectRegisteredHotkeys(byConfig, registeredScreens);

        entries = List.copyOf(byConfig.values());
        sourceModIds = Collections.unmodifiableSet(new LinkedHashSet<>(
                entries.stream().map(entry -> entry.getMetadata().getModId()).toList()));
    }

    public static synchronized List<GlobalConfigOptionWrapper> getEntries() {
        return entries;
    }

    public static synchronized Set<String> getSourceModIds() {
        return sourceModIds;
    }

    private static void collectRegisteredScreenConfigs(
            Map<String, GlobalConfigOptionWrapper> byConfig
    ) {
        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            Supplier<GuiBase> supplier = modInfo.getConfigScreenSupplier();

            if (supplier == null) {
                continue;
            }

            GuiBase screen = createScreen(supplier);

            if (screen instanceof GuiConfigsBase configScreen) {
                if (!ReflectiveGlobalConfigTabCollector.collect(configScreen, page -> addScreenConfigs(
                        byConfig, modInfo, supplier, page.category(), page.options()))) {
                    addScreenConfigs(byConfig, modInfo, supplier, "Configs", configScreen.getConfigs());
                }
            }
        }
    }

    private static void collectRegisteredHotkeys(
            Map<String, GlobalConfigOptionWrapper> byConfig,
            Map<String, ModInfo> registeredScreens
    ) {
        for (KeybindCategory category : InputEventHandler.getKeybindManager().getKeybindCategories()) {
            ModInfo modInfo = findModInfo(category.getModName(), registeredScreens);
            String modId = modInfo != null ? modInfo.getModId() : category.getModName();
            String modName = modInfo != null ? modInfo.getModName() : category.getModName();
            Supplier<GuiBase> supplier = modInfo != null ? modInfo.getConfigScreenSupplier() : null;

            for (IHotkey hotkey : category.getHotkeys()) {
                addConfig(byConfig, modId, modName, category.getCategory(), supplier,
                        !LOCAL_MOD_ID.equalsIgnoreCase(modId), hotkey, hotkey.getKeybind());
            }
        }
    }

    private static void addScreenConfigs(
            Map<String, GlobalConfigOptionWrapper> byConfig,
            ModInfo modInfo,
            Supplier<GuiBase> supplier,
            String initialCategory,
            List<ConfigOptionWrapper> wrappers
    ) {
        String category = initialCategory == null || initialCategory.isBlank()
                ? "Configs" : initialCategory;

        try {
            for (ConfigOptionWrapper wrapper : wrappers) {
                if (wrapper.getType() == ConfigOptionWrapper.Type.LABEL) {
                    String label = wrapper.getLabel();

                    if (label != null && !label.isBlank() && !label.replace("-", "").isBlank()) {
                        category = label.trim();
                    }
                } else if (wrapper.getConfig() != null) {
                    IConfigBase config = wrapper.getConfig();
                    addScreenConfig(byConfig, modInfo.getModId(), modInfo.getModName(), category,
                            supplier, !LOCAL_MOD_ID.equalsIgnoreCase(modInfo.getModId()), config,
                            getKeybind(config));
                }
            }
        } catch (RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Could not collect config options from {}.", modInfo.getModId(), exception);
        }
    }

    private static void addConfig(
            Map<String, GlobalConfigOptionWrapper> byConfig,
            String modId,
            String modName,
            String category,
            Supplier<GuiBase> supplier,
            boolean showSource,
            IConfigBase config,
            IKeybind keybind
    ) {
        if (config == null || modId == null || modName == null) {
            return;
        }

        String uniqueKey = modId.toLowerCase(Locale.ROOT) + "\u0000" + config.getName().toLowerCase(Locale.ROOT);

        GlobalConfigOptionWrapper existing = byConfig.get(uniqueKey);

        if (existing == null) {
            GlobalConfigMetadata metadata = new GlobalConfigMetadata(
                    modId, modName, category, supplier, showSource, keybind);
            byConfig.put(uniqueKey, new GlobalConfigOptionWrapper(config, metadata));
            return;
        }

        existing.getMetadata().addCategory(category);

        if (existing.getMetadata().getConfigScreenSupplier() == null && supplier != null) {
            existing.getMetadata().setConfigScreenSupplier(supplier);
        }

        if (existing.getMetadata().getKeybind() == null && keybind != null) {
            existing.getMetadata().setKeybind(keybind);
        }
    }

    private static void addScreenConfig(
            Map<String, GlobalConfigOptionWrapper> byConfig,
            String modId,
            String modName,
            String category,
            Supplier<GuiBase> supplier,
            boolean showSource,
            IConfigBase config,
            IKeybind keybind
    ) {
        if (config == null || modId == null || modName == null) {
            return;
        }

        String uniqueKey = modId.toLowerCase(Locale.ROOT) + "\u0000" + config.getName().toLowerCase(Locale.ROOT);
        GlobalConfigMetadata metadata = new GlobalConfigMetadata(
                modId, modName, category, supplier, showSource, keybind);

        // A later tab is usually more specific than an "All" tab, so preserve its source.
        byConfig.put(uniqueKey, new GlobalConfigOptionWrapper(config, metadata));
    }

    private static IKeybind getKeybind(IConfigBase config) {
        if (config instanceof IHotkey hotkey) {
            return hotkey.getKeybind();
        }

        if (config instanceof BooleanHotkeyGuiWrapper wrapper) {
            return wrapper.getKeybind();
        }

        return null;
    }

    private static GuiBase createScreen(Supplier<GuiBase> supplier) {
        try {
            return supplier.get();
        } catch (RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Could not create a registered config screen.", exception);
            return null;
        }
    }

    private static Map<String, ModInfo> getRegisteredScreens() {
        Map<String, ModInfo> result = new LinkedHashMap<>();

        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            result.put(modInfo.getModId().toLowerCase(Locale.ROOT), modInfo);
            result.put(modInfo.getModName().toLowerCase(Locale.ROOT), modInfo);
        }

        return result;
    }

    private static ModInfo findModInfo(String categoryModName, Map<String, ModInfo> registeredScreens) {
        if (categoryModName == null) {
            return null;
        }

        ModInfo result = registeredScreens.get(categoryModName.toLowerCase(Locale.ROOT));

        if (result != null) {
            return result;
        }

        String query = categoryModName.toLowerCase(Locale.ROOT);

        for (var mod : FabricLoader.getInstance().getAllMods()) {
            if (mod.getMetadata().getId().equalsIgnoreCase(query) ||
                    mod.getMetadata().getName().equalsIgnoreCase(categoryModName)) {
                ModInfo registered = registeredScreens.get(mod.getMetadata().getId().toLowerCase(Locale.ROOT));

                if (registered != null) {
                    return registered;
                }

                return new ModInfo(mod.getMetadata().getId(), mod.getMetadata().getName());
            }
        }

        return null;
    }

}
