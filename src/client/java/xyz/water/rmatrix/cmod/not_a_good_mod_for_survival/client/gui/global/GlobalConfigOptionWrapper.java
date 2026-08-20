package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;

/** A Malilib config wrapper carrying the source metadata needed by global search. */
public final class GlobalConfigOptionWrapper extends ConfigOptionWrapper {
    private final IConfigBase config;
    private final GlobalConfigMetadata metadata;

    public GlobalConfigOptionWrapper(IConfigBase config, GlobalConfigMetadata metadata) {
        super(config);
        this.config = config;
        this.metadata = metadata;
    }

    @Override
    public IConfigBase getConfig() {
        return this.config;
    }

    public GlobalConfigMetadata getMetadata() {
        return this.metadata;
    }
}
