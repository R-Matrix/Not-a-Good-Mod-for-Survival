package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util;

import net.fabricmc.loader.api.FabricLoader;

/** Provides the runtime checks used by optional client integrations. */
public final class ModEnvironment {
    public static final String LITEMATICA_MOD_ID = "litematica";

    private ModEnvironment() {
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static boolean isLitematicaLoaded() {
        return isModLoaded(LITEMATICA_MOD_ID);
    }
}
