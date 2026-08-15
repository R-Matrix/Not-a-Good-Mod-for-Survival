package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** OpenZen-style editor screen for the material-list panel. */
public final class MaterialListClickGuiScreen extends Screen {
    private final OpenZenMaterialHudRenderer renderer;

    public MaterialListClickGuiScreen(OpenZenMaterialHudRenderer renderer) {
        super(Text.literal("Material List Click GUI"));
        this.renderer = renderer;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        drawContext.fill(0, 0, width, height, 0x44000000);
        renderer.renderEditor(drawContext);

        if (client != null) {
            String help = "Left drag header  |  Right click collapse  |  H / ESC close";
            drawContext.drawTextWithShadow(client.textRenderer, help, 8, height - 18, 0xFFD8CEE8);
        }

        super.render(drawContext, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return renderer.editorMouseClicked(mouseX, mouseY, button)
                || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return renderer.editorMouseDragged(mouseX, mouseY, button)
                || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return renderer.editorMouseReleased(button)
                || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MaterialHudController.matchesEditKey(keyCode, scanCode)) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        renderer.stopDragging();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
