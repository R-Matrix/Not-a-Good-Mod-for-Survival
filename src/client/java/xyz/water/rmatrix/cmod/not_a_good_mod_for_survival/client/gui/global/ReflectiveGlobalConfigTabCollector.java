package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Best-effort discovery for Malilib screens that expose a nested ConfigGuiTab enum. */
final class ReflectiveGlobalConfigTabCollector {
    private static final String TAB_ENUM_NAME = "ConfigGuiTab";

    private ReflectiveGlobalConfigTabCollector() {
    }

    static boolean collect(GuiConfigsBase screen, Consumer<GlobalConfigTabPage> pageConsumer) {
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
            Consumer<GlobalConfigTabPage> pageConsumer
    ) {
        Class<?> screenType = screen.getClass();
        Class<?> tabType = findTabType(screenType);

        if (tabType == null) {
            return false;
        }

        TabState state = findTabState(screen, screenType, tabType);

        if (state == null) {
            return false;
        }

        Object[] constants = tabType.getEnumConstants();

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
                    pageConsumer.accept(new GlobalConfigTabPage(
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

        return new MethodTabState(getter, setter, getterStatic ? null : instance);
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

        return new FieldTabState(bestField, Modifier.isStatic(bestField.getModifiers()) ? null : instance);
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
        Enum<?> get() throws ReflectiveOperationException;

        void set(Enum<?> tab) throws ReflectiveOperationException;

        GlobalConfigTabTarget createTarget(Enum<?> tab);
    }

    private record FieldTabState(Field field, Object target) implements TabState {
        @Override
        public Enum<?> get() throws IllegalAccessException {
            return (Enum<?>) this.field.get(this.target);
        }

        @Override
        public void set(Enum<?> tab) throws IllegalAccessException {
            this.field.set(this.target, tab);
        }

        @Override
        public GlobalConfigTabTarget createTarget(Enum<?> tab) {
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

    private record MethodTabState(Method getter, Method setter, Object target) implements TabState {
        @Override
        public Enum<?> get() throws IllegalAccessException, InvocationTargetException {
            return (Enum<?>) this.getter.invoke(this.target);
        }

        @Override
        public void set(Enum<?> tab) throws IllegalAccessException, InvocationTargetException {
            this.setter.invoke(this.target, tab);
        }

        @Override
        public GlobalConfigTabTarget createTarget(Enum<?> tab) {
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

    record GlobalConfigTabPage(
            String category,
            List<ConfigOptionWrapper> options,
            GlobalConfigTabTarget tabTarget
    ) {
        GlobalConfigTabPage {
            category = category == null || category.isBlank() ? "Configs" : category.trim();
            options = List.copyOf(options);
        }
    }
}
