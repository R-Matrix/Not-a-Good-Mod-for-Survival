package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client;

import net.fabricmc.api.ClientModInitializer;
import fi.dy.masa.malilib.event.InitializationHandler;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.mapcatalog.MapCatalogSyncClient;

public class NotAGoodModForSurvivalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
		MapCatalogSyncClient.init();
        InitializationHandler.getInstance().registerInitializationHandler(new ClientInitHandler());
    }
}
