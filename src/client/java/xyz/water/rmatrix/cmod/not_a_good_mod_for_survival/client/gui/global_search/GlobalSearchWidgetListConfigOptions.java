package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global_search;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;

import java.util.Collection;
import java.util.List;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search.GlobalSearchOption;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search.GlobalSearchQuery;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search.GlobalSearchRepository;

/** Malilib config list that applies the global query syntax and jump affordance. */
public final class GlobalSearchWidgetListConfigOptions extends WidgetListConfigOptions {
    public GlobalSearchWidgetListConfigOptions(
            int x,
            int y,
            int width,
            int height,
            int configWidth,
            float zLevel,
            GuiConfigsBase parent
    ) {
        super(x, y, width, height, configWidth, zLevel, false, parent);
    }

    @Override
    protected Collection<ConfigOptionWrapper> getAllEntries() {
        return GlobalSearchRepository.getEntries().stream()
                .map(entry -> (ConfigOptionWrapper) entry)
                .toList();
    }

    @Override
    protected void addFilteredContents(Collection<ConfigOptionWrapper> entries) {
        String filterText = this.getFilterText();

        if (filterText == null || filterText.isBlank()) {
            this.addNonFilteredContents(entries);
            return;
        }

        GlobalSearchQuery query = GlobalSearchQuery.parse(filterText);

        for (ConfigOptionWrapper entry : entries) {
            if (entry instanceof GlobalSearchOption globalEntry && query.matches(globalEntry)) {
                this.listContents.add(entry);
            }
        }
    }

    @Override
    protected void addNonFilteredContents(Collection<ConfigOptionWrapper> entries) {
        for (ConfigOptionWrapper entry : entries) {
            if (entry instanceof GlobalSearchOption globalEntry &&
                    !globalEntry.getMetadata().hasExternalSource()) {
                this.listContents.add(entry);
            }
        }
    }

    @Override
    protected WidgetConfigOption createListEntryWidget(
            int x,
            int y,
            int listIndex,
            boolean isOdd,
            ConfigOptionWrapper wrapper
    ) {
        if (wrapper instanceof GlobalSearchOption globalEntry) {
            int entryHeight = this.getBrowserEntryHeightFor(wrapper);
            List<String> highlightTerms = GlobalSearchQuery.parse(this.getFilterText()).getTextTerms();

            return new GlobalSearchWidgetConfigOption(
                    x, y, this.browserEntryWidth, entryHeight,
                    this.maxLabelWidth, this.configWidth, globalEntry, listIndex, this.parent, this,
                    highlightTerms);
        }

        return super.createListEntryWidget(x, y, listIndex, isOdd, wrapper);
    }

    @Override
    protected int getBrowserEntryHeightFor(ConfigOptionWrapper entry) {
        if (entry instanceof GlobalSearchOption globalEntry &&
                globalEntry.getMetadata().shouldShowSource()) {
            return 30;
        }

        return super.getBrowserEntryHeightFor(entry);
    }

    @Override
    public int getMaxNameLengthWrapped(List<ConfigOptionWrapper> wrappers) {
        int width = 0;

        for (ConfigOptionWrapper wrapper : wrappers) {
            if (wrapper.getConfig() != null) {
                width = Math.max(width, this.getStringWidth(wrapper.getConfig().getConfigGuiDisplayName()));
            }
        }

        return width;
    }
}
