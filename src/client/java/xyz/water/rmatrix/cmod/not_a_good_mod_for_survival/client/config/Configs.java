package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Project-owned malilib configuration values and persistence. */
public final class Configs implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = NotAGoodModForSurvival.MOD_ID + ".json";
    private static final String TEST_KEY = NotAGoodModForSurvival.MOD_ID + ".config.test";

    public static final class Test {
        public static final ConfigBooleanHotkeyed TEST_BOOLEAN = new ConfigBooleanHotkeyed(
                "testBoolean", false, "", "A test boolean setting.").apply(TEST_KEY);
        public static final ConfigInteger TEST_INTEGER = new ConfigInteger(
                "testInteger", 0, 0, 100, "A test integer setting.").apply(TEST_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                TEST_BOOLEAN,
                TEST_INTEGER
        );

        private Test() {
        }
    }

    public static final class DebugRender {
        private static final String CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.debugRender";

        public static final ConfigBooleanHotkeyed ENTITY_VIEW_ARROW = new ConfigBooleanHotkeyed(
                "entityViewArrow", false, "",
                "Add an arrowhead to the entity view vector when F3+B hitboxes are visible.")
                .apply(CONFIG_KEY);
        public static final ConfigBooleanHotkeyed THICK_CHUNK_BORDER_LINES = new ConfigBooleanHotkeyed(
                "thickChunkBorderLines", false, "",
                "Use thicker red and blue lines for the F3+G chunk-border overlay.")
                .apply(CONFIG_KEY);
        public static final ConfigInteger CHUNK_BORDER_LINE_WIDTH = new ConfigInteger(
                "chunkBorderLineWidth", 4, 1, 10,
                "Width of the enhanced red and blue F3+G chunk-border lines, from 1 to 10.")
                .apply(CONFIG_KEY);
        public static final ConfigBooleanHotkeyed SHOW_OCCLUDED_CURRENT_SUBCHUNK_BLUE_LINES = new ConfigBooleanHotkeyed(
                "occludedCurrentSubchunkBlueLines", false, "",
                "Keep the current subchunk's blue frame visible through blocks as thin lines.")
                .apply(CONFIG_KEY);
        public static final ConfigBooleanHotkeyed HIDE_INVENTORY_PLAYER_MODEL_HITBOX = new ConfigBooleanHotkeyed(
                "hideInventoryPlayerModelHitbox", false, "",
                "Hide the player's collision box while rendering the player model in the inventory screen.")
                .apply(CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENTITY_VIEW_ARROW,
                THICK_CHUNK_BORDER_LINES,
                CHUNK_BORDER_LINE_WIDTH,
                SHOW_OCCLUDED_CURRENT_SUBCHUNK_BLUE_LINES,
                HIDE_INVENTORY_PLAYER_MODEL_HITBOX
        );

        private DebugRender() {
        }
    }

    public static final class Fireworks {
        private static final String CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.fireworks";

        /** -1 keeps vanilla's max-age/2 behavior; non-negative values are absolute particle ages in ticks. */
        public static final ConfigInteger FADE_START_TICK = new ConfigInteger(
                "fadeStartTick", -1, -1, 59,
                "The tick at which firework spark color and alpha fading starts. -1 uses vanilla timing.")
                .apply(CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                FADE_START_TICK
        );

        private Fireworks() {
        }
    }

    public static final class Signs {
        private static final String CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.signs";

        public static final ConfigBooleanHotkeyed ENLARGE_SINGLE_CHARACTER = new ConfigBooleanHotkeyed(
                "enlargeSingleCharacter", true, "",
                "Enlarge and center a sign face when it contains exactly one visible character.")
                .apply(CONFIG_KEY);
        public static final ConfigDouble SINGLE_CHARACTER_SCALE = new ConfigDouble(
                "singleCharacterScale", 2.0D, 1.0D, 4.0D, true,
                "Scale applied to a sign face containing exactly one visible character, from 1.0 to 4.0.") {
            @Override
            protected double getClampedValue(double value) {
                double steppedValue = Math.round(value * 10.0D) / 10.0D;
                return super.getClampedValue(steppedValue);
            }
        }.apply(CONFIG_KEY);
        public static final ConfigDouble SINGLE_CHARACTER_VERTICAL_OFFSET = new ConfigDouble(
                "singleCharacterVerticalOffset", 0.0D, -8.0D, 8.0D, true,
                "Vertical offset for a single-character sign face. Positive values move the character down, from -8.0 to 8.0.") {
            @Override
            protected double getClampedValue(double value) {
                double steppedValue = Math.round(value * 10.0D) / 10.0D;
                return super.getClampedValue(steppedValue);
            }
        }.apply(CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENLARGE_SINGLE_CHARACTER,
                SINGLE_CHARACTER_SCALE,
                SINGLE_CHARACTER_VERTICAL_OFFSET
        );

        private Signs() {
        }
    }

    /** All boolean configuration toggles, registered so assigned keys can trigger them in-game. */
    public static final List<IHotkey> BOOLEAN_HOTKEY_LIST = ImmutableList.of(
            Test.TEST_BOOLEAN,
            DebugRender.ENTITY_VIEW_ARROW,
            DebugRender.THICK_CHUNK_BORDER_LINES,
            DebugRender.SHOW_OCCLUDED_CURRENT_SUBCHUNK_BLUE_LINES
    );

    public static final Configs INSTANCE = new Configs();

    private Configs() {
    }

    public static void loadFromFile() {
        Path configFile = FileUtils.getConfigDirectoryAsPath().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile) && Files.isReadable(configFile)) {
            JsonElement element = JsonUtils.parseJsonFileAsPath(configFile);

            if (element != null && element.isJsonObject()) {
                JsonObject root = element.getAsJsonObject();
                ConfigUtils.readConfigBase(root, "Test", Test.OPTIONS);
                ConfigUtils.readConfigBase(root, "DebugRender", DebugRender.OPTIONS);
                ConfigUtils.readConfigBase(root, "Fireworks", Fireworks.OPTIONS);
                ConfigUtils.readConfigBase(root, "Signs", Signs.OPTIONS);
                ConfigUtils.readConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            } else {
                NotAGoodModForSurvival.LOGGER.error(
                        "Failed to load configuration file '{}'.", configFile.toAbsolutePath());
            }
        }
    }

    public static void saveToFile() {
        Path directory = FileUtils.getConfigDirectoryAsPath();

        if (!Files.exists(directory)) {
            FileUtils.createDirectoriesIfMissing(directory);
        }

        if (Files.isDirectory(directory)) {
            JsonObject root = new JsonObject();
            ConfigUtils.writeConfigBase(root, "Test", Test.OPTIONS);
            ConfigUtils.writeConfigBase(root, "DebugRender", DebugRender.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Fireworks", Fireworks.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Signs", Signs.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            JsonUtils.writeJsonToFileAsPath(root, directory.resolve(CONFIG_FILE_NAME));
        } else {
            NotAGoodModForSurvival.LOGGER.error(
                    "Configuration path '{}' is not a directory.", directory.toAbsolutePath());
        }
    }

    @Override
    public void load() {
        loadFromFile();
    }

    @Override
    public void save() {
        saveToFile();
    }
}
