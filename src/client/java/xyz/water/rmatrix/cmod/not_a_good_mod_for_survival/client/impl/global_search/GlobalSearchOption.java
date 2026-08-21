package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;

/** A Malilib config wrapper carrying the source metadata needed by global search. */
public final class GlobalSearchOption extends ConfigOptionWrapper {
    private final IConfigBase config;
    private final GlobalSearchMetadata metadata;

    public GlobalSearchOption(IConfigBase config, GlobalSearchMetadata metadata) {
        super(config);
        this.config = config;
        this.metadata = metadata;
    }

    @Override
    public IConfigBase getConfig() {
        return this.config;
    }

    public GlobalSearchMetadata getMetadata() {
        return this.metadata;
    }
}
