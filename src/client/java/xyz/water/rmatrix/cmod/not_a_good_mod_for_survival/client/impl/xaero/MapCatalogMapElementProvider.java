package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import xaero.map.element.MapElementRenderLocation;
import xaero.map.element.MapElementRenderProvider;

final class MapCatalogMapElementProvider extends MapElementRenderProvider<MapCatalogMapDisplayGroup, MapCatalogMapElementContext> {
    @Override
    public void begin(int location, MapCatalogMapElementContext context) {
        context.begin(location);
    }

    @Override
    public boolean hasNext(int location, MapCatalogMapElementContext context) {
        return location == MapElementRenderLocation.WORLD_MAP && context.hasNext();
    }

    @Override
    public MapCatalogMapDisplayGroup getNext(int location, MapCatalogMapElementContext context) {
        return context.next();
    }

    @Override
    public void end(int location, MapCatalogMapElementContext context) {
    }
}
