package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.texture.TextureManager;
import xaero.map.element.MapElementRenderLocation;
import xaero.map.element.MapElementRenderer;
import xaero.map.graphics.renderer.multitexture.MultiTextureRenderTypeRendererProvider;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;

final class MapCatalogBannerElementRenderer extends MapElementRenderer<MapCatalogBannerDisplay, MapCatalogBannerElementContext, MapCatalogBannerElementRenderer> {
    MapCatalogBannerElementRenderer(
            MapCatalogBannerElementContext context,
            MapCatalogBannerElementProvider provider,
            MapCatalogBannerElementReader reader
    ) {
        super(context, provider, reader);
    }

    @Override
    public void beforeRender(int location, MinecraftClient mc, DrawContext guiGraphics, double cameraX, double cameraZ,
                             double mouseX, double mouseZ, float brightness, double scale, double guiBasedScale,
                             TextureManager textureManager, TextRenderer fontRenderer, Immediate renderTypeBuffers,
                             MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {
    }

    @Override
    public void afterRender(int location, MinecraftClient mc, DrawContext guiGraphics, double cameraX, double cameraZ,
                            double mouseX, double mouseZ, float brightness, double scale, double guiBasedScale,
                            TextureManager textureManager, TextRenderer fontRenderer, Immediate renderTypeBuffers,
                            MultiTextureRenderTypeRendererProvider rendererProvider, boolean pre) {
    }

    @Override
    public void renderElementPre(int location, MapCatalogBannerDisplay element, boolean hovered, MinecraftClient mc,
                                 DrawContext guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ,
                                 float brightness, double scale, double guiBasedScale, TextureManager textureManager,
                                 TextRenderer fontRenderer, Immediate renderTypeBuffers,
                                 MultiTextureRenderTypeRendererProvider rendererProvider, float optionalScale,
                                 double partialX, double partialY, boolean cave, float partialTicks) {
    }

    @Override
    public boolean renderElement(int location, MapCatalogBannerDisplay element, boolean hovered, MinecraftClient mc,
                                 DrawContext guiGraphics, double cameraX, double cameraZ, double mouseX, double mouseZ,
                                 float brightness, double scale, double guiBasedScale, TextureManager textureManager,
                                 TextRenderer fontRenderer, Immediate renderTypeBuffers,
                                 MultiTextureRenderTypeRendererProvider rendererProvider, int elementIndex,
                                 double optionalDepth, float optionalScale, double partialX, double partialY,
                                 boolean cave, float partialTicks) {
        if (location != MapElementRenderLocation.WORLD_MAP) {
            return false;
        }

        int color = element.color().getMapColor().color | 0xFF000000;
        int outline = hovered ? 0xFFFFFFFF : 0xFF202020;

        // The anchor is the bottom of the pole, matching Xaero's waypoint anchor.
        guiGraphics.fill(-2, -43, 2, 2, outline);
        guiGraphics.fill(0, -41, 18, -28, outline);
        guiGraphics.fill(0, -39, 16, -30, color);
        guiGraphics.fill(0, -37, 12, -34, color);
        guiGraphics.fill(-1, -1, 3, 2, outline);
        guiGraphics.fill(0, -1, 2, 1, color);

        if (hovered) {
            guiGraphics.fill(-16, -44, 16, -43, 0xFFFFFFFF);
            guiGraphics.fill(-16, 2, 16, 3, 0xFFFFFFFF);
        }
        return false;
    }

    @Override
    public boolean shouldRender(int location, boolean pre) {
        return location == MapElementRenderLocation.WORLD_MAP
                && RenderConfigs.MapCatalogMaps.ENABLE_MAP_CATALOG_DISPLAY.getBooleanValue()
                && RenderConfigs.MapCatalogMaps.SHOW_MAP_BANNERS.getBooleanValue();
    }

    @Override
    public int getOrder() {
        return 120;
    }

    @Override
    public boolean shouldBeDimScaled() {
        return false;
    }
}
