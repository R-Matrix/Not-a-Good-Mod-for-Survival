package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Project-owned malilib configuration values and persistence. */
public final class Configs implements IConfigHandler {
    private static final String CONFIG_FILE_NAME = NotAGoodModForSurvival.MOD_ID + ".json";
    private static final String TEST_KEY = NotAGoodModForSurvival.MOD_ID + ".config.test";

    public static final class Test {
        public static final ConfigBoolean TEST_BOOLEAN = new ConfigBoolean(
                "testBoolean", false, "A test boolean setting.").apply(TEST_KEY);
        public static final ConfigInteger TEST_INTEGER = new ConfigInteger(
                "testInteger", 0, 0, 100, "A test integer setting.").apply(TEST_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                TEST_BOOLEAN,
                TEST_INTEGER
        );

        private Test() {
        }
    }

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
