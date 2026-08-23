package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

import java.util.List;

/** Classification metadata transported by MapCatalogSync. */
public record MapCatalogClassification(
        boolean hasExplorationMarker,
        List<MapCatalogBanner> banners
) {
    public MapCatalogClassification {
        banners = banners == null ? List.of() : List.copyOf(banners);
    }
}
