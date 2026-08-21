package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client;

import net.fabricmc.api.ClientModInitializer;
import fi.dy.masa.malilib.event.InitializationHandler;

public class NotAGoodModForSurvivalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        InitializationHandler.getInstance().registerInitializationHandler(new ClientInitHandler());
    }
}
