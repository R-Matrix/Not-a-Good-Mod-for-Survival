package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** Malilib config list that applies the global query syntax and jump affordance. */
public final class GlobalWidgetListConfigOptions extends WidgetListConfigOptions {
    public GlobalWidgetListConfigOptions(
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
        return GlobalConfigRepository.getEntries().stream()
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
            if (entry instanceof GlobalConfigOptionWrapper globalEntry && query.matches(globalEntry)) {
                this.listContents.add(entry);
            }
        }
    }

    @Override
    protected void addNonFilteredContents(Collection<ConfigOptionWrapper> entries) {
        for (ConfigOptionWrapper entry : entries) {
            if (entry instanceof GlobalConfigOptionWrapper globalEntry &&
                    !globalEntry.getMetadata().shouldShowSource()) {
                this.listContents.add(entry);
            }
        }
    }

    @Override
    protected Comparator<ConfigOptionWrapper> getComparator() {
        return Comparator.comparing(entry -> {
            if (entry instanceof GlobalConfigOptionWrapper globalEntry) {
                return globalEntry.getMetadata().getModName() + "\u0000" + entry.getConfig().getName();
            }

            return entry.getConfig().getName();
        }, String.CASE_INSENSITIVE_ORDER);
    }

    @Override
    protected WidgetConfigOption createListEntryWidget(
            int x,
            int y,
            int listIndex,
            boolean isOdd,
            ConfigOptionWrapper wrapper
    ) {
        if (wrapper instanceof GlobalConfigOptionWrapper globalEntry) {
            int entryHeight = this.getBrowserEntryHeightFor(wrapper);
            List<String> highlightTerms = GlobalSearchQuery.parse(this.getFilterText()).getTextTerms();

            return new GlobalWidgetConfigOption(
                    x, y, this.browserEntryWidth, entryHeight,
                    this.maxLabelWidth, this.configWidth, globalEntry, listIndex, this.parent, this,
                    highlightTerms);
        }

        return super.createListEntryWidget(x, y, listIndex, isOdd, wrapper);
    }

    @Override
    protected int getBrowserEntryHeightFor(ConfigOptionWrapper entry) {
        if (entry instanceof GlobalConfigOptionWrapper globalEntry &&
                globalEntry.getMetadata().shouldShowSource()) {
            return 26;
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
