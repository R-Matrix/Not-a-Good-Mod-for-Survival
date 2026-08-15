package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class NotAGoodModForSurvivalClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MaterialHudController.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(MaterialHudController::tick);
    }
}
