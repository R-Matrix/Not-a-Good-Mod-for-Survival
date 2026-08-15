package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.materials.MaterialListBase;
import fi.dy.masa.litematica.materials.MaterialListEntry;
import fi.dy.masa.litematica.materials.MaterialListHudRenderer;
import fi.dy.masa.litematica.materials.MaterialListSorter;
import fi.dy.masa.litematica.materials.MaterialListUtils;
import fi.dy.masa.litematica.render.infohud.IInfoHudRenderer;
import fi.dy.masa.litematica.render.infohud.RenderPhase;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

/**
 * A compact material HUD using the dark panel, accent strip and glow-like layering
 * used by OpenZen's panel Click GUI.
 */
public final class OpenZenMaterialHudRenderer implements IInfoHudRenderer {
    private static final int ROW_HEIGHT = 19;
    private static final int HEADER_HEIGHT = 24;
    private static final int HEADER_HIT_PADDING = 7;
    private static final int PANEL_PADDING = 5;
    private static final float FONT_SCALE = 0.88F;
    private static final int ACCENT = 0xFFE0C8FF;
    private static final int ACCENT_DARK = 0xFF8B64B5;
    private static final int PANEL = 0xEE15151A;
    private static final int PANEL_INNER = 0xB522222A;
    private static final int HEADER_FILL = 0xB52B2634;
    private static final int HEADER_BORDER = 0xD18F69BE;
    private static final int HEADER_LINE = 0xE0D1A8F1;
    private static final int TEXT = 0xFFF1EDF7;
    private static final int TEXT_MUTED = 0xFFAAA4B2;
    private static final int MISSING = 0xFFE57373;
    private static final int MISSING_BG = 0xB545252C;

    private final MaterialListBase materialList;
    private final MaterialListHudRenderer stockRenderer;
    private final MaterialListSorter sorter;
    private final List<DisplayEntry> displayEntries = new ArrayList<>();
    private long lastAvailableUpdate;
    private HudLayout lastLayout;
    private boolean collapsed;
    private boolean dragging;
    private boolean hasCustomPosition;
    private int customX;
    private int customY;
    private double dragOffsetX;
    private double dragOffsetY;

    public OpenZenMaterialHudRenderer(MaterialListBase materialList) {
        this.materialList = materialList;
        this.stockRenderer = materialList.getHudRenderer();
        this.sorter = new MaterialListSorter(materialList);
    }

    @Override
    public boolean getShouldRenderText(RenderPhase phase) {
        return false;
    }

    @Override
    public boolean getShouldRenderCustom() {
        return stockRenderer.getShouldRenderCustom();
    }

    @Override
    public boolean shouldRenderInGuis() {
        return !(MinecraftClient.getInstance().currentScreen instanceof MaterialListClickGuiScreen)
                && stockRenderer.shouldRenderInGuis();
    }

    @Override
    public List<String> getText(RenderPhase phase) {
        return Collections.emptyList();
    }

