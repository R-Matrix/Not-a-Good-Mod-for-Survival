package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import fi.dy.masa.malilib.gui.GuiBase;

/** Selects the tab that owns a config on a newly created Malilib screen. */
@FunctionalInterface
interface GlobalConfigTabTarget {
    boolean select(GuiBase screen);
}
