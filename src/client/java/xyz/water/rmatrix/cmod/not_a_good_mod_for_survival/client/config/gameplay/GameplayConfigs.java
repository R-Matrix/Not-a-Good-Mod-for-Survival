package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Configuration options that directly change gameplay or editing behavior. */
public final class GameplayConfigs {
    private static final String SIGNS_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.signs";
    private static final String BRIDGING_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.bridging";
    private static final String MOVEMENT_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.movement";

    public static final class Signs {
        public static final ConfigBoolean ENABLE_LONG_SIGN_TEXT = new ConfigBoolean(
                "enableLongSignText", false,
                "Open a large, material-independent editor for long sign messages.")
                .apply(SIGNS_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_LONG_SIGN_TEXT
        );

        private Signs() {
        }
    }

    public static final class Bridging {
        public static final ConfigBoolean ENABLE_FORWARD_BRIDGING = new ConfigBoolean(
                "enableForwardBridging", false,
                "Place a block in the horizontal space directly in front of the block below you when aiming at it.")
                .apply(BRIDGING_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_FORWARD_BRIDGING
        );

        private Bridging() {
        }
    }

    public static final class Movement {
        public static final ConfigBoolean MORE_AGGRESSIVE_SPRINT = new ConfigBoolean(
                "moreAggressiveSprint", false,
                "Keep sprinting while moving forward even when hunger would normally prevent sprinting.")
                .apply(MOVEMENT_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                MORE_AGGRESSIVE_SPRINT
        );

        private Movement() {
        }
    }

    public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.<IConfigBase>builder()
            .addAll(Signs.OPTIONS)
            .addAll(Bridging.OPTIONS)
            .addAll(Movement.OPTIONS)
            .build();

    private GameplayConfigs() {
    }
}
