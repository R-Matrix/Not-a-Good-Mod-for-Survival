package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Configuration options that change rendered or visual output. */
public final class RenderConfigs {
    private static final String DEBUG_RENDER_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.debugRender";
    private static final String FIREWORKS_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.fireworks";
    private static final String SIGNS_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.signs";
    private static final String MAP_CATALOG_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.mapCatalogMaps";
    private static final String SCHEMATIC_RENDER_RANGE_CONFIG_KEY = NotAGoodModForSurvival.MOD_ID + ".config.schematicRenderRange";

    public static final class DebugRender {
        public static final ConfigBooleanHotkeyed ENTITY_VIEW_ARROW = new ConfigBooleanHotkeyed(
                "entityViewArrow", false, "",
                "Add an arrowhead to the entity view vector when F3+B hitboxes are visible.")
                .apply(DEBUG_RENDER_CONFIG_KEY);
        public static final ConfigBooleanHotkeyed THICK_CHUNK_BORDER_LINES = new ConfigBooleanHotkeyed(
                "thickChunkBorderLines", false, "",
                "Use thicker red and blue lines for the F3+G chunk-border overlay.")
                .apply(DEBUG_RENDER_CONFIG_KEY);
        public static final ConfigInteger CHUNK_BORDER_LINE_WIDTH = new ConfigInteger(
                "chunkBorderLineWidth", 4, 1, 10,
                "Width of the enhanced red and blue F3+G chunk-border lines, from 1 to 10.")
                .apply(DEBUG_RENDER_CONFIG_KEY);
        public static final ConfigBooleanHotkeyed SHOW_OCCLUDED_CURRENT_SUBCHUNK_BLUE_LINES = new ConfigBooleanHotkeyed(
                "occludedCurrentSubchunkBlueLines", false, "",
                "Keep the current subchunk's blue frame visible through blocks as thin lines.")
                .apply(DEBUG_RENDER_CONFIG_KEY);
        public static final ConfigBooleanHotkeyed HIDE_INVENTORY_PLAYER_MODEL_HITBOX = new ConfigBooleanHotkeyed(
                "hideInventoryPlayerModelHitbox", false, "",
                "Hide the player's collision box while rendering the player model in the inventory screen.")
                .apply(DEBUG_RENDER_CONFIG_KEY);

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
        /** -1 keeps vanilla's max-age/2 behavior; non-negative values are absolute particle ages in ticks. */
        public static final ConfigInteger FADE_START_TICK = new ConfigInteger(
                "fadeStartTick", -1, -1, 59,
                "The tick at which firework spark color and alpha fading starts. -1 uses vanilla timing.")
                .apply(FIREWORKS_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                FADE_START_TICK
        );

        private Fireworks() {
        }
    }

    public static final class Signs {
        public static final ConfigBooleanHotkeyed ENLARGE_SINGLE_CHARACTER = new ConfigBooleanHotkeyed(
                "enlargeSingleCharacter", false, "",
                "Enlarge and center a sign face when it contains exactly one visible character.")
                .apply(SIGNS_CONFIG_KEY);
        public static final ConfigDouble SINGLE_CHARACTER_SCALE = new ConfigDouble(
                "singleCharacterScale", 2.0D, 1.0D, 4.0D, true,
                "Scale applied to a sign face containing exactly one visible character, from 1.0 to 4.0.") {
            @Override
            protected double getClampedValue(double value) {
                double steppedValue = Math.round(value * 10.0D) / 10.0D;
                return super.getClampedValue(steppedValue);
            }
        }.apply(SIGNS_CONFIG_KEY);
        public static final ConfigDouble SINGLE_CHARACTER_VERTICAL_OFFSET = new ConfigDouble(
                "singleCharacterVerticalOffset", 0.0D, -8.0D, 8.0D, true,
                "Vertical offset for a single-character sign face. Positive values move the character down, from -8.0 to 8.0.") {
            @Override
            protected double getClampedValue(double value) {
                double steppedValue = Math.round(value * 10.0D) / 10.0D;
                return super.getClampedValue(steppedValue);
            }
        }.apply(SIGNS_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENLARGE_SINGLE_CHARACTER,
                SINGLE_CHARACTER_SCALE,
                SINGLE_CHARACTER_VERTICAL_OFFSET
        );

        private Signs() {
        }
    }

