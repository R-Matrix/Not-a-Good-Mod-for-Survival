package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import xaero.map.element.MapElementReader;
import xaero.map.gui.CursorBox;

final class MapCatalogBannerElementReader extends MapElementReader<MapCatalogBannerDisplay, MapCatalogBannerElementContext, MapCatalogBannerElementRenderer> {
    @Override
    public boolean isHidden(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context) {
        return context.dimension() == null || !element.dimension().equals(context.dimension());
    }

    @Override
    public double getRenderX(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return element.banner().worldX();
    }

    @Override
    public double getRenderZ(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return element.banner().worldZ();
    }

    @Override
    public int getInteractionBoxLeft(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return -15;
    }

    @Override
    public int getInteractionBoxRight(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return 15;
    }

    @Override
    public int getInteractionBoxTop(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return -43;
    }

    @Override
    public int getInteractionBoxBottom(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return 2;
    }

    @Override
    public int getRenderBoxLeft(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return getInteractionBoxLeft(element, context, partialTicks);
    }

    @Override
    public int getRenderBoxRight(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return getInteractionBoxRight(element, context, partialTicks);
    }

    @Override
    public int getRenderBoxTop(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return getInteractionBoxTop(element, context, partialTicks);
    }

    @Override
    public int getRenderBoxBottom(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, float partialTicks) {
        return getInteractionBoxBottom(element, context, partialTicks);
    }

    @Override
    public int getLeftSideLength(MapCatalogBannerDisplay element, MinecraftClient mc) {
        return 40;
    }

    @Override
    public String getMenuName(MapCatalogBannerDisplay element) {
        return element.nameString().isEmpty()
                ? I18n.translate("not-a-good-mod-for-survival.gui.xaero.map_catalog.banner")
                : element.nameString();
    }

    @Override
    public String getFilterName(MapCatalogBannerDisplay element) {
        return getMenuName(element) + " " + element.mapIdsText();
    }

    @Override
    public int getMenuTextFillLeftPadding(MapCatalogBannerDisplay element) {
        return 0;
    }

    @Override
    public int getRightClickTitleBackgroundColor(MapCatalogBannerDisplay element) {
        return element.color().getMapColor().color | 0xFF000000;
    }

    @Override
    public boolean shouldScaleBoxWithOptionalScale() {
        return false;
    }

    @Override
    public boolean isInteractable(int location, MapCatalogBannerDisplay element) {
        return location == xaero.map.element.MapElementRenderLocation.WORLD_MAP;
    }

    @Override
    public CursorBox getTooltip(MapCatalogBannerDisplay element, MapCatalogBannerElementContext context, boolean overMenu) {
        Text color = Text.translatable("color.minecraft." + element.color().getName());
        Text name = element.banner().name().orElse(Text.translatable(
                "not-a-good-mod-for-survival.gui.xaero.map_catalog.banner.no_name"));
        return new CursorBox(Text.translatable(
                "not-a-good-mod-for-survival.gui.xaero.map_catalog.banner.tooltip",
                color,
                element.banner().worldX(),
                element.banner().worldZ(),
                name,
                element.mapIdsText()
        ));
    }
}
