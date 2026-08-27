/*
 * Design reference: Fallen_Breath's TweakerMore optional-rule presentation.
 * This file independently adapts its public display-line modifier so that
 * external TweakerMore configurations can use the same unavailable styling.
 *
 * Source: https://github.com/Fallen-Breath/TweakerMore
 */
package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.config;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiBase;

import org.jetbrains.annotations.NotNull;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.api.config.IConfigAvailability;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.tools.ToolConfigs;

/** Resolves availability for local and supported optional-mod configurations. */
public final class ConfigAvailabilityResolver {
    private static final String TWEAKER_MORE_CONFIG_BASE =
            "me.fallenbreath.tweakermore.config.options.TweakerMoreIConfigBase";
    private static final String DISPLAY_LINE_MODIFIER = "getGuiDisplayLineModifier";
    private static final String PROBE_LINE = "not-a-good-mod-for-survival$availability";

    private static final AtomicBoolean MISSING_TWEAKER_MORE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean REFLECTION_FAILURE_LOGGED = new AtomicBoolean();
    private static final ClassValue<Optional<Method>> TWEAKER_MORE_MODIFIER_METHOD =
            new ClassValue<>() {
                @Override
                protected Optional<Method> computeValue(@NotNull Class<?> configClass) {
                    return findTweakerMoreModifier(configClass);
                }
            };
    private static final Map<IConfigBase, Boolean> AVAILABILITY_CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());
    private static final ConcurrentMap<Class<?>, Boolean> TWEAKER_MORE_CONFIG_CLASSES =
            new ConcurrentHashMap<>();

    private ConfigAvailabilityResolver() {
    }

    public static boolean isAvailable(IConfigBase config) {
        if (config == null) {
            return true;
        }

        if (config instanceof IConfigAvailability availability) {
            return availability.isAvailable();
        }

        if (!ToolConfigs.ENABLE_TWEAKER_MORE_COMPATIBILITY.getBooleanValue()) {
            return true;
        }

        Boolean cached = AVAILABILITY_CACHE.get(config);
        if (cached != null) {
            return cached;
        }

        boolean available = resolveTweakerMoreAvailability(config);
        AVAILABILITY_CACHE.put(config, available);
        return available;
    }

    public static boolean isUnavailable(IConfigBase config) {
        return !isAvailable(config);
    }

    private static boolean resolveTweakerMoreAvailability(IConfigBase config) {
        if (!ToolConfigs.ENABLE_TWEAKER_MORE_COMPATIBILITY.getBooleanValue()) {
            return true;
        }

        Optional<Method> method = TWEAKER_MORE_MODIFIER_METHOD.get(config.getClass());

        if (method.isEmpty()) {
            return true;
        }

        try {
            Object modifierObject = method.get().invoke(config);

            if (!(modifierObject instanceof Function<?, ?> modifier)) {
                return true;
            }

            @SuppressWarnings("unchecked")
            Object modifiedLine = ((Function<Object, ?>) modifier).apply(PROBE_LINE);
            return !(modifiedLine instanceof String line && line.startsWith(GuiBase.TXT_DARK_RED));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            if (REFLECTION_FAILURE_LOGGED.compareAndSet(false, true)) {
                NotAGoodModForSurvival.LOGGER.debug(
                        "Could not inspect an external conditional config availability.", exception);
            }
            return true;
        }
    }

    private static Optional<Method> findTweakerMoreModifier(Class<?> configClass) {
        Class<?> tweakerMoreConfigBase = findTweakerMoreConfigBase(configClass.getClassLoader());

        if (tweakerMoreConfigBase == null || !tweakerMoreConfigBase.isAssignableFrom(configClass)) {
            return Optional.empty();
        }

        Boolean knownConfigClass = TWEAKER_MORE_CONFIG_CLASSES.putIfAbsent(configClass, true);
        if (knownConfigClass == null) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "Detected TweakerMore conditional config class: {}.", configClass.getName());
        }

        try {
            return Optional.of(tweakerMoreConfigBase.getMethod(DISPLAY_LINE_MODIFIER));
        } catch (NoSuchMethodException | SecurityException exception) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "TweakerMore conditional config interface has no usable display modifier.", exception);
            return Optional.empty();
        }
    }

    private static Class<?> findTweakerMoreConfigBase(ClassLoader configClassLoader) {
        ClassLoader loader = configClassLoader != null
                ? configClassLoader : ConfigAvailabilityResolver.class.getClassLoader();

        try {
            return Class.forName(TWEAKER_MORE_CONFIG_BASE, false, loader);
        } catch (ClassNotFoundException exception) {
            if (MISSING_TWEAKER_MORE_LOGGED.compareAndSet(false, true)) {
                NotAGoodModForSurvival.LOGGER.debug(
                        "TweakerMore is not present; external conditional config support is disabled.");
            }
            return null;
        } catch (LinkageError | SecurityException exception) {
            if (MISSING_TWEAKER_MORE_LOGGED.compareAndSet(false, true)) {
                NotAGoodModForSurvival.LOGGER.debug(
                        "Could not load TweakerMore's conditional config interface.", exception);
            }
            return null;
        }
    }
}
