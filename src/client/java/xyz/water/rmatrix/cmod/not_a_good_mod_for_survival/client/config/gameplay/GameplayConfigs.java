package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Configuration options that directly change gameplay or editing behavior. */
public final class GameplayConfigs {
    private static final String SIGNS_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.signs";
    private static final String BRIDGING_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.bridging";
    private static final String MOVEMENT_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.movement";
    private static final String PROJECTION_AIDS_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.projectionAids";

    public static final class Signs {
        public static final ConfigBooleanHotkeyed ENABLE_LONG_SIGN_TEXT = new ConfigBooleanHotkeyed(
                "enableLongSignText", false, "",
                "Open a large, material-independent editor for long sign messages.")
                .apply(SIGNS_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_LONG_SIGN_TEXT
        );

        private Signs() {
        }
    }

    public static final class Bridging {
        public static final ConfigBooleanHotkeyed ENABLE_FORWARD_BRIDGING = new ConfigBooleanHotkeyed(
                "enableForwardBridging", false, "",
                "Place a block in the horizontal space directly in front of the block below you when aiming at it.")
                .apply(BRIDGING_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_FORWARD_BRIDGING
        );

        private Bridging() {
        }
    }

    public static final class Movement {
        public static final ConfigBooleanHotkeyed MORE_AGGRESSIVE_SPRINT = new ConfigBooleanHotkeyed(
                "moreAggressiveSprint", false, "",
                "Keep sprinting while moving forward even when hunger would normally prevent sprinting.")
                .apply(MOVEMENT_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                MORE_AGGRESSIVE_SPRINT
        );

        private Movement() {
        }
    }

    /**
     * Litematica projection building aids. Their data is only readable while Litematica
     * is loaded, but these settings stay plain booleans so they persist independently of
     * the optional integration and need no keybind of their own.
     */
    public static final class ProjectionAids {
        public static final ConfigBoolean ENABLE_ITEM_FRAME_EASY_PLACE = new ConfigBoolean(
                "enableItemFrameEasyPlace", false,
                "Place exactly one projected item frame per easy place click, where you click. Only frames Litematica can already be drawing are offered, so a part of the projection that has not rendered yet cannot be built through.")
                .apply(PROJECTION_AIDS_CONFIG_KEY);
        public static final ConfigBoolean ENABLE_ITEM_FRAME_AUTO_PRINT = new ConfigBoolean(
                "enableItemFrameAutoPrint", false,
                "Also print the framed item and its rotation with the same click: once the frame appears, the fill and every turn go out together as one ordered run of ordinary interactions. Needs those items in your hotbar.")
                .apply(PROJECTION_AIDS_CONFIG_KEY);
        public static final ConfigBoolean PLACE_ITEM_FRAME_ROTATION = new ConfigBoolean(
                "placeItemFrameItemRotation", true,
                "While auto-printing, also rotate the framed item to the rotation stored in the projection. Costs extra empty hand interactions, sent together with the fill.")
                .apply(PROJECTION_AIDS_CONFIG_KEY);
        public static final ConfigBoolean ENABLE_PROJECTION_CONTENT_PREVIEW = new ConfigBoolean(
                "enableProjectionContentPreview", false,
                "Press the projection content preview key (left shift and middle click by default) with an empty hand to preview the item a projected item frame holds, or to read the book on a projected lectern.")
                .apply(PROJECTION_AIDS_CONFIG_KEY);
        public static final ConfigBoolean ENABLE_PROJECTION_BOOK_COPY = new ConfigBoolean(
                "enableProjectionBookTextCopy", false,
                "While holding a writable book, press the projection content preview key on a projected lectern to copy the projection's book text into it.")
                .apply(PROJECTION_AIDS_CONFIG_KEY);
        public static final ConfigBoolean ENABLE_CREATIVE_FRAME_SUPPLY = new ConfigBoolean(
                "enableCreativeFrameSupply", false,
                "In creative mode, keep an item frame in the current hotbar slot and supply the exact framed item for auto-print when placing projected frames, and middle-click a projected item frame to pick the frame or its content with components.")
                .apply(PROJECTION_AIDS_CONFIG_KEY);
        public static final ConfigBoolean ENABLE_SURVIVAL_FRAME_SUPPLY = new ConfigBoolean(
                "enableSurvivalFrameSupply", false,
                "In survival mode, bring the item frame or the exact framed item from anywhere in your inventory to the current hotbar slot when placing or filling projected frames, and when middle-clicking a projected item frame.")
                .apply(PROJECTION_AIDS_CONFIG_KEY);
        public static final ConfigBoolean ENABLE_PROJECTION_CONTENT_HIGHLIGHT = new ConfigBoolean(
                "enableProjectionContentHighlight", false,
                "While a container screen is open, highlight the slots holding the item a projected item frame the crosshair is aiming at contains, using the Litematica pick-block highlight colour.")
                .apply(PROJECTION_AIDS_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_ITEM_FRAME_EASY_PLACE,
                ENABLE_ITEM_FRAME_AUTO_PRINT,
                PLACE_ITEM_FRAME_ROTATION,
                ENABLE_PROJECTION_CONTENT_PREVIEW,
                ENABLE_PROJECTION_BOOK_COPY,
                ENABLE_CREATIVE_FRAME_SUPPLY,
                ENABLE_SURVIVAL_FRAME_SUPPLY,
                ENABLE_PROJECTION_CONTENT_HIGHLIGHT
        );

        private ProjectionAids() {
        }
    }

    public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.<IConfigBase>builder()
            .addAll(Signs.OPTIONS)
            .addAll(Bridging.OPTIONS)
            .addAll(Movement.OPTIONS)
            .addAll(ProjectionAids.OPTIONS)
            .build();

    private GameplayConfigs() {
    }
}
