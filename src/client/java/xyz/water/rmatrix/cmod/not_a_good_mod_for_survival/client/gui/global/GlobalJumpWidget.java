package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.global;

import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.util.StringUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/** A small gray arrow that opens the source Malilib configuration screen. */
public final class GlobalJumpWidget extends WidgetBase {
    private final GlobalConfigMetadata metadata;
    private final IConfigBase config;

    public GlobalJumpWidget(
            int x,
            int y,
            int width,
            int height,
            GlobalConfigMetadata metadata,
            IConfigBase config
    ) {
        super(x, y, width, height);
        this.metadata = metadata;
        this.config = config;
    }

    @Override
    public boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) {
            return false;
        }

        var screen = this.metadata.createConfigScreen();

        if (screen != null) {
            GlobalConfigNavigation.begin(screen, this.metadata, this.config);
            MinecraftClient.getInstance().setScreen(screen);
            GlobalConfigNavigation.afterScreenOpened(screen);
        }

        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, DrawContext drawContext) {
        int color = this.isMouseOver(mouseX, mouseY) ? 0xFFFFA0 : 0xFF808080;
        this.drawCenteredString(this.x + this.width / 2, this.y + 6, color, "→", drawContext);
    }

    @Override
    public void postRenderHovered(int mouseX, int mouseY, boolean selected, DrawContext drawContext) {
        if (this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawHoverText(mouseX, mouseY,
                    List.of(StringUtils.translate("not-a-good-mod-for-survival.gui.global_search.jump")),
                    drawContext);
        }
    }
}
