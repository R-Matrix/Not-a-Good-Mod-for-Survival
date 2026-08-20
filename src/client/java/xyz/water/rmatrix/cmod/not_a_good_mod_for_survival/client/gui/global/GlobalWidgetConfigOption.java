package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetLabel;
import fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import net.minecraft.client.gui.DrawContext;

/** Standard Malilib config row with source information and text-match highlighting. */
public final class GlobalWidgetConfigOption extends WidgetConfigOption {
    private static final float SOURCE_SCALE = 0.75F;
    static final int MATCH_BACKGROUND = 0xD0D99000;
    private static final int SOURCE_COLOR = 0xFF808080;

    private final int labelWidth;
    private final GlobalConfigOptionWrapper globalWrapper;
    private final String configName;
    private final List<String> highlightTerms;

    public GlobalWidgetConfigOption(
            int x,
            int y,
            int width,
            int height,
            int labelWidth,
            int configWidth,
            GlobalConfigOptionWrapper wrapper,
            int listIndex,
            GuiConfigsBase host,
            WidgetListConfigOptionsBase<?, ?> parent,
            List<String> highlightTerms
    ) {
        super(x, y, width, height, labelWidth, configWidth, wrapper, listIndex, host, parent);
        this.labelWidth = labelWidth;
        this.globalWrapper = wrapper;
        this.configName = wrapper.getConfig().getConfigGuiDisplayName();
        this.highlightTerms = List.copyOf(highlightTerms);

        // WidgetConfigOption adds the visible name first. Remove only that label;
        // the comment hover widget and all interactive controls remain untouched.
        this.subWidgets.removeIf(widget -> widget instanceof WidgetLabel);
        this.replaceCommentHoverWidget();

        if (wrapper.getMetadata().getConfigScreenSupplier() != null) {
            this.addWidget(new GlobalJumpWidget(
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
            this.addWidget(new GlobalCommentHoverWidget(
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
        String lowerName = this.configName.toLowerCase(Locale.ROOT);
        List<int[]> matches = new ArrayList<>();

        for (String term : this.highlightTerms) {
            int start = lowerName.indexOf(term);

            while (start >= 0) {
                matches.add(new int[]{start, start + term.length()});
                start = lowerName.indexOf(term, start + term.length());
            }
        }

        for (int[] match : matches) {
            int startX = this.x + this.getStringWidth(this.configName.substring(0, match[0]));
            int endX = this.x + this.getStringWidth(this.configName.substring(0, match[1]));
        drawContext.fill(startX, this.y + 6, endX, this.y + 16, MATCH_BACKGROUND);
        }

        this.drawStringWithShadow(this.x, this.y + 8, 0xFFFFFFFF, this.configName, drawContext);
    }

    private void renderSource(DrawContext drawContext) {
        int sourceX = this.x;
        int sourceRight = this.x + this.labelWidth + 7;
        int availableWidth = sourceRight - sourceX;

        if (availableWidth <= 0) {
            return;
        }

        String source = this.textRenderer.trimToWidth(
                this.globalWrapper.getMetadata().getSourceDisplayName(),
                Math.max(1, (int) (availableWidth / SOURCE_SCALE)));

        if (source.isEmpty()) {
            return;
        }

        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.scale(SOURCE_SCALE, SOURCE_SCALE, 1.0F);
        this.drawStringWithShadow(
                Math.round(sourceX / SOURCE_SCALE),
                Math.round((this.y + 17) / SOURCE_SCALE),
                SOURCE_COLOR, source, drawContext);
        matrices.pop();
    }
}
