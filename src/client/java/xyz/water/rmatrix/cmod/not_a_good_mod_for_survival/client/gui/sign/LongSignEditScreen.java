package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.sign;

import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/** Material-independent, multiline sign editor for long sign text. */
public final class LongSignEditScreen extends Screen {
    private static final int AREA_MAX_WIDTH = 760;
    private static final int AREA_MAX_HEIGHT = 500;
    private static final int SCREEN_MARGIN = 24;
    private static final int AREA_BUTTON_GAP = 12;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_WIDTH = 100;

    private final SignBlockEntity blockEntity;
    private final boolean front;
    private final boolean filtered;
    private LongSignTextArea textArea;
    private ButtonWidget doneButton;
    private ButtonWidget cancelButton;

    public LongSignEditScreen(SignBlockEntity blockEntity, boolean front, boolean filtered) {
        super(Text.translatable("not-a-good-mod-for-survival.gui.long_sign_edit.title"));
        this.blockEntity = blockEntity;
        this.front = front;
        this.filtered = filtered;
    }

    @Override
    protected void init() {
        int areaWidth = Math.min(AREA_MAX_WIDTH, Math.max(320, this.width - SCREEN_MARGIN * 2));
        int areaHeight = Math.min(AREA_MAX_HEIGHT, Math.max(160, this.height - 80));
        int areaLeft = (this.width - areaWidth) / 2;
        int totalHeight = areaHeight + AREA_BUTTON_GAP + 20;
        int areaTop = Math.max(SCREEN_MARGIN, (this.height - totalHeight) / 2);

        if (this.textArea == null) {
            this.textArea = new LongSignTextArea(
                    this.textRenderer,
                    SignTextLayout.joinSignLines(this.blockEntity, this.front, this.filtered),
                    SignTextLayout.MAX_TOTAL_LENGTH
            );
        }
        this.textArea.setBounds(
                areaLeft,
                areaTop,
                areaWidth,
                areaHeight
        );

        int buttonY = areaTop + areaHeight + AREA_BUTTON_GAP;
        int buttonX = this.width / 2 - BUTTON_WIDTH - BUTTON_GAP / 2;
        this.doneButton = this.addDrawableChild(
                ButtonWidget.builder(ScreenTexts.DONE, button -> this.submit())
                        .dimensions(buttonX, buttonY, BUTTON_WIDTH, 20)
                        .build()
        );
        this.cancelButton = this.addDrawableChild(
                ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.close())
                        .dimensions(buttonX + BUTTON_WIDTH + BUTTON_GAP, buttonY, BUTTON_WIDTH, 20)
                        .build()
        );
        this.textArea.setFocused(true);
    }

    @Override
    public void tick() {
        if (this.client == null || this.client.player == null
                || this.blockEntity.isRemoved()
                || this.blockEntity.isPlayerTooFarToEdit(this.client.player.getUuid())) {
            this.close();
            return;
        }
        this.textArea.tick();
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);

        SignTextLayout.LayoutResult layout = SignTextLayout.wrapForSign(
                this.textArea.getText(),
                this.textRenderer,
                this.blockEntity.getMaxTextWidth()
        );
        int statusColor = layout.fits() ? 0xFFB7C0CC : 0xFFFF6B6B;
        Text lengthStatus = Text.translatable(
                "not-a-good-mod-for-survival.gui.long_sign_edit.length",
                this.textArea.getTextLength(),
                SignTextLayout.MAX_TOTAL_LENGTH
        );
        Text rowStatus = layout.failure() == SignTextLayout.Failure.TOO_MANY_LINES
                ? Text.translatable(
                        "not-a-good-mod-for-survival.gui.long_sign_edit.too_many_lines",
                        layout.requiredLines(),
                        SignTextLayout.MAX_SIGN_LINES
                )
                : Text.translatable(
                        "not-a-good-mod-for-survival.gui.long_sign_edit.rows",
                        layout.requiredLines(),
                        SignTextLayout.MAX_SIGN_LINES
                );

        this.textArea.render(context, mouseX, mouseY, lengthStatus, rowStatus, statusColor, statusColor);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.textArea.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.textArea.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.textArea.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.textArea.mouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.textArea.mouseReleased(button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.textArea.mouseScrolled(mouseX, mouseY, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void submit() {
        SignTextLayout.LayoutResult layout = SignTextLayout.wrapForSign(
                this.textArea.getText(),
                this.textRenderer,
                this.blockEntity.getMaxTextWidth()
        );
        if (!layout.fits() || this.client == null || this.client.getNetworkHandler() == null) {
            return;
        }

        this.client.getNetworkHandler().sendPacket(
                new UpdateSignC2SPacket(
                        this.blockEntity.getPos(),
                        this.front,
                        layout.lines().get(0),
                        layout.lines().get(1),
                        layout.lines().get(2),
                        layout.lines().get(3)
                )
        );
        this.client.setScreen(null);
    }

}
