package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Interaction model for adjusting the display-range corners. */
public enum CornerEditMode implements IConfigOptionListEntry {
    CORNERS("corners", NotAGoodModForSurvival.MOD_ID + ".enum.schematic_render_range.corner_edit_mode.corners"),
    EXPAND("expand", NotAGoodModForSurvival.MOD_ID + ".enum.schematic_render_range.corner_edit_mode.expand");

    private final String configString;
    private final String translationKey;

    CornerEditMode(String configString, String translationKey) {
        this.configString = configString;
        this.translationKey = translationKey;
    }

    @Override
    public String getStringValue() {
        return this.configString;
    }

    @Override
    public String getDisplayName() {
        return StringUtils.translate(this.translationKey);
    }

    @Override
    public CornerEditMode cycle(boolean forward) {
        int next = this.ordinal() + (forward ? 1 : -1);
        if (next < 0) {
            next = values().length - 1;
        }
        return values()[next % values().length];
    }

    @Override
    public CornerEditMode fromString(String name) {
        for (CornerEditMode mode : values()) {
            if (mode.configString.equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return CORNERS;
    }
}
