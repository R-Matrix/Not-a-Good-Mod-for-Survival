package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import xaero.map.WorldMap;
import xaero.map.WorldMapSession;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;

/** Optional Xaero World Map adapter for MapCatalogSync metadata. */
public final class MapCatalogXaeroIntegration {
    private static boolean registered;
    private static boolean installed;
    private static boolean viewedDimensionFailureLogged;
    private static MapCatalogMapElementRenderer renderer;
    private static MapCatalogBannerElementRenderer bannerRenderer;

    private MapCatalogXaeroIntegration() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        ClientTickEvents.END_CLIENT_TICK.register(MapCatalogXaeroIntegration::tryInstall);
    }

    public static Identifier getViewedDimension() {
        try {
            WorldMapSession session = WorldMapSession.getCurrentSession();
            if (session == null || session.getMapProcessor().getMapWorld().getCurrentDimensionId() == null) {
                return null;
            }
            return session.getMapProcessor().getMapWorld().getCurrentDimensionId().getValue();
        } catch (LinkageError | RuntimeException exception) {
            if (!viewedDimensionFailureLogged) {
                viewedDimensionFailureLogged = true;
                NotAGoodModForSurvival.LOGGER.debug(
                        "Could not read the current Xaero World Map dimension for MapCatalogSync rendering.",
                        exception);
            }
            return null;
        }
    }

    private static void tryInstall(MinecraftClient client) {
        if (installed) {
            return;
        }
        try {
            if (WorldMap.mapElementRenderHandler == null) {
                return;
            }
            MapCatalogMapElementContext context = new MapCatalogMapElementContext();
            MapCatalogMapElementProvider provider = new MapCatalogMapElementProvider();
            MapCatalogMapElementReader reader = new MapCatalogMapElementReader();
            renderer = new MapCatalogMapElementRenderer(context, provider, reader);
            WorldMap.mapElementRenderHandler.add(renderer);
            MapCatalogBannerElementContext bannerContext = new MapCatalogBannerElementContext();
            MapCatalogBannerElementProvider bannerProvider = new MapCatalogBannerElementProvider();
            MapCatalogBannerElementReader bannerReader = new MapCatalogBannerElementReader();
            bannerRenderer = new MapCatalogBannerElementRenderer(bannerContext, bannerProvider, bannerReader);
            WorldMap.mapElementRenderHandler.add(bannerRenderer);
            installed = true;
            NotAGoodModForSurvival.LOGGER.info("MapCatalogSync Xaero World Map integration initialized.");
        } catch (LinkageError | RuntimeException exception) {
            NotAGoodModForSurvival.LOGGER.warn(
                    "Xaero World Map was detected, but the optional MapCatalogSync renderer could not be initialized.",
                    exception);
            installed = true;
        }
    }
}
