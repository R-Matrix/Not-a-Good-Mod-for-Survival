package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global_search;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.gui.widgets.WidgetHoverInfo;
import fi.dy.masa.malilib.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search.GlobalSearchSettings;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search.GlobalSearchText;

/** Malilib comment tooltip that also highlights plain-text search matches. */
final class GlobalSearchCommentHoverWidget extends WidgetHoverInfo {
    private final List<String> highlightTerms;

    GlobalSearchCommentHoverWidget(
            int x,
            int y,
            int width,
            int height,
            List<String> lines,
            List<String> highlightTerms
    ) {
        super(x, y, width, height, "");
        this.getLines().clear();
        this.getLines().addAll(lines);
        this.highlightTerms = List.copyOf(highlightTerms);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        Screen screen = client.currentScreen;

        if (screen == null || this.getLines().isEmpty()) {
            return;
        }

        RenderSystem.enableDepthTest();

        List<String> flattenedLines = new ArrayList<>();
        int maxLineLength = 0;

        for (String originalLine : this.getLines()) {
            for (String line : originalLine.split("\\n")) {
                maxLineLength = Math.max(maxLineLength, this.getStringWidth(line));
                flattenedLines.add(line);
            }
        }

        int lineHeight = this.fontHeight + 1;
        int textHeight = flattenedLines.size() * lineHeight - 2;
        int textStartX = mouseX + 4;
        int textStartY = Math.max(8, mouseY - textHeight - 6);

        if (textStartX + maxLineLength + 6 > screen.width) {
            textStartX = Math.max(2, screen.width - maxLineLength - 8);
        }

        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(0.0, 0.0, 300.0);

        int zLevel = 300;
        int borderColor = 0xF0100010;
        RenderUtils.drawGradientRect(textStartX - 3, textStartY - 4,
                textStartX + maxLineLength + 3, textStartY - 3,
                zLevel, borderColor, borderColor);
        RenderUtils.drawGradientRect(textStartX - 3, textStartY + textHeight + 3,
                textStartX + maxLineLength + 3, textStartY + textHeight + 4,
                zLevel, borderColor, borderColor);
        RenderUtils.drawGradientRect(textStartX - 3, textStartY - 3,
                textStartX + maxLineLength + 3, textStartY + textHeight + 3,
                zLevel, borderColor, borderColor);
        RenderUtils.drawGradientRect(textStartX - 4, textStartY - 3,
                textStartX - 3, textStartY + textHeight + 3,
                zLevel, borderColor, borderColor);
        RenderUtils.drawGradientRect(textStartX + maxLineLength + 3, textStartY - 3,
                textStartX + maxLineLength + 4, textStartY + textHeight + 3,
                zLevel, borderColor, borderColor);

        int fillColor1 = 0x505000FF;
        int fillColor2 = 0x5028007F;
        RenderUtils.drawGradientRect(textStartX - 3, textStartY - 2,
                textStartX - 2, textStartY + textHeight + 2,
                zLevel, fillColor1, fillColor2);
        RenderUtils.drawGradientRect(textStartX + maxLineLength + 2, textStartY - 2,
                textStartX + maxLineLength + 3, textStartY + textHeight + 2,
                zLevel, fillColor1, fillColor2);
        RenderUtils.drawGradientRect(textStartX - 3, textStartY - 3,
                textStartX + maxLineLength + 3, textStartY - 2,
                zLevel, fillColor1, fillColor1);
        RenderUtils.drawGradientRect(textStartX - 3, textStartY + textHeight + 2,
                textStartX + maxLineLength + 3, textStartY + textHeight + 3,
                zLevel, fillColor2, fillColor2);

        int lineY = textStartY;

        for (String line : flattenedLines) {
            this.renderHighlightedLine(drawContext, line, textStartX, lineY);
            lineY += lineHeight;
        }

        RenderUtils.forceDraw(drawContext);
        matrices.pop();
    }

    private void renderHighlightedLine(DrawContext drawContext, String line, int x, int y) {
        if (!GlobalSearchSettings.isSearchHighlightEnabled()) {
            drawContext.drawTextWithShadow(this.textRenderer, line, x, y, 0xFFFFFFFF);
            return;
        }

        for (String term : this.highlightTerms) {
            for (GlobalSearchText.Match match : GlobalSearchText.findMatches(line, term)) {
                int startX = x + this.getStringWidth(line.substring(0, match.start()));
                int endX = x + this.getStringWidth(line.substring(0, match.end()));
                drawContext.fill(startX, y - 1, endX, y + this.fontHeight + 1,
                        GlobalSearchWidgetConfigOption.MATCH_BACKGROUND);
            }
        }

        drawContext.drawTextWithShadow(this.textRenderer, line, x, y, 0xFFFFFFFF);
    }
}