    @Override
    public int render(int xOffset, int yOffset, HudAlignment alignment, DrawContext drawContext) {
        MinecraftClient minecraft = MinecraftClient.getInstance();
        if (minecraft.player == null) {
            return 0;
        }

        refreshEntries(minecraft);
        if (displayEntries.isEmpty()) {
            lastLayout = null;
            return 0;
        }

        TextRenderer textRenderer = minecraft.textRenderer;
        double scale = Math.max(0.05D, Configs.InfoOverlays.MATERIAL_LIST_HUD_SCALE.getDoubleValue());
        int maxLines = Math.max(1, Configs.InfoOverlays.MATERIAL_LIST_HUD_MAX_LINES.getIntegerValue());
        int size = Math.min(displayEntries.size(), maxLines);
        int maxNameWidth = 0;
        int maxCountWidth = 0;

        for (int i = 0; i < size; i++) {
            DisplayEntry entry = displayEntries.get(i);
            maxNameWidth = Math.max(maxNameWidth, scaledTextWidth(textRenderer, entry.name()));
            maxCountWidth = Math.max(maxCountWidth, scaledTextWidth(textRenderer, entry.count()));
        }

        HudLayout layout = createLayout(xOffset, yOffset, alignment, scale, size, maxNameWidth, maxCountWidth,
                textRenderer);
        lastLayout = layout;
        int panelX = layout.x;
        int panelY = layout.y;
        int panelWidth = layout.width;
        int panelHeight = layout.height;
        int countLabelWidth = layout.countLabelWidth;
        String title = layout.title;

        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.scale((float) scale, (float) scale, 1.0F);

        drawPanel(drawContext, panelX, panelY, panelWidth, panelHeight);
        drawScaledText(drawContext, textRenderer, title, panelX + 10, panelY + 7, TEXT);
        drawScaledText(drawContext, textRenderer, size + " ITEMS", panelX + panelWidth - countLabelWidth - 20,
                panelY + 8, TEXT_MUTED);

        String collapseIcon = collapsed ? "+" : "-";
        drawScaledText(drawContext, textRenderer, collapseIcon, panelX + panelWidth - 11, panelY + 8, ACCENT);

        int rowY = panelY + HEADER_HEIGHT;
        int rowCount = collapsed ? 0 : layout.itemCount;
        for (int i = 0; i < rowCount; i++) {
            DisplayEntry entry = displayEntries.get(i);
            ItemStack stack = entry.stack();
            int rowColor = (i & 1) == 0 ? PANEL_INNER : 0x98272A31;
            drawRoundedRect(drawContext, panelX + 5, rowY, panelWidth - 10, ROW_HEIGHT, 3.5F, rowColor);
            drawContext.drawItem(stack, panelX + 8, rowY + 1);

            String name = trimName(textRenderer, entry.name(),
                    (int) ((panelWidth - 74 - maxCountWidth) / FONT_SCALE));
            drawScaledText(drawContext, textRenderer, name, panelX + 30, rowY + 6, TEXT);

            String count = entry.count();
            int countWidth = scaledTextWidth(textRenderer, count);
            int countX = panelX + panelWidth - countWidth - 12;
            drawRoundedRect(drawContext, countX - 4, rowY + 3, countWidth + 8, 14, 4.0F, MISSING_BG);
            drawScaledText(drawContext, textRenderer, count, countX, rowY + 6, MISSING);
            rowY += ROW_HEIGHT;
        }

        matrices.pop();
        return panelHeight + 4;
    }

    public void renderEditor(DrawContext drawContext) {
        int xOffset = Configs.InfoOverlays.INFO_HUD_OFFSET_X.getIntegerValue();
        int yOffset = Configs.InfoOverlays.INFO_HUD_OFFSET_Y.getIntegerValue();
        HudAlignment alignment = (HudAlignment) Configs.InfoOverlays.INFO_HUD_ALIGNMENT.getOptionListValue();
        render(xOffset, yOffset, alignment, drawContext);
    }

    public boolean editorMouseClicked(double mouseX, double mouseY, int button) {
        if (lastLayout == null) {
            return false;
        }

        double logicalX = mouseX / lastLayout.scale;
        double logicalY = mouseY / lastLayout.scale;
        if (!isOverHeader(lastLayout, logicalX, logicalY)) {
            return false;
        }

        if (button == 0) {
            dragging = true;
            hasCustomPosition = true;
            customX = lastLayout.x;
            customY = lastLayout.y;
            dragOffsetX = logicalX - customX;
            dragOffsetY = logicalY - customY;
            return true;
        }

        if (button == 1) {
            collapsed = !collapsed;
            return true;
        }

        return false;
    }

