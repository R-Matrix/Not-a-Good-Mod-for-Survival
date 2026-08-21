package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.tools;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Configuration options for the global configuration search tools. */
public final class ToolConfigs {
    private static final String CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.globalSearch";

    public static final ConfigBoolean ENABLE_GLOBAL_MALILIB_SEARCH = new ConfigBoolean(
            "enableGlobalMalilibSearch", false,
            "Search Malilib configuration options from other mods in the All tab.")
            .apply(CONFIG_KEY);

    public static final ConfigBoolean HIGHLIGHT_SEARCH_RESULTS = new ConfigBoolean(
            "highlightSearchResults", true,
            "Highlight text matched by the global configuration search.")
            .apply(CONFIG_KEY);

    public static final ConfigBoolean HIGHLIGHT_JUMP_TARGET = new ConfigBoolean(
            "highlightJumpTarget", true,
            "Highlight the configuration option reached by a global-search jump.")
            .apply(CONFIG_KEY);

    public static final ConfigBoolean ENABLE_PINYIN_SEARCH = new ConfigBoolean(
            "enablePinyinSearch", true,
            "Allow global configuration search to match Chinese text by pinyin.")
            .apply(CONFIG_KEY);

    public static final ConfigBoolean SHOW_CONFIG_SOURCE = new ConfigBoolean(
            "showConfigSource", true,
            "Show the originating mod and category below global configuration options.")
            .apply(CONFIG_KEY);

    public static final ConfigBoolean SEARCH_ENGLISH_CONFIG_NAMES_IN_CHINESE = new ConfigBoolean(
            "searchEnglishConfigNamesInChinese", true,
            "When using a Chinese language, also search English configuration names.")
            .apply(CONFIG_KEY);

    public static final ConfigBoolean SEARCH_COMMENTS = new ConfigBoolean(
            "searchComments", true,
            "Include configuration comments in global search results.")
            .apply(CONFIG_KEY);

    public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
            ENABLE_GLOBAL_MALILIB_SEARCH,
            HIGHLIGHT_SEARCH_RESULTS,
            HIGHLIGHT_JUMP_TARGET,
            ENABLE_PINYIN_SEARCH,
            SHOW_CONFIG_SOURCE,
            SEARCH_ENGLISH_CONFIG_NAMES_IN_CHINESE,
            SEARCH_COMMENTS
    );

    private ToolConfigs() {
    }
}
