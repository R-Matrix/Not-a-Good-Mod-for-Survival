package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;

import java.util.ArrayList;
import java.util.List;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Hotkeys;

/** A compact malilib-style settings screen for this project. */
public final class GuiConfigs extends GuiConfigsBase {
    public GuiConfigs() {
        super(10, 50, NotAGoodModForSurvival.MOD_ID, null,
                "not-a-good-mod-for-survival.gui.title.configs", "1.0.0");
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<IConfigBase> configs = new ArrayList<>();
        configs.addAll(Configs.Test.OPTIONS);
        configs.addAll(Configs.DebugRender.OPTIONS);
        configs.addAll(Configs.Fireworks.OPTIONS);
        configs.addAll(Hotkeys.HOTKEY_LIST);
        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    protected boolean useKeybindSearch() {
        return true;
    }

    @Override
    protected int getConfigWidth() {
        return 220;
    }
}
