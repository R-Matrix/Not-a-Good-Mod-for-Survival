package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.fabricmc.loader.api.FabricLoader;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
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
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Hotkeys;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.global.ConfigManagerMixin;

/** Builds a searchable snapshot of the Malilib configuration and hotkey registries. */
public final class GlobalConfigRepository {
    private static final String LOCAL_MOD_ID = NotAGoodModForSurvival.MOD_ID;
    private static final String LOCAL_MOD_NAME = "Not a Good Mod for Survival";
    private static List<GlobalConfigOptionWrapper> entries = List.of();
    private static Set<String> sourceModIds = Set.of();

    private GlobalConfigRepository() {
    }

    public static synchronized void rebuild() {
        Map<String, GlobalConfigOptionWrapper> byConfig = new LinkedHashMap<>();
        Map<String, ModInfo> registeredScreens = getRegisteredScreens();

        addConfigs(byConfig, LOCAL_MOD_ID, LOCAL_MOD_NAME, "Test", null, false, Configs.Test.OPTIONS);
        addConfigs(byConfig, LOCAL_MOD_ID, LOCAL_MOD_NAME, "Debug Render", null, false, Configs.DebugRender.OPTIONS);
        addConfigs(byConfig, LOCAL_MOD_ID, LOCAL_MOD_NAME, "Fireworks", null, false, Configs.Fireworks.OPTIONS);
        addConfigs(byConfig, LOCAL_MOD_ID, LOCAL_MOD_NAME, "Signs", null, false, Configs.Signs.OPTIONS);
        addConfigs(byConfig, LOCAL_MOD_ID, LOCAL_MOD_NAME, "Bridging", null, false, Configs.Bridging.OPTIONS);
        addConfigs(byConfig, LOCAL_MOD_ID, LOCAL_MOD_NAME, "Movement", null, false, Configs.Movement.OPTIONS);
        addConfigs(byConfig, LOCAL_MOD_ID, LOCAL_MOD_NAME, "Global Search", null, false, Configs.GlobalSearch.OPTIONS);
        addConfigs(byConfig, LOCAL_MOD_ID, LOCAL_MOD_NAME, "Hotkeys", null, false, Hotkeys.HOTKEY_LIST);

        collectRegisteredScreenConfigs(byConfig);
        collectRegisteredHandlerConfigs(byConfig, registeredScreens);
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
            if (LOCAL_MOD_ID.equalsIgnoreCase(modInfo.getModId())) {
                continue;
            }

            Supplier<GuiBase> supplier = modInfo.getConfigScreenSupplier();

            if (supplier == null) {
                continue;
            }

            GuiBase screen = createScreen(supplier);

            if (screen instanceof GuiConfigsBase configScreen) {
                addScreenConfigs(byConfig, modInfo, supplier, configScreen);
            }
        }
    }

    private static void collectRegisteredHandlerConfigs(
            Map<String, GlobalConfigOptionWrapper> byConfig,
            Map<String, ModInfo> registeredScreens
    ) {
        Map<String, IConfigHandler> handlers = getRegisteredConfigHandlers();

        for (Map.Entry<String, IConfigHandler> handlerEntry : handlers.entrySet()) {
            String modId = handlerEntry.getKey();

            if (LOCAL_MOD_ID.equalsIgnoreCase(modId)) {
                continue;
            }

            ModInfo registered = registeredScreens.get(modId.toLowerCase(Locale.ROOT));
            String modName = registered != null ? registered.getModName() : getModName(modId);
            Supplier<GuiBase> supplier = registered != null ? registered.getConfigScreenSupplier() : null;

            for (DiscoveredConfig discovered : discoverConfigs(handlerEntry.getValue())) {
                addConfig(byConfig, modId, modName, discovered.category(), supplier, true,
                        discovered.config(), getKeybind(discovered.config()));
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

    @SuppressWarnings("unchecked")
    private static Map<String, IConfigHandler> getRegisteredConfigHandlers() {
        try {
            return ((ConfigManagerMixin) (Object) ConfigManager.getInstance())
                    .notAGoodModForSurvival$getConfigHandlers();
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    private static List<DiscoveredConfig> discoverConfigs(IConfigHandler handler) {
        if (handler == null) {
            return List.of();
        }

        List<DiscoveredConfig> result = new java.util.ArrayList<>();
        Set<Class<?>> visitedClasses = new HashSet<>();
        Set<IConfigBase> visitedConfigs = Collections.newSetFromMap(new IdentityHashMap<>());
        discoverClass(handler.getClass(), handler, handler.getClass().getSimpleName(),
                visitedClasses, visitedConfigs, result);
        return result;
    }

    private static void discoverClass(
            Class<?> type,
            Object instance,
            String fallbackCategory,
            Set<Class<?>> visitedClasses,
            Set<IConfigBase> visitedConfigs,
            List<DiscoveredConfig> result
    ) {
        if (type == null || type == Object.class || !visitedClasses.add(type)) {
            return;
        }

        String category = type.getSimpleName().isBlank() ? fallbackCategory : type.getSimpleName();

        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }

            boolean isStatic = Modifier.isStatic(field.getModifiers());

            if (!isStatic && instance == null) {
                continue;
            }

            if (!isStatic && type != instance.getClass()) {
                continue;
            }

            try {
                if (!field.trySetAccessible()) {
                    continue;
                }
            } catch (RuntimeException ignored) {
                continue;
            }

            try {
                Object value = field.get(isStatic ? null : instance);
                collectConfigValue(value, field.getName(), category, visitedConfigs, result);
            } catch (IllegalAccessException | RuntimeException ignored) {
                // A third-party handler may contain inaccessible or client-state fields.
            }
        }

        for (Class<?> nestedClass : type.getDeclaredClasses()) {
            discoverClass(nestedClass, null, category, visitedClasses, visitedConfigs, result);
        }

        discoverClass(type.getSuperclass(), instance, fallbackCategory,
                visitedClasses, visitedConfigs, result);
    }

    private static void collectConfigValue(
            Object value,
            String fieldName,
            String category,
            Set<IConfigBase> visitedConfigs,
            List<DiscoveredConfig> result
    ) {
        if (value instanceof IConfigBase config) {
            if (visitedConfigs.add(config)) {
                result.add(new DiscoveredConfig(config, category));
            }
            return;
        }

        if (!looksLikeConfigCollection(fieldName) || value == null) {
            return;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object element : iterable) {
                collectConfigValue(element, fieldName, category, visitedConfigs, result);
            }
            return;
        }

        if (value instanceof Map<?, ?> map) {
            for (Object element : map.values()) {
                collectConfigValue(element, fieldName, category, visitedConfigs, result);
            }
            return;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);

            for (int i = 0; i < length; i++) {
                collectConfigValue(Array.get(value, i), fieldName, category, visitedConfigs, result);
            }
        }
    }

    private static boolean looksLikeConfigCollection(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        return normalized.contains("config") || normalized.contains("option") || normalized.contains("hotkey");
    }

    private static void addScreenConfigs(
            Map<String, GlobalConfigOptionWrapper> byConfig,
            ModInfo modInfo,
            Supplier<GuiBase> supplier,
            GuiConfigsBase configScreen
    ) {
        String category = "Configs";

        try {
            for (ConfigOptionWrapper wrapper : configScreen.getConfigs()) {
                if (wrapper.getType() == ConfigOptionWrapper.Type.LABEL) {
                    String label = wrapper.getLabel();

                    if (label != null && !label.isBlank() && !label.replace("-", "").isBlank()) {
                        category = label.trim();
                    }
                } else if (wrapper.getConfig() != null) {
                    IConfigBase config = wrapper.getConfig();
                    addConfig(byConfig, modInfo.getModId(), modInfo.getModName(), category, supplier, true,
                            config, getKeybind(config));
                }
            }
        } catch (RuntimeException ignored) {
            // The screen can depend on a selected tab or client state. Reflection remains the fallback.
        }
    }

    private static void addConfigs(
            Map<String, GlobalConfigOptionWrapper> byConfig,
            String modId,
            String modName,
            String category,
            Supplier<GuiBase> supplier,
            boolean showSource,
            List<? extends IConfigBase> configs
    ) {
        for (IConfigBase config : configs) {
            addConfig(byConfig, modId, modName, category, supplier, showSource, config, getKeybind(config));
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

        if (!byConfig.containsKey(uniqueKey)) {
            GlobalConfigMetadata metadata = new GlobalConfigMetadata(
                    modId, modName, category, supplier, showSource, keybind);
            byConfig.put(uniqueKey, new GlobalConfigOptionWrapper(config, metadata));
        } else {
            GlobalConfigOptionWrapper existing = byConfig.get(uniqueKey);
            existing.getMetadata().addCategory(category);

            if (existing.getMetadata().getConfigScreenSupplier() == null && supplier != null) {
                existing.getMetadata().setConfigScreenSupplier(supplier);
            }

            if (existing.getMetadata().getKeybind() == null && keybind != null) {
                existing.getMetadata().setKeybind(keybind);
            }
        }
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
        } catch (RuntimeException ignored) {
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

    private static String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElse(modId);
    }

    private record DiscoveredConfig(IConfigBase config, String category) {
    }
}
