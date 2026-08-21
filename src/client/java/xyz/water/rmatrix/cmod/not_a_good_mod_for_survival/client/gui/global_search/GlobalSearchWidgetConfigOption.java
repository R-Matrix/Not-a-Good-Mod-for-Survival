package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global_search;

import java.util.List;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetLabel;
import fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import net.minecraft.client.gui.DrawContext;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search.GlobalSearchOption;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search.GlobalSearchQuery;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search.GlobalSearchSettings;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search.GlobalSearchText;

/** Standard Malilib config row with source information and text-match highlighting. */
public final class GlobalSearchWidgetConfigOption extends WidgetConfigOption {
    private static final float SOURCE_SCALE = 0.75F;
    static final int MATCH_BACKGROUND = 0xD0D99000;
    private static final int SOURCE_COLOR = 0xFF808080;

    private final GlobalSearchOption globalWrapper;
    private final String configName;
    private final List<String> highlightTerms;

    public GlobalSearchWidgetConfigOption(
            int x,
            int y,
            int width,
            int height,
            int labelWidth,
            int configWidth,
            GlobalSearchOption wrapper,
            int listIndex,
            GuiConfigsBase host,
            WidgetListConfigOptionsBase<?, ?> parent,
            List<String> highlightTerms
    ) {
        super(x, y, width, height, labelWidth, configWidth, wrapper, listIndex, host, parent);
        this.globalWrapper = wrapper;
        this.configName = wrapper.getConfig().getConfigGuiDisplayName();
        this.highlightTerms = List.copyOf(highlightTerms);

        // WidgetConfigOption adds the visible name first. Remove only that label;
        // the comment hover widget and all interactive controls remain untouched.
        this.subWidgets.removeIf(widget -> widget instanceof WidgetLabel);
        this.replaceCommentHoverWidget();

        if (wrapper.getMetadata().getConfigScreenSupplier() != null) {
            this.addWidget(new GlobalSearchJumpWidget(
                    this.x + this.width - 14, this.y + 1, 14, 20,
                    wrapper.getMetadata(), wrapper.getConfig()));
        }
    }

    private void replaceCommentHoverWidget() {
        WidgetHoverInfo original = null;

        for (WidgetBase widget : this.subWidgets) {
            if (widget instanceof WidgetHoverInfo hoverInfo) {
                original = hoverInfo;
                break;
            }
        }

        if (original != null) {
            this.subWidgets.remove(original);
            this.addWidget(new GlobalSearchCommentHoverWidget(
                    original.getX(), original.getY(), original.getWidth(), original.getHeight(),
                    original.getLines(), this.highlightTerms));
        }
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext) {
        super.render(mouseX, mouseY, selected, drawContext);
        this.renderConfigName(drawContext);

        if (this.globalWrapper.getMetadata().shouldShowSource()) {
            this.renderSource(drawContext);
        }
    }

    private void renderConfigName(DrawContext drawContext) {
        if (!GlobalSearchSettings.isSearchHighlightEnabled()) {
            this.drawStringWithShadow(this.x, this.y + 8, 0xFFFFFFFF, this.configName, drawContext);
            return;
        }

        for (String term : this.highlightTerms) {
            for (GlobalSearchText.Match match : GlobalSearchText.findMatches(this.configName, term)) {
                int startX = this.x + this.getStringWidth(this.configName.substring(0, match.start()));
                int endX = this.x + this.getStringWidth(this.configName.substring(0, match.end()));
                drawContext.fill(startX, this.y + 6, endX, this.y + 16, MATCH_BACKGROUND);
            }
        }

        this.drawStringWithShadow(this.x, this.y + 8, 0xFFFFFFFF, this.configName, drawContext);
    }

    private void renderSource(DrawContext drawContext) {
        int sourceX = this.x;
        int sourceRight = this.x + this.width - 18;
        int availableWidth = sourceRight - sourceX;

        if (availableWidth <= 0) {
            return;
        }

        String source = this.globalWrapper.getMetadata().getSourceDisplayName();

        if (source.isEmpty()) {
            return;
        }

        int sourceWidth = this.getStringWidth(source);
        float scale = sourceWidth > 0
                ? Math.min(SOURCE_SCALE, availableWidth / (float) sourceWidth)
                : SOURCE_SCALE;

        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, 1.0F);
        this.drawStringWithShadow(
                Math.round(sourceX / scale),
                Math.round((this.y + 21) / scale),
                SOURCE_COLOR, source, drawContext);
        matrices.pop();
    }
}
