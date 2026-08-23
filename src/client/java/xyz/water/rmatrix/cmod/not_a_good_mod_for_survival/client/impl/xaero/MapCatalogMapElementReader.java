package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import xaero.map.element.MapElementReader;
import xaero.map.gui.CursorBox;

final class MapCatalogMapElementReader extends MapElementReader<MapCatalogMapDisplayGroup, MapCatalogMapElementContext, MapCatalogMapElementRenderer> {
    @Override
    public boolean isHidden(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context) {
        return context.dimension() == null
                || !element.representative().dimension().equals(context.dimension());
    }

    @Override
    public double getRenderX(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return element.representative().centerX();
    }

    @Override
    public double getRenderZ(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return element.representative().centerZ();
    }

    @Override
    public int getInteractionBoxLeft(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return -element.coverageBlocks() / 2;
    }

    @Override
    public int getInteractionBoxRight(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return element.coverageBlocks() / 2;
    }

    @Override
    public int getInteractionBoxTop(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return -element.coverageBlocks() / 2;
    }

    @Override
    public int getInteractionBoxBottom(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return element.coverageBlocks() / 2;
    }

    @Override
    public int getRenderBoxLeft(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return getInteractionBoxLeft(element, context, partialTicks);
    }

    @Override
    public int getRenderBoxRight(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return getInteractionBoxRight(element, context, partialTicks);
    }

    @Override
    public int getRenderBoxTop(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return getInteractionBoxTop(element, context, partialTicks);
    }

    @Override
    public int getRenderBoxBottom(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, float partialTicks) {
        return getInteractionBoxBottom(element, context, partialTicks);
    }

    @Override
    public int getLeftSideLength(MapCatalogMapDisplayGroup element, MinecraftClient mc) {
        return 9 + mc.textRenderer.getWidth(element.label());
    }

    @Override
    public String getMenuName(MapCatalogMapDisplayGroup element) {
        return element.label();
    }

    @Override
    public String getFilterName(MapCatalogMapDisplayGroup element) {
        return element.fullIdRanges();
    }

    @Override
    public int getMenuTextFillLeftPadding(MapCatalogMapDisplayGroup element) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(MapCatalogMapDisplayGroup element) {
        return 0xFFCC8A24;
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return false;
    }

    @Override
    public boolean isInteractable(int location, MapCatalogMapDisplayGroup element) {
        return location == xaero.map.element.MapElementRenderLocation.WORLD_MAP;
    }

    @Override
    public boolean isHoveredOnMap(
            int location,
            MapCatalogMapDisplayGroup element,
            double mouseX,
            double mouseZ,
            double scale,
            double screenSizeBasedScale,
            double rendererDimDiv,
            MapCatalogMapElementContext context,
            float partialTicks
    ) {
        if (location != xaero.map.element.MapElementRenderLocation.WORLD_MAP
                || scale < MapCatalogMapElementRenderer.MIN_MAP_LABEL_ZOOM
                || isHidden(element, context)) {
            return false;
        }

        String label = element.label();
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        int textWidth = textRenderer.getWidth(label);
        int textHeight = textRenderer.fontHeight;
        double halfWidth = (textWidth / 2.0D + MapCatalogMapElementRenderer.MAP_LABEL_PADDING)
                * MapCatalogMapElementRenderer.MAP_LABEL_SCALE;
        double halfHeight = (textHeight / 2.0D + MapCatalogMapElementRenderer.MAP_LABEL_PADDING)
                * MapCatalogMapElementRenderer.MAP_LABEL_SCALE;
        double screenOffX = (mouseX - getRenderX(element, context, partialTicks) / rendererDimDiv) * scale;
        double screenOffZ = (mouseZ - getRenderZ(element, context, partialTicks) / rendererDimDiv) * scale;
        return Math.abs(screenOffX) < halfWidth * scale && Math.abs(screenOffZ) < halfHeight * scale;
    }

    @Override
    public boolean isOnScreen(
            MapCatalogMapDisplayGroup element,
            double cameraX,
            double cameraZ,
            int width,
            int height,
            double scale,
            double screenSizeBasedScale,
            double rendererDimDiv,
            MapCatalogMapElementContext context,
            float partialTicks
    ) {
        double x = (getRenderX(element, context, partialTicks) / rendererDimDiv - cameraX) * scale + width / 2.0D;
        double z = (getRenderZ(element, context, partialTicks) / rendererDimDiv - cameraZ) * scale + height / 2.0D;
        double half = element.coverageBlocks() * scale / 2.0D;
        return x + half > 0.0D && x - half < width && z + half > 0.0D && z - half < height;
    }

    @Override
    public CursorBox getTooltip(MapCatalogMapDisplayGroup element, MapCatalogMapElementContext context, boolean overMenu) {
        return new CursorBox(Text.literal(element.label())
                .append(Text.literal(" \n "))
                .append(Text.literal(element.fullIdRanges())));
    }
}
