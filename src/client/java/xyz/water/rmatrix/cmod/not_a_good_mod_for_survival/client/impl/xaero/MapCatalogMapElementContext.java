package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.minecraft.util.Identifier;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.mapcatalog.MapCatalogSyncClient;

import java.util.List;

final class MapCatalogMapElementContext {
    private List<MapCatalogMapDisplayGroup> groups = List.of();
    private Identifier dimension;
    private int nextIndex;

    void begin(int location) {
        nextIndex = 0;
        if (location != xaero.map.element.MapElementRenderLocation.WORLD_MAP
                || !RenderConfigs.MapCatalogMaps.ENABLE_MAP_CATALOG_DISPLAY.getBooleanValue()) {
            groups = List.of();
            dimension = null;
            return;
        }

        dimension = MapCatalogXaeroIntegration.getViewedDimension();
        if (dimension == null) {
            groups = List.of();
            return;
        }

        MapCatalogSyncClient.requestIfNeeded(dimension);
        groups = MapCatalogMapDisplayGroup.fromMaps(
                MapCatalogSyncClient.snapshot(),
                dimension,
                RenderConfigs.MapCatalogMaps.ONLY_LEVEL_ONE_MAPS.getBooleanValue(),
                RenderConfigs.MapCatalogMaps.ONLY_PLAYER_MAPS.getBooleanValue()
        );
    }

    boolean hasNext() {
        return nextIndex < groups.size();
    }

    MapCatalogMapDisplayGroup next() {
        return hasNext() ? groups.get(nextIndex++) : null;
    }

    Identifier dimension() {
        return dimension;
    }
}
