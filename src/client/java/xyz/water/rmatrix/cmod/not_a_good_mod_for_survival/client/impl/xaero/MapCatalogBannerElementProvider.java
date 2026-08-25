package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import xaero.map.element.MapElementRenderLocation;
import xaero.map.element.MapElementRenderProvider;

final class MapCatalogBannerElementProvider extends MapElementRenderProvider<MapCatalogBannerDisplay, MapCatalogBannerElementContext> {
    @Override
    public void begin(int location, MapCatalogBannerElementContext context) {
        context.begin(location);
    }

    @Override
    public boolean hasNext(int location, MapCatalogBannerElementContext context) {
        return location == MapElementRenderLocation.WORLD_MAP && context.hasNext();
    }

    @Override
    public MapCatalogBannerDisplay getNext(int location, MapCatalogBannerElementContext context) {
        return context.next();
    }

    @Override
    public void end(int location, MapCatalogBannerElementContext context) {
    }
}
