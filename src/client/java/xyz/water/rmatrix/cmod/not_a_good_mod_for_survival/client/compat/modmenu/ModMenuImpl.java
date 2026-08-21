package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.compat.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.GuiConfigs;

/** Provides this mod's Malilib configuration screen to Mod Menu. */
public final class ModMenuImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            GuiConfigs screen = new GuiConfigs();
            screen.setParent(parent);
            return screen;
        };
    }
}
