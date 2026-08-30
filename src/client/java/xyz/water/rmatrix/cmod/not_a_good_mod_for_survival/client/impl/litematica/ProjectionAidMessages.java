package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import fi.dy.masa.malilib.util.InfoUtils;

/** Action bar feedback for the Litematica projection building aids. */
public final class ProjectionAidMessages {
    private static final String PREFIX = "not-a-good-mod-for-survival.message.projection.";

    private ProjectionAidMessages() {
    }

    /** Prints a translatable action bar message below the crosshair. */
    public static void print(String key, Object... args) {
        InfoUtils.printActionbarMessage(PREFIX + key, args);
    }
}
