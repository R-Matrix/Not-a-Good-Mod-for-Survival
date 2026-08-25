package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.minecraft.util.Identifier;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.mapcatalog.MapCatalogSyncClient;

import java.util.List;

final class MapCatalogBannerElementContext {
    private List<MapCatalogBannerDisplay> banners = List.of();
    private Identifier dimension;
    private int nextIndex;

    void begin(int location) {
        nextIndex = 0;
        if (location != xaero.map.element.MapElementRenderLocation.WORLD_MAP
                || !RenderConfigs.MapCatalogMaps.ENABLE_MAP_CATALOG_DISPLAY.getBooleanValue()
                || !RenderConfigs.MapCatalogMaps.SHOW_MAP_BANNERS.getBooleanValue()) {
            banners = List.of();
            dimension = null;
            return;
        }

        dimension = MapCatalogXaeroIntegration.getViewedDimension();
        if (dimension == null) {
            banners = List.of();
            return;
        }

        MapCatalogSyncClient.requestIfNeeded(dimension);
        banners = MapCatalogBannerDisplay.fromMaps(
                MapCatalogSyncClient.snapshot(),
                dimension,
                RenderConfigs.MapCatalogMaps.ONLY_LEVEL_ONE_MAPS.getBooleanValue(),
                RenderConfigs.MapCatalogMaps.ONLY_PLAYER_MAPS.getBooleanValue()
        );
    }

    boolean hasNext() {
        return nextIndex < banners.size();
    }

    MapCatalogBannerDisplay next() {
        return hasNext() ? banners.get(nextIndex++) : null;
    }

    Identifier dimension() {
        return dimension;
    }
}
