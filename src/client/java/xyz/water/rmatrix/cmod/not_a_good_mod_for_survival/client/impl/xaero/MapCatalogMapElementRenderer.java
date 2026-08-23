package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import xaero.map.element.MapElementRenderLocation;
import xaero.map.element.MapElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;

final class MapCatalogMapElementRenderer extends MapElementRenderer<MapCatalogMapDisplayGroup, MapCatalogMapElementContext, MapCatalogMapElementRenderer> {
    static final float MAP_LABEL_SCALE = 2.5F;
    static final int MAP_LABEL_PADDING = 3;
    static final double MIN_MAP_LABEL_ZOOM = 0.404D;

    MapCatalogMapElementRenderer(
            MapCatalogMapElementContext context,
            MapCatalogMapElementProvider provider,
            MapCatalogMapElementReader reader
    ) {
        super(context, provider, reader);
    }

    @Override
    public void beforeRender(
            int location, MinecraftClient mc, DrawContext guiGraphics, double cameraX, double cameraZ,
            double mouseX, double mouseZ, float brightness, double scale, double guiBasedScale,
            TextureManager textureManager, TextRenderer fontRenderer, Immediate renderTypeBuffers,
            MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre
    ) {
    }

    @Override
    public void afterRender(
            int location, MinecraftClient mc, DrawContext guiGraphics, double cameraX, double cameraZ,
            double mouseX, double mouseZ, float brightness, double scale, double guiBasedScale,
            TextureManager textureManager, TextRenderer fontRenderer, Immediate renderTypeBuffers,
            MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre
    ) {
    }

    @Override
    public void renderElementPre(
            int location, MapCatalogMapDisplayGroup element, boolean hovered, MinecraftClient mc,
            DrawContext guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ,
            float brightness, double scale, double guiBasedScale, TextureManager textureManager,
            TextRenderer fontRenderer, Immediate renderTypeBuffers,
            MultiTextureRenderTypeRendererProvider rendererProvider, float optionalScale,
            double partialX, double partialY, boolean cave, float partialTicks
    ) {
    }

    @Override
    public boolean renderElement(
            int location, MapCatalogMapDisplayGroup element, boolean hovered, MinecraftClient mc,
            DrawContext guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ,
            float brightness, double scale, double guiBasedScale, TextureManager textureManager,
            TextRenderer fontRenderer, Immediate renderTypeBuffers,
            MultiTextureRenderTypeRendererProvider rendererProvider, int elementIndex,
            double optionalDepth, float optionalScale, double partialX, double partialY,
            boolean cave, float partialTicks
    ) {
        if (location != MapElementRenderLocation.WORLD_MAP) {
            return false;
        }

        MatrixStack matrices = guiGraphics.getMatrices();
        int half = (int)Math.ceil(element.coverageBlocks() * scale / 2.0D);
        int border = Math.max(1, Math.min(4, (int)Math.ceil(scale)));
        int borderColor = hovered ? 0xFFFFE66D : 0xFFFFB52E;

        if (RenderConfigs.MapCatalogMaps.SHOW_MAP_BORDERS.getBooleanValue()) {
            guiGraphics.fill(-half, -half, half, -half + border, borderColor);
            guiGraphics.fill(-half, half - border, half, half, borderColor);
            guiGraphics.fill(-half, -half, -half + border, half, borderColor);
            guiGraphics.fill(half - border, -half, half, half, borderColor);
        }

        if (scale >= MIN_MAP_LABEL_ZOOM
                && RenderConfigs.MapCatalogMaps.SHOW_MAP_NUMBERS.getBooleanValue()) {
            String label = element.label();
            int textWidth = fontRenderer.getWidth(label);
            int textX = -textWidth / 2;
            int textY = -fontRenderer.fontHeight / 2;
            matrices.push();
            matrices.scale(MAP_LABEL_SCALE, MAP_LABEL_SCALE, 1.0F);
            guiGraphics.fill(
                    textX - MAP_LABEL_PADDING,
                    textY - MAP_LABEL_PADDING,
                    textX + textWidth + MAP_LABEL_PADDING,
                    textY + fontRenderer.fontHeight + MAP_LABEL_PADDING,
                    0xB0000000);
            guiGraphics.drawTextWithShadow(fontRenderer, label, textX, textY, hovered ? 0xFFFFF2A3 : 0xFFFFFFFF);
            matrices.pop();
        }
        return false;
    }

    @Override
    public boolean shouldRender(int location, boolean pre) {
        return location == MapElementRenderLocation.WORLD_MAP
                && RenderConfigs.MapCatalogMaps.ENABLE_MAP_CATALOG_DISPLAY.getBooleanValue();
    }

    @Override
    public int getOrder() {
        return 110;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }
}
