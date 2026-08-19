package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Configs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.Hotkeys;

/** A compact malilib-style settings screen for this project. */
public final class GuiConfigs extends GuiConfigsBase {
    private ConfigGuiTab selectedTab = ConfigGuiTab.ALL;

    public GuiConfigs() {
        super(10, 50, NotAGoodModForSurvival.MOD_ID, null,
                "not-a-good-mod-for-survival.gui.title.configs", "1.0.0");
    }

    @Override
    public void initGui() {
        super.initGui();
        this.clearOptions();

        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values()) {
            x += this.createButton(x, y, -1, tab);
        }
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab) {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(this.selectedTab != tab);
        this.addButton(button, new ButtonListener(tab, this));

        return button.getWidth() + 2;
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs() {
        List<? extends IConfigBase> configs = switch (this.selectedTab) {
            case ALL -> {
                List<IConfigBase> allConfigs = new ArrayList<>();
                allConfigs.addAll(Configs.Test.OPTIONS);
                allConfigs.addAll(Configs.DebugRender.OPTIONS);
                allConfigs.addAll(Configs.Fireworks.OPTIONS);
                allConfigs.addAll(Configs.Signs.OPTIONS);
                allConfigs.addAll(Hotkeys.HOTKEY_LIST);
                yield allConfigs;
            }
            case TEST -> Configs.Test.OPTIONS;
            case DEBUG_RENDER -> Configs.DebugRender.OPTIONS;
            case FIREWORKS -> Configs.Fireworks.OPTIONS;
            case SIGNS -> Configs.Signs.OPTIONS;
            case HOTKEYS -> Hotkeys.HOTKEY_LIST;
        };
        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    protected boolean useKeybindSearch() {
        return this.selectedTab == ConfigGuiTab.HOTKEYS;
    }

    @Override
    protected int getConfigWidth() {
        return 220;
    }

    private record ButtonListener(ConfigGuiTab tab, GuiConfigs parent) implements IButtonActionListener {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            this.parent.selectedTab = this.tab;
            this.parent.reCreateListWidget();
            Objects.requireNonNull(this.parent.getListWidget()).resetScrollbarPosition();
            this.parent.initGui();
        }
    }

    private enum ConfigGuiTab {
        ALL("not-a-good-mod-for-survival.gui.button.config_gui.all"),
        TEST("not-a-good-mod-for-survival.gui.button.config_gui.test"),
        DEBUG_RENDER("not-a-good-mod-for-survival.gui.button.config_gui.debug_render"),
        FIREWORKS("not-a-good-mod-for-survival.gui.button.config_gui.fireworks"),
        SIGNS("not-a-good-mod-for-survival.gui.button.config_gui.signs"),
        HOTKEYS("not-a-good-mod-for-survival.gui.button.config_gui.hotkeys");

        private final String translationKey;

        ConfigGuiTab(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getDisplayName() {
            return StringUtils.translate(this.translationKey);
        }
    }
}
