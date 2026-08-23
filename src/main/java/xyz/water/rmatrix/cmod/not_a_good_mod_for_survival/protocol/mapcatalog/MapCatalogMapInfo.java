package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

import net.minecraft.util.Identifier;

/** Immutable map metadata transported by MapCatalogSync. */
public record MapCatalogMapInfo(
        int mapId,
        Identifier dimension,
        int centerX,
        int centerZ,
        byte scale,
        boolean locked,
        MapCatalogClassification classification
) {
    public MapCatalogMapInfo {
        classification = classification == null
                ? new MapCatalogClassification(false, java.util.List.of())
                : classification;
    }
}