    public static final class MapCatalogMaps {
        public static final ConfigBoolean ENABLE_MAP_CATALOG_DISPLAY = new ConfigBoolean(
                "enableMapCatalogDisplay", false,
                "Display synchronized server map numbers and coverage borders in Xaero's World Map.\n\n"
                        + "§6NOTE: This feature requires the §dMapCatalog map synchronization protocol§6 "
                        + "to be enabled on the server.§r")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigBoolean ONLY_LEVEL_ONE_MAPS = new ConfigBoolean(
                "onlyLevelOneMaps", true,
                "Only display maps at scale level 1, covering 8x8 chunks.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigBoolean ONLY_PLAYER_MAPS = new ConfigBoolean(
                "onlyPlayerMaps", true,
                "Hide maps that contain exploration-style map decorations.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigBoolean SHOW_MAP_NUMBERS = new ConfigBoolean(
                "showMapNumbers", true,
                "Show compressed map numbers for each map coverage group.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigBoolean RENDER_MAP_RANGES = new ConfigBoolean(
                "renderMapRanges", true,
                "Render the coverage range for each map coverage group.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigBoolean SHOW_MAP_BANNERS = new ConfigBoolean(
                "showMapBanners", true,
                "Display banner decorations from synchronized maps in Xaero's World Map.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigColor MAP_BORDER_LEVEL_1_COLOR = new ConfigColor(
                "mapBorderLevel1Color", "#FFFFB52E",
                "Coverage border color for map scale level 1.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigColor MAP_BORDER_LEVEL_2_COLOR = new ConfigColor(
                "mapBorderLevel2Color", "#FF55FFFF",
                "Coverage border color for map scale level 2.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigColor MAP_BORDER_LEVEL_3_COLOR = new ConfigColor(
                "mapBorderLevel3Color", "#FF55FF55",
                "Coverage border color for map scale level 3.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigColor MAP_BORDER_LEVEL_4_COLOR = new ConfigColor(
                "mapBorderLevel4Color", "#FFFF55FF",
                "Coverage border color for map scale level 4.")
                .apply(MAP_CATALOG_CONFIG_KEY);
        public static final ConfigColor MAP_BORDER_LEVEL_5_COLOR = new ConfigColor(
                "mapBorderLevel5Color", "#FFFF5555",
                "Coverage border color for map scale level 5.")
                .apply(MAP_CATALOG_CONFIG_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE_MAP_CATALOG_DISPLAY,
                ONLY_LEVEL_ONE_MAPS,
                ONLY_PLAYER_MAPS,
                SHOW_MAP_NUMBERS,
                RENDER_MAP_RANGES,
                SHOW_MAP_BANNERS,
                MAP_BORDER_LEVEL_1_COLOR,
                MAP_BORDER_LEVEL_2_COLOR,
                MAP_BORDER_LEVEL_3_COLOR,
                MAP_BORDER_LEVEL_4_COLOR,
                MAP_BORDER_LEVEL_5_COLOR
        );

        private MapCatalogMaps() {
        }
    }

    public static final class SchematicRenderRange {
        public static final ConfigBoolean ENABLE = new ConfigBoolean(
                "enableSchematicRenderRange", false,
                "Limit the selected Litematica projection to its saved display range. Easy place, "
                + "verification and material lists only follow the range while the projection uses the "
                + "render-layers mode.")
                .apply(SCHEMATIC_RENDER_RANGE_CONFIG_KEY);
        public static final ConfigOptionList CORNER_EDIT_MODE = new ConfigOptionList(
                "schematicRenderRangeCornerEditMode", CornerEditMode.CORNERS,
                "Interaction mode for adjusting the range corners: corner picking or expand-to-contain.")
                .apply(SCHEMATIC_RENDER_RANGE_CONFIG_KEY);
        public static final ConfigColor OUTLINE_COLOR = new ConfigColor(
                "schematicRenderRangeOutlineColor", "#FFFFE100",
                "Color of the selected projection display-range outline.")
                .apply(SCHEMATIC_RENDER_RANGE_CONFIG_KEY);
        public static final ConfigColor SURFACE_COLOR = new ConfigColor(
                "schematicRenderRangeSurfaceColor", "#2AFFE100",
                "Translucent surface color of the selected projection display range.")
                .apply(SCHEMATIC_RENDER_RANGE_CONFIG_KEY);
        public static final ConfigDouble OUTLINE_LINE_WIDTH = new ConfigDouble(
                "schematicRenderRangeOutlineLineWidth", 2.0D, 1.0D, 6.0D, true,
                "Line width of the selected projection display-range outline, from 1.0 to 6.0.") {
            @Override
            protected double getClampedValue(double value) {
                double steppedValue = Math.round(value * 10.0D) / 10.0D;
                return super.getClampedValue(steppedValue);
            }
        }.apply(SCHEMATIC_RENDER_RANGE_CONFIG_KEY);
        public static final ConfigColor CORNER_1_COLOR = new ConfigColor(
                "schematicRenderRangeCorner1Color", "#AFFF5555",
                "Marker color for the first corner of the selected projection display range.")
                .apply(SCHEMATIC_RENDER_RANGE_CONFIG_KEY);
        public static final ConfigColor CORNER_2_COLOR = new ConfigColor(
                "schematicRenderRangeCorner2Color", "#AF55FF55",
                "Marker color for the second corner of the selected projection display range.")
                .apply(SCHEMATIC_RENDER_RANGE_CONFIG_KEY);
        public static CornerEditMode getCornerEditMode() {
            return (CornerEditMode) CORNER_EDIT_MODE.getOptionListValue();
        }

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                ENABLE,
                CORNER_EDIT_MODE,
                OUTLINE_COLOR,
                SURFACE_COLOR,
                OUTLINE_LINE_WIDTH,
                CORNER_1_COLOR,
                CORNER_2_COLOR
        );

        private SchematicRenderRange() {
        }
    }

    public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.<IConfigBase>builder()
            .addAll(DebugRender.OPTIONS)
            .addAll(Fireworks.OPTIONS)
            .addAll(Signs.OPTIONS)
            .addAll(MapCatalogMaps.OPTIONS)
            .addAll(SchematicRenderRange.OPTIONS)
            .build();

    private RenderConfigs() {
    }
}