    public boolean editorMouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0 || lastLayout == null) {
            return false;
        }

        double logicalX = mouseX / lastLayout.scale;
        double logicalY = mouseY / lastLayout.scale;
        int maxX = Math.max(0, (int) (GuiUtils.getScaledWindowWidth() / lastLayout.scale) - lastLayout.width);
        int maxY = Math.max(0, (int) (GuiUtils.getScaledWindowHeight() / lastLayout.scale) - lastLayout.height);
        customX = clamp((int) Math.round(logicalX - dragOffsetX), 0, maxX);
        customY = clamp((int) Math.round(logicalY - dragOffsetY), 0, maxY);
        lastLayout = lastLayout.withPosition(customX, customY);
        return true;
    }

    public boolean editorMouseReleased(int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    public void stopDragging() {
        dragging = false;
    }


    private HudLayout createLayout(int xOffset, int yOffset, HudAlignment alignment, double scale, int size,
                                   int maxNameWidth, int maxCountWidth, TextRenderer textRenderer) {
        String title = StringUtils.translate("litematica.gui.button.material_list");
        int titleWidth = scaledTextWidth(textRenderer, title);
        int countLabelWidth = scaledTextWidth(textRenderer, size + " ITEMS");
        int panelWidth = Math.max(titleWidth + 34, Math.max(156, 18 + maxNameWidth + 16 + maxCountWidth + 12));
        panelWidth = Math.max(panelWidth, 18 + countLabelWidth + 30);
        int panelHeight = HEADER_HEIGHT + (collapsed ? 0 : size * ROW_HEIGHT) + PANEL_PADDING;

        int marginX = Math.max(2, xOffset);
        int marginY = Math.max(2, yOffset);
        int panelX = switch (alignment) {
            case TOP_RIGHT, BOTTOM_RIGHT -> (int) ((GuiUtils.getScaledWindowWidth() / scale) - panelWidth - marginX);
            case CENTER -> (int) ((GuiUtils.getScaledWindowWidth() / scale / 2.0D) - (panelWidth / 2.0D) - xOffset);
            default -> marginX;
        };

        if (scale != 1.0D) {
            yOffset = (int) (yOffset / scale);
        }

        int panelY = RenderUtils.getHudPosY(marginY, yOffset, panelHeight, scale, alignment);
        panelY += RenderUtils.getHudOffsetForPotions(alignment, scale, MinecraftClient.getInstance().player);

        if (hasCustomPosition) {
            panelX = customX;
            panelY = customY;
        }

        return new HudLayout(panelX, panelY, panelWidth, panelHeight, size, countLabelWidth, title, scale);
    }

    private boolean isOverHeader(HudLayout layout, double mouseX, double mouseY) {
        return mouseX >= layout.x - HEADER_HIT_PADDING
                && mouseX <= layout.x + layout.width + HEADER_HIT_PADDING
                && mouseY >= layout.y - HEADER_HIT_PADDING
                && mouseY <= layout.y + HEADER_HEIGHT + HEADER_HIT_PADDING;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int scaledTextWidth(TextRenderer textRenderer, String text) {
        return Math.max(1, Math.round(textRenderer.getWidth(text) * FONT_SCALE));
    }

    private void drawScaledText(DrawContext drawContext, TextRenderer textRenderer, String text, int x, int y,
                                int color) {
        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0D);
        matrices.scale(FONT_SCALE, FONT_SCALE, 1.0F);
        drawContext.drawText(textRenderer, text, 0, 0, color, false);
        matrices.pop();
    }

    private void refreshEntries(MinecraftClient minecraft) {
        long now = System.currentTimeMillis();
        if (now - lastAvailableUpdate < 2000L) {
            return;
        }

        MaterialListUtils.updateAvailableCounts(materialList.getMaterialsAll(), minecraft.player);
        List<MaterialListEntry> refreshedEntries = new ArrayList<>(materialList.getMaterialsMissingOnly(true));
        refreshedEntries.sort(sorter);

        displayEntries.clear();
        for (MaterialListEntry entry : refreshedEntries) {
            displayEntries.add(new DisplayEntry(
                    entry.getStack().copy(),
                    entry.getStack().getName().getString(),
                    formatCount(entry)
            ));
        }
        lastAvailableUpdate = now;
    }

    private String formatCount(MaterialListEntry entry) {
        int multiplier = materialList.getMultiplier();
        int count = multiplier == 1
                ? entry.getCountMissing() - entry.getCountAvailable()
                : entry.getCountTotal();
        count = Math.max(0, count * multiplier);
        return formatCountString(count, entry.getStack().getMaxCount());
    }

    private String formatCountString(int count, int maxStackSize) {
        int stacks = count / maxStackSize;
        int remainder = count % maxStackSize;
        double boxCount = (double) count / (27D * maxStackSize);

        if (count > maxStackSize) {
            if (boxCount >= 1.0D) {
                return String.format("%d (%.2f %s)", count, boxCount,
                        StringUtils.translate("litematica.gui.label.material_list.abbr.shulker_box"));
            }
            if (remainder > 0) {
                return String.format("%d (%d x %d + %d)", count, stacks, maxStackSize, remainder);
            }
            return String.format("%d (%d x %d)", count, stacks, maxStackSize);
        }

        return Integer.toString(count);
    }

    private String trimName(TextRenderer textRenderer, String name, int maxWidth) {
        if (textRenderer.getWidth(name) <= maxWidth) {
            return name;
        }

        String ellipsis = "...";
        for (int length = name.length() - 1; length > 0; length--) {
            String candidate = name.substring(0, length) + ellipsis;
            if (textRenderer.getWidth(candidate) <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }

    private void drawPanel(DrawContext drawContext, int x, int y, int width, int height) {
        drawRoundedRect(drawContext, x, y, width, height, 6.0F, PANEL);

        // Keep the title treatment simple: one thin border and a compact inner fill.
        drawRoundedRect(drawContext, x + 2, y + 2, width - 4, HEADER_HEIGHT - 2, 5.0F, HEADER_BORDER);
        drawRoundedRect(drawContext, x + 4, y + 4, width - 8, HEADER_HEIGHT - 6, 3.5F, HEADER_FILL);
        RenderUtils.drawGradientRect(x + 9, y + 3, x + width - 9, y + 5, 0.0F, ACCENT, ACCENT_DARK);
        RenderUtils.drawRect(x + 8, y + HEADER_HEIGHT - 2, width - 16, 1, HEADER_LINE);
    }

    private void drawRoundedRect(DrawContext drawContext, int x, int y, int width, int height,
                                 float radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float corner = Math.min(radius, Math.min(width, height) * 0.5F);
        float left = x;
        float top = y;
        float right = x + width;
        float bottom = y + height;
        int segments = Math.max(8, Math.round(corner * 2.0F));
        MatrixStack.Entry matrix = drawContext.getMatrices().peek();

        drawContext.draw(vertexConsumers -> {
            VertexConsumer vertices = vertexConsumers.getBuffer(RenderLayer.getDebugTriangleFan());
            float firstX = right - corner;
            float firstY = top;
            emitVertex(vertices, matrix, (left + right) * 0.5F, (top + bottom) * 0.5F, color);

            emitVertex(vertices, matrix, firstX, firstY, color);
            emitCorner(vertices, matrix, right - corner, top + corner, -90.0F, 0.0F, segments, corner, color);
            emitCorner(vertices, matrix, right - corner, bottom - corner, 0.0F, 90.0F, segments, corner, color);
            emitCorner(vertices, matrix, left + corner, bottom - corner, 90.0F, 180.0F, segments, corner, color);
            emitCorner(vertices, matrix, left + corner, top + corner, 180.0F, 270.0F, segments, corner, color);
            emitVertex(vertices, matrix, firstX, firstY, color);
        });
    }

    private void emitCorner(VertexConsumer vertices, MatrixStack.Entry matrix, float centerX, float centerY,
                            float startAngle, float endAngle, int segments, float radius, int color) {
        for (int i = 1; i <= segments; i++) {
            float progress = (float) i / segments;
            double angle = Math.toRadians(startAngle + (endAngle - startAngle) * progress);
            emitVertex(vertices, matrix,
                    centerX + (float) Math.cos(angle) * radius,
                    centerY + (float) Math.sin(angle) * radius,
                    color);
        }
    }

    private void emitVertex(VertexConsumer vertices, MatrixStack.Entry matrix, float x, float y, int color) {
        vertices.vertex(matrix, x, y, 0.0F).color(color);
    }

    private record DisplayEntry(ItemStack stack, String name, String count) {
    }

    private record HudLayout(int x, int y, int width, int height, int itemCount, int countLabelWidth,
                             String title, double scale) {
        private HudLayout withPosition(int newX, int newY) {
            return new HudLayout(newX, newY, width, height, itemCount, countLabelWidth, title, scale);
        }
    }
}
