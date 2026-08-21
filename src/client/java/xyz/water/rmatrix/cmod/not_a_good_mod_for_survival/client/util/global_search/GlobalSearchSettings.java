package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search;

import net.minecraft.client.MinecraftClient;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.tools.ToolConfigs;

import java.util.Locale;

/** Runtime access to the user-facing global-search options. */
public final class GlobalSearchSettings {
    private GlobalSearchSettings() {
    }

    public static boolean isSearchHighlightEnabled() {
        return ToolConfigs.HIGHLIGHT_SEARCH_RESULTS.getBooleanValue();
    }

    public static boolean isJumpHighlightEnabled() {
        return ToolConfigs.HIGHLIGHT_JUMP_TARGET.getBooleanValue();
    }

    public static boolean isPinyinSearchEnabled() {
        return ToolConfigs.ENABLE_PINYIN_SEARCH.getBooleanValue();
    }

    public static boolean isSourceDisplayEnabled() {
        return ToolConfigs.SHOW_CONFIG_SOURCE.getBooleanValue();
    }

    public static boolean isCommentSearchEnabled() {
        return ToolConfigs.SEARCH_COMMENTS.getBooleanValue();
    }

    public static boolean isEnglishConfigNameSearchEnabled() {
        return !isChineseLanguage() ||
                ToolConfigs.SEARCH_ENGLISH_CONFIG_NAMES_IN_CHINESE.getBooleanValue();
    }

    private static boolean isChineseLanguage() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            String language = client.getLanguageManager().getLanguage();
            return language != null && language.toLowerCase(Locale.ROOT).startsWith("zh");
        } catch (RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.debug("Could not read the current language.", exception);
            return false;
        }
    }
}
