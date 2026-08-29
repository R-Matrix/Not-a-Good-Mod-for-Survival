package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.api.global_search.IGlobalSearchTabTarget;

/** Best-effort discovery for Malilib screens whose tab-like enum drives getConfigs(). */
final class ReflectiveConfigTabCollector {
    private static final String TAB_ENUM_NAME = "ConfigGuiTab";
    private static final int MAX_GENERIC_TABS = 64;
    private static final Set<String> GENERIC_SCAN_BLOCKED_PREFIXES = Set.of(
            "java.", "javax.", "jdk.", "sun.", "net.minecraft.", "fi.dy.masa.",
            "com.mojang.", "org.", "net.fabricmc.", "com.google.", "it.unimi.");

    private ReflectiveConfigTabCollector() {
    }

    static boolean collect(GuiConfigsBase screen, Consumer<GlobalSearchTabPage> pageConsumer) {
        try {
            return collectInternal(screen, pageConsumer);
        } catch (LinkageError | RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Could not inspect config tabs for {}. Falling back to the first page.",
                    screen.getClass().getName(), exception);
            return false;
        }
    }

    private static boolean collectInternal(
            GuiConfigsBase screen,
            Consumer<GlobalSearchTabPage> pageConsumer
    ) {
        Class<?> screenType = screen.getClass();
        Class<?> tabType = findTabType(screenType);
        TabState state = tabType != null ? findTabState(screen, screenType, tabType) : null;

        if (state == null) {
            state = findGenericTabState(screen, screenType);
        }

        if (state == null) {
            return false;
        }

        Object[] constants = state.tabType().getEnumConstants();

        if (constants == null || constants.length == 0) {
            return false;
        }

        Enum<?> previousTab;
        try {
            previousTab = state.get();
        } catch (ReflectiveOperationException | RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Could not read the current config tab for {}. Falling back to the first page.",
                    screenType.getName(), exception);
            return false;
        }

        boolean collected = false;

        try {
            for (Object constant : constants) {
                if (!(constant instanceof Enum<?> tab)) {
                    continue;
                }

                try {
                    state.set(tab);

                    List<ConfigOptionWrapper> options = screen.getConfigs();
                    pageConsumer.accept(new GlobalSearchTabPage(
                            getTabName(tab), options, state.createTarget(tab)));
                    collected = true;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    NotAGoodModForSurvival.LOGGER.debug(
                            "Could not collect config tab {} from {}.",
                            tab.name(), screenType.getName(), exception);
                }
            }
        } finally {
            try {
                state.set(previousTab);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                NotAGoodModForSurvival.LOGGER.warn(
                        "Could not restore config tab {} for {} after global config scanning.",
                        previousTab, screenType.getName(), exception);
            }
        }

        return collected;
    }

    private static Class<?> findTabType(Class<?> screenType) {
        for (Class<?> type = screenType; type != null && type != Object.class; type = type.getSuperclass()) {
            for (Class<?> nestedType : type.getDeclaredClasses()) {
                if (nestedType.isEnum() && TAB_ENUM_NAME.equals(nestedType.getSimpleName())) {
                    return nestedType;
                }
            }
        }

        return null;
    }

    private static TabState findTabState(GuiConfigsBase screen, Class<?> screenType, Class<?> tabType) {
        TabState state = findMethodState(screenType, screen, tabType, false);

        if (state != null) {
            return state;
        }

        state = findFieldState(screenType, screen, tabType, false);

        if (state != null) {
            return state;
        }

        for (String holderName : getPossibleStateHolders(screenType)) {
            Class<?> holderType = loadClass(holderName, screenType.getClassLoader());

            if (holderType == null) {
                continue;
            }

            state = findMethodState(holderType, null, tabType, true);

            if (state != null) {
                return state;
            }

            state = findFieldState(holderType, null, tabType, true);

            if (state != null) {
                return state;
            }
        }

        return null;
    }

    private static TabState findMethodState(
            Class<?> type,
            Object instance,
            Class<?> tabType,
            boolean staticOnly
    ) {
        Method getter = null;
        Method setter = null;

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (!containsTab(method.getName())) {
                    continue;
                }

                int modifiers = method.getModifiers();

                if (staticOnly && !Modifier.isStatic(modifiers)) {
                    continue;
                }

                if (method.getParameterCount() == 0 && method.getReturnType() == tabType) {
                    if (makeAccessible(method)) {
                        getter = method;
                    }
                } else if (method.getParameterCount() == 1 &&
                        method.getParameterTypes()[0] == tabType && makeAccessible(method)) {
                    setter = method;
                }
            }
        }

        if (getter == null || setter == null) {
            return null;
        }

        boolean getterStatic = Modifier.isStatic(getter.getModifiers());
        boolean setterStatic = Modifier.isStatic(setter.getModifiers());

        if (getterStatic != setterStatic || (!getterStatic && instance == null)) {
            return null;
        }

        return new MethodTabState(getter, setter, getterStatic ? null : instance, tabType);
    }

    private static TabState findFieldState(
            Class<?> type,
            Object instance,
            Class<?> tabType,
            boolean staticOnly
    ) {
        Field bestField = null;
        int bestScore = Integer.MIN_VALUE;

        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType() != tabType || Modifier.isFinal(field.getModifiers())) {
                    continue;
                }

                boolean isStatic = Modifier.isStatic(field.getModifiers());

                if ((staticOnly && !isStatic) || (!staticOnly && !isStatic && instance == null)) {
                    continue;
                }

                if (!isStatic && Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                if (!makeAccessible(field)) {
                    continue;
                }

                int score = tabFieldScore(field.getName());

                if (score > bestScore) {
                    bestField = field;
                    bestScore = score;
                }
            }
        }

        if (bestField == null) {
            return null;
        }

        return new FieldTabState(bestField,
                Modifier.isStatic(bestField.getModifiers()) ? null : instance, tabType);
    }

    private static Set<String> getPossibleStateHolders(Class<?> screenType) {
        Set<String> names = new LinkedHashSet<>();
        String packageName = screenType.getPackageName();

        addHolder(names, packageName, "DataManager");

        int guiPackageIndex = packageName.lastIndexOf(".gui");

        if (guiPackageIndex >= 0) {
            String modPackage = packageName.substring(0, guiPackageIndex);
            addHolder(names, modPackage + ".data", "DataManager");
            addHolder(names, modPackage + ".config", "DataManager");
            addHolder(names, modPackage, "DataManager");
        }

        int lastPackageIndex = packageName.lastIndexOf('.');

        if (lastPackageIndex > 0) {
            addHolder(names, packageName.substring(0, lastPackageIndex), "DataManager");
        }

        return names;
    }

    private static void addHolder(Set<String> names, String packageName, String simpleName) {
        if (packageName != null && !packageName.isBlank()) {
            names.add(packageName + "." + simpleName);
        }
    }

    private static Class<?> loadClass(String name, ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException | LinkageError | RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.trace(
                    "Optional config state holder {} could not be loaded.", name, exception);
            return null;
        }
    }

    private static boolean containsTab(String name) {
        return name.toLowerCase(java.util.Locale.ROOT).contains("tab");
    }

    private static int tabFieldScore(String name) {
        String normalized = name.toLowerCase(java.util.Locale.ROOT);

        if ("configguitab".equals(normalized)) {
            return 100;
        }

        if ("selectedtab".equals(normalized)) {
            return 90;
        }

        if ("tab".equals(normalized)) {
            return 80;
        }

        return normalized.contains("tab") ? 50 : 0;
    }

    private static TabState findGenericTabState(GuiConfigsBase screen, Class<?> screenType) {
        for (FieldPath path : collectEnumFieldPaths(screenType)) {
            Class<?> enumType = path.terminal().getType();
            Object[] constants = enumType.getEnumConstants();

            if (constants == null || constants.length < 2 || constants.length > MAX_GENERIC_TABS) {
                continue;
            }

            if ((path.holder() != null && !makeAccessible(path.holder())) ||
                    !makeAccessible(path.terminal())) {
                continue;
            }

            if (!validateTabPath(screen, path, constants)) {
                continue;
            }

            NotAGoodModForSurvival.LOGGER.debug(
                    "Detected generic config tab enum {} for {}.",
                    enumType.getName(), screenType.getName());
            return new PathTabState(path.holder(), path.terminal(), enumType, screen);
        }

        return null;
    }

    private static List<FieldPath> collectEnumFieldPaths(Class<?> screenType) {
        List<FieldPath> paths = new ArrayList<>();
        String modPackage = getModPackage(screenType);

        for (Class<?> type = screenType;
             type != null && type != Object.class && type != GuiConfigsBase.class;
             type = type.getSuperclass()) {

            for (Field field : type.getDeclaredFields()) {
                if (isGenericEnumTerminal(field)) {
                    paths.add(new FieldPath(null, field));
                }
            }

            for (Field holder : type.getDeclaredFields()) {
                if (holder.isSynthetic() || !Modifier.isStatic(holder.getModifiers()) ||
                        !isPlausibleHolder(holder.getType(), modPackage)) {
                    continue;
                }

                for (Field inner : holder.getType().getDeclaredFields()) {
                    if (isGenericEnumTerminal(inner)) {
                        paths.add(new FieldPath(holder, inner));
                    }
                }
            }
        }

        return paths;
    }

    private static boolean isGenericEnumTerminal(Field field) {
        return !field.isSynthetic() &&
                !Modifier.isFinal(field.getModifiers()) &&
                field.getType().isEnum() &&
                !isBlockedType(field.getType()) &&
                containsTabOrCategory(field.getName());
    }

    private static boolean isPlausibleHolder(Class<?> holderType, String modPackage) {
        if (holderType.isEnum() || holderType.isArray() || holderType.isPrimitive()) {
            return false;
        }

        String packageName = holderType.getPackageName();

        return !packageName.isEmpty() &&
                (packageName.equals(modPackage) || packageName.startsWith(modPackage + ".")) &&
                !isBlockedType(holderType);
    }

    private static boolean isBlockedType(Class<?> type) {
        String name = type.getName();

        for (String prefix : GENERIC_SCAN_BLOCKED_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static String getModPackage(Class<?> screenType) {
        String packageName = screenType.getPackageName();
        int guiPackageIndex = packageName.lastIndexOf(".gui");

        return guiPackageIndex >= 0 ? packageName.substring(0, guiPackageIndex) : packageName;
    }

    private static boolean containsTabOrCategory(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);

        return normalized.contains("tab") || normalized.contains("category");
    }

    private static boolean validateTabPath(
            GuiConfigsBase screen,
            FieldPath path,
            Object[] constants
    ) {
        Object holder;
        Object original;

        try {
            holder = resolvePathHolder(screen, path.holder(), path.terminal());
            original = path.terminal().get(holder);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }

        Set<String> pageSignatures = new HashSet<>();
        int nonEmptyPages = 0;

        try {
            for (Object constant : constants) {
                path.terminal().set(holder, constant);

                List<String> names = new ArrayList<>();

                for (ConfigOptionWrapper wrapper : screen.getConfigs()) {
                    names.add(wrapper.getConfig() == null ? "" : wrapper.getConfig().getName());
                }

                if (!names.isEmpty()) {
                    nonEmptyPages++;
                    pageSignatures.add(String.join("\u0000", names));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        } finally {
            try {
                path.terminal().set(holder, original);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                NotAGoodModForSurvival.LOGGER.trace(
                        "Could not restore a generic config tab probe value on {}.",
                        screen.getClass().getName(), exception);
            }
        }

        return nonEmptyPages >= 2 && pageSignatures.size() >= 2;
    }

    private static Object resolvePathHolder(Object screen, Field holderField, Field terminalField)
            throws ReflectiveOperationException {
        if (holderField == null) {
            return Modifier.isStatic(terminalField.getModifiers()) ? null : screen;
        }

        Object base = Modifier.isStatic(holderField.getModifiers()) ? null : screen;
        return holderField.get(base);
    }

    private static String getTabName(Enum<?> tab) {
        Class<?> tabType = tab.getDeclaringClass();

        for (String methodName : List.of("getDisplayName", "getName")) {
            try {
                Method method = tabType.getDeclaredMethod(methodName);

                if (method.getReturnType() == String.class && makeAccessible(method)) {
                    return (String) method.invoke(tab);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                     RuntimeException exception) {
                NotAGoodModForSurvival.LOGGER.trace(
                        "Could not resolve display name method {} on {}. Using the enum name.",
                        methodName, tabType.getName(), exception);
            }
        }

        return tab.name();
    }

    private static boolean makeAccessible(java.lang.reflect.AccessibleObject object) {
        try {
            return object.trySetAccessible();
        } catch (RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.trace(
                    "Could not make reflective member accessible: {}.", object, exception);
            return false;
        }
    }

    private interface TabState {
        Class<?> tabType();

        Enum<?> get() throws ReflectiveOperationException;

        void set(Enum<?> tab) throws ReflectiveOperationException;

        IGlobalSearchTabTarget createTarget(Enum<?> tab);
    }

    private record FieldTabState(Field field, Object target, Class<?> tabType) implements TabState {
        @Override
        public Enum<?> get() throws IllegalAccessException {
            return (Enum<?>) this.field.get(this.target);
        }

        @Override
        public void set(Enum<?> tab) throws IllegalAccessException {
            this.field.set(this.target, tab);
        }

        @Override
        public IGlobalSearchTabTarget createTarget(Enum<?> tab) {
            return screen -> {
                Object target = Modifier.isStatic(this.field.getModifiers()) ? null : screen;

                if (target != null && !this.field.getDeclaringClass().isInstance(target)) {
                    return false;
                }

                try {
                    this.field.set(target, tab);
                    return true;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    NotAGoodModForSurvival.LOGGER.debug(
                            "Could not select config tab {} on {}.",
                            tab.name(), screen.getClass().getName(), exception);
                    return false;
                }
            };
        }
    }

    private record MethodTabState(
            Method getter,
            Method setter,
            Object target,
            Class<?> tabType
    ) implements TabState {
        @Override
        public Enum<?> get() throws IllegalAccessException, InvocationTargetException {
            return (Enum<?>) this.getter.invoke(this.target);
        }

        @Override
        public void set(Enum<?> tab) throws IllegalAccessException, InvocationTargetException {
            this.setter.invoke(this.target, tab);
        }

        @Override
        public IGlobalSearchTabTarget createTarget(Enum<?> tab) {
            return screen -> {
                Object target = Modifier.isStatic(this.setter.getModifiers()) ? null : screen;

                if (target != null && !this.setter.getDeclaringClass().isInstance(target)) {
                    return false;
                }

                try {
                    this.setter.invoke(target, tab);
                    return true;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    NotAGoodModForSurvival.LOGGER.debug(
                            "Could not select config tab {} on {}.",
                            tab.name(), screen.getClass().getName(), exception);
                    return false;
                }
            };
        }
    }

    private record PathTabState(
            Field holderField,
            Field terminalField,
            Class<?> tabType,
            GuiConfigsBase scanTarget
    ) implements TabState {
        @Override
        public Enum<?> get() throws ReflectiveOperationException {
            return (Enum<?>) this.terminalField.get(resolvePathHolder(
                    this.scanTarget, this.holderField, this.terminalField));
        }

        @Override
        public void set(Enum<?> tab) throws ReflectiveOperationException {
            this.terminalField.set(resolvePathHolder(
                    this.scanTarget, this.holderField, this.terminalField), tab);
        }

        @Override
        public IGlobalSearchTabTarget createTarget(Enum<?> tab) {
            return screen -> {
                try {
                    if (this.holderField != null) {
                        Object base = Modifier.isStatic(this.holderField.getModifiers())
                                ? null : screen;

                        if (base != null && !this.holderField.getDeclaringClass().isInstance(base)) {
                            return false;
                        }

                        Object holder = this.holderField.get(base);

                        if (holder == null ||
                                !this.terminalField.getDeclaringClass().isInstance(holder)) {
                            return false;
                        }

                        this.terminalField.set(holder, tab);
                        return true;
                    }

                    Object target = Modifier.isStatic(this.terminalField.getModifiers())
                            ? null : screen;

                    if (target != null &&
                            !this.terminalField.getDeclaringClass().isInstance(target)) {
                        return false;
                    }

                    this.terminalField.set(target, tab);
                    return true;
                } catch (ReflectiveOperationException | RuntimeException exception) {
                    NotAGoodModForSurvival.LOGGER.debug(
                            "Could not select the generic config tab {} on {}.",
                            tab.name(), screen.getClass().getName(), exception);
                    return false;
                }
            };
        }
    }

    private record FieldPath(Field holder, Field terminal) {
    }

    record GlobalSearchTabPage(
            String category,
            List<ConfigOptionWrapper> options,
            IGlobalSearchTabTarget tabTarget
    ) {
        GlobalSearchTabPage {
            category = category == null || category.isBlank() ? "Configs" : category.trim();
            options = List.copyOf(options);
        }
    }
}
