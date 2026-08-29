package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search;

import java.util.*;
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
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.api.global_search.IGlobalSearchTabTarget;

/** Builds a searchable snapshot of the Malilib configuration and hotkey registries. */
public final class GlobalSearchRepository {
    private static final String LOCAL_MOD_ID = NotAGoodModForSurvival.MOD_ID;
    private static final GlobalSearchBaselineStore BASELINE_STORE = new GlobalSearchBaselineStore();
    private static List<GlobalSearchOption> entries = List.of();
    private static Set<String> sourceModIds = Set.of();

    private GlobalSearchRepository() {
    }

    public static synchronized void rebuild() {
        Map<String, GlobalSearchOption> byConfig = new LinkedHashMap<>();
        Map<String, ModInfo> registeredScreens = getRegisteredScreens();

        collectRegisteredScreenConfigs(byConfig);
        collectRegisteredHotkeys(byConfig, registeredScreens);

        entries = List.copyOf(byConfig.values());
        sourceModIds = Collections.unmodifiableSet(new LinkedHashSet<>(
                entries.stream().map(entry -> entry.getMetadata().getModId()).toList()));
        captureBaselines();
    }

    /**
     * Reports the external mods whose indexed options changed since the last call and
     * refreshes keybind handling when a bound option was among them.
     */
    public static synchronized Set<String> collectChangedExternalModIds() {
        Map<String, String> currentValues = new HashMap<>();

        for (GlobalSearchOption entry : entries) {
            currentValues.put(uniqueKey(entry.getMetadata().getModId(), entry.getConfig().getName()),
                    readValueString(entry));
        }

        List<String> changedKeys = BASELINE_STORE.diffAndCommit(currentValues);

        if (changedKeys.isEmpty()) {
            return Set.of();
        }

        Set<String> externalModIds = new LinkedHashSet<>();
        boolean keybindChanged = false;

        for (GlobalSearchOption entry : entries) {
            String key = uniqueKey(entry.getMetadata().getModId(), entry.getConfig().getName());

            if (!changedKeys.contains(key)) {
                continue;
            }

            if (entry.getMetadata().getKeybind() != null) {
                keybindChanged = true;
            }

            if (entry.getMetadata().hasExternalSource()) {
                externalModIds.add(entry.getMetadata().getModId());
            }
        }

        if (keybindChanged) {
            InputEventHandler.getKeybindManager().updateUsedKeys();
        }

        return externalModIds;
    }

    private static void captureBaselines() {
        Set<String> liveKeys = new LinkedHashSet<>();

        for (GlobalSearchOption entry : entries) {
            String key = uniqueKey(entry.getMetadata().getModId(), entry.getConfig().getName());
            liveKeys.add(key);
            BASELINE_STORE.recordIfAbsent(key, readValueString(entry));
        }

        BASELINE_STORE.retainKeys(liveKeys);
    }

    private static String readValueString(GlobalSearchOption entry) {
        try {
            String value = entry.getConfig().getAsJsonElement().toString();
            IKeybind keybind = entry.getMetadata().getKeybind();

            if (keybind != null) {
                value = value + "|" + keybind.getKeysDisplayString();
            }

            return value;
        } catch (RuntimeException | LinkageError exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Could not read a config value snapshot for {}.", entry.getConfig().getName(), exception);
            return null;
        }
    }

    private static String uniqueKey(String modId, String configName) {
        return modId.toLowerCase(Locale.ROOT) + "\u0000" + configName.toLowerCase(Locale.ROOT);
    }

    public static synchronized List<GlobalSearchOption> getEntries() {
        return entries;
    }

    private static void collectRegisteredScreenConfigs(
            Map<String, GlobalSearchOption> byConfig) {
        for (ModInfo modInfo : Registry.CONFIG_SCREEN.getAllModsWithConfigScreens()) {
            Supplier<GuiBase> supplier = modInfo.getConfigScreenSupplier();

            if (supplier == null) {
                continue;
            }

            GuiBase screen = createScreen(supplier);

            if (screen instanceof GuiConfigsBase configScreen) {
                if (!ReflectiveConfigTabCollector.collect(configScreen, page -> addScreenConfigs(
                        byConfig, modInfo, supplier, page.category(), page.options(), page.tabTarget()))) {
                    addScreenConfigs(byConfig, modInfo, supplier, "Configs", configScreen.getConfigs(), null);
                }
            }
        }
    }

    private static void collectRegisteredHotkeys(
            Map<String, GlobalSearchOption> byConfig,
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
            Map<String, GlobalSearchOption> byConfig,
            ModInfo modInfo,
            Supplier<GuiBase> supplier,
            String initialCategory,
            List<ConfigOptionWrapper> wrappers,
            IGlobalSearchTabTarget tabTarget
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
                            getKeybind(config), tabTarget);
                }
            }
        } catch (RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Could not collect config options from {}.", modInfo.getModId(), exception);
        }
    }

    private static void addConfig(
            Map<String, GlobalSearchOption> byConfig,
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

        String uniqueKey = uniqueKey(modId, config.getName());

        GlobalSearchOption existing = byConfig.get(uniqueKey);

        if (existing == null) {
            GlobalSearchMetadata metadata = new GlobalSearchMetadata(
                    modId, modName, category, supplier, showSource, keybind, null);
            byConfig.put(uniqueKey, new GlobalSearchOption(config, metadata));
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
            Map<String, GlobalSearchOption> byConfig,
            String modId,
            String modName,
            String category,
            Supplier<GuiBase> supplier,
            boolean showSource,
            IConfigBase config,
            IKeybind keybind,
            IGlobalSearchTabTarget tabTarget
    ) {
        if (config == null || modId == null || modName == null) {
            return;
        }

        String uniqueKey = uniqueKey(modId, config.getName());
        GlobalSearchMetadata metadata = new GlobalSearchMetadata(
                modId, modName, category, supplier, showSource, keybind, tabTarget);

        GlobalSearchOption existing = byConfig.get(uniqueKey);

        if (existing != null) {
            existing.getMetadata().getCategories().forEach(metadata::addCategory);
        }

        // A later tab is usually more specific than an "All" tab, so preserve its source.
        byConfig.put(uniqueKey, new GlobalSearchOption(config, metadata));
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

                return Objects.requireNonNullElseGet(registered, () -> new ModInfo(mod.getMetadata().getId(), mod.getMetadata().getName()));

            }
        }

        return null;
    }

}
