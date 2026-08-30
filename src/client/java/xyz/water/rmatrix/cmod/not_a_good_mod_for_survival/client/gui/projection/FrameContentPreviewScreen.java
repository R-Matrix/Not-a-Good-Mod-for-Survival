package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.gui.projection;

import com.mojang.blaze3d.systems.RenderSystem;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.ProjectionAimScanner;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FrameContentAvailability;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FrameContentAvailability.Availability;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FramePreviewPalette;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FramePreviewPalette.PreviewState;

/**
 * Shows the item a projected item frame holds inside the compact one-slot chest panel
 * that malilib's inventory overlay renders for single-item blocks such as lecterns.
 *
 * <p>The handler only exists on the client, every slot action is dropped, and the
 * preview can never move anything. The single slot follows the QuickCraft container
 * verifier look and judges the item actually placed in the frame against the projected
 * one: an empty frame shows the expected icon as a translucent blue ghost, a frame
 * holding the same item with different components washes the slot red, and a matching
 * frame stays untouched.
 */
public final class FrameContentPreviewScreen extends HandledScreen<ScreenHandler> {
    private static final Identifier CHEST_TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");
    private static final int PREVIEW_SYNC_ID = 1;
    /** The panel is a 7 pixel border around one 18 pixel slot cell, like malilib's
     * {@code SINGLE_ITEM} inventory overlay. */
    private static final int BORDER = 7;
    private static final int MIDDLE = BORDER + 18;
    private static final int PANEL_WIDTH = BORDER + MIDDLE;
    private static final int PANEL_HEIGHT = BORDER + MIDDLE;
    private static final int SLOT_SIZE = 18;
    private static final int ITEM_SIZE = 16;
    private static final int SLOT_X = BORDER + 1;
    private static final int SLOT_Y = BORDER + 1;
    private static final int INFO_LINE_HEIGHT = 12;
    private static final int INFO_COLOR = 0xA0A0A0;
    private static final int TINT_DEPTH = 300;

    private final ItemStack content;
    private final BlockPos pos;
    private final boolean glowFrame;
    private final int rotation;

    private FrameContentPreviewScreen(ItemStack content, BlockPos pos, boolean glowFrame,
            int rotation, PlayerInventory playerInventory) {
        super(new PreviewScreenHandler(new SimpleInventory(content), content, pos),
                playerInventory, Text.translatable(key("title")));
        this.content = content;
        this.pos = pos;
        this.glowFrame = glowFrame;
        this.rotation = rotation;
        this.backgroundWidth = PANEL_WIDTH;
        this.backgroundHeight = PANEL_HEIGHT;
    }

    /**
     * Returns a preview for {@code content}, or null when there is nothing to show or no
     * player to build the screen for.
     */
    @Nullable
    public static FrameContentPreviewScreen create(ItemStack content, BlockPos pos,
            boolean glowFrame, int rotation) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (content.isEmpty() || client.player == null) {
            return null;
        }

        return new FrameContentPreviewScreen(content, pos, glowFrame, rotation, client.player.getInventory());
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // The same five chest texture strips malilib draws for a single-item overlay.
        context.drawTexture(RenderLayer::getGuiTextured, CHEST_TEXTURE, this.x, this.y,
                0.0F, 0.0F, BORDER, MIDDLE, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, CHEST_TEXTURE, this.x + BORDER, this.y,
                176.0F - MIDDLE, 0.0F, MIDDLE, BORDER, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, CHEST_TEXTURE, this.x, this.y + MIDDLE,
                0.0F, 215.0F, MIDDLE, BORDER, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, CHEST_TEXTURE, this.x + MIDDLE, this.y + BORDER,
                169.0F, 222.0F - MIDDLE, BORDER, MIDDLE, 256, 256);
        context.drawTexture(RenderLayer::getGuiTextured, CHEST_TEXTURE, this.x + BORDER, this.y + BORDER,
                7.0F, 17.0F, SLOT_SIZE, SLOT_SIZE, 256, 256);
    }

    /**
     * HandledScreen itself never draws the hovered slot tooltip; the vanilla container
     * screens call {@code drawMouseoverTooltip} from their render override, which is
     * what makes a slot behave like a chest slot here too.
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        int lineY = PANEL_HEIGHT + INFO_LINE_HEIGHT;
        context.drawCenteredTextWithShadow(this.textRenderer, this.frameLabel(),
                PANEL_WIDTH / 2, lineY, INFO_COLOR);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable(key("rotation"),
                Integer.toString(this.rotation)), PANEL_WIDTH / 2, lineY + INFO_LINE_HEIGHT, INFO_COLOR);

        if (this.isReadableBook()) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable(key("clickBook")),
                    PANEL_WIDTH / 2, lineY + INFO_LINE_HEIGHT * 2, INFO_COLOR);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isReadableBook() && this.isOverItem(mouseX, mouseY)) {
            MinecraftClient client = MinecraftClient.getInstance();
            BookScreen.Contents contents = BookScreen.Contents.create(this.content);

            if (contents != null) {
                client.setScreen(new BookScreen(contents));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** The preview is a glance at a projection, so it must never pause the world. */
    @Override
    public boolean shouldPause() {
        return false;
    }

    /**
     * The previewed stack is decoration only, so dropping every action here also means no
     * click can ever reach {@code clickSlot} and leak a packet for this client-only window.
     */
    @Override
    protected void onMouseClick(Slot slot, int slotId, int button,
            SlotActionType actionType) {
    }

    /**
     * Draws the single slot according to the frame's actual content: an empty frame
     * gets the QuickCraft ghost layering of the expected icon, a wrong item is washed
     * red, and a matching item is drawn untouched.
     *
     * <p>The GUI depth maps z to NDC as {@code -z / 10000}, so the icon (z=150) only
     * stays visible when everything drawn before it sits farther away (z=0) and
     * everything drawn after it sits closer (z=300); a closer layer drawn first would
     * fail the icon's depth test and hide it completely.
     */
    @Override
    protected void drawSlot(DrawContext context, Slot slot) {
        ItemStack actual = this.actualStack();
        PreviewState state = previewState(actual);

        if (state == PreviewState.MISSING) {
            context.fill(slot.x, slot.y, slot.x + ITEM_SIZE, slot.y + ITEM_SIZE,
                    FramePreviewPalette.fillColor(state));
            // The GUI shader applies the global colour modulator when the buffered
            // vertices flush, so flushing the pending background first and then drawing
            // the item while the modulator carries the ghost alpha renders a genuinely
            // translucent icon through vanilla's own GUI pipeline.
            context.draw();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, FramePreviewPalette.ghostItemAlpha());
            context.drawItem(this.content, slot.x, slot.y);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            context.fill(slot.x, slot.y, slot.x + ITEM_SIZE, slot.y + ITEM_SIZE, TINT_DEPTH,
                    FramePreviewPalette.ghostMaskColor(state));
            this.drawOutline(context, slot, FramePreviewPalette.borderColor(state), TINT_DEPTH);
            return;
        }

        if (state == PreviewState.WRONG_COMPONENTS) {
            context.fill(slot.x, slot.y, slot.x + ITEM_SIZE, slot.y + ITEM_SIZE,
                    FramePreviewPalette.fillColor(PreviewState.WRONG_COMPONENTS));
            this.drawOutline(context, slot, FramePreviewPalette.borderColor(state), 0);
        }

        context.drawItem(actual, slot.x, slot.y);
    }

    /**
     * Resolved while drawing so the verdict tracks the real frame even while the
     * preview stays open.
     */
    private ItemStack actualStack() {
        ItemFrameEntity frame = ProjectionAimScanner.findRealItemFrame(MinecraftClient.getInstance(), this.pos);
        return frame != null ? frame.getHeldItemStack() : ItemStack.EMPTY;
    }

    private void drawOutline(DrawContext context, Slot slot, int color, int z) {
        int left = slot.x;
        int top = slot.y;
        int right = left + ITEM_SIZE;
        int bottom = top + ITEM_SIZE;
        context.fill(left, top, right, top + 1, z, color);
        context.fill(left, bottom - 1, right, bottom, z, color);
        context.fill(left, top + 1, left + 1, bottom - 1, z, color);
        context.fill(right - 1, top + 1, right, bottom - 1, z, color);
    }

    private PreviewState previewState(ItemStack actual) {
        return switch (FrameContentAvailability.classifyFrameContent(actual, this.content)) {
            case MISSING -> PreviewState.MISSING;
            case WRONG_COMPONENTS -> PreviewState.WRONG_COMPONENTS;
            case PRESENT -> PreviewState.PRESENT;
        };
    }

    private Text frameLabel() {
        return Text.translatable(key(this.glowFrame ? "glow" : "plain"),
                Integer.toString(this.pos.getX()),
                Integer.toString(this.pos.getY()),
                Integer.toString(this.pos.getZ()));
    }

    private boolean isOverItem(double mouseX, double mouseY) {
        int left = this.x + SLOT_X;
        int top = this.y + SLOT_Y;

        return mouseX >= left && mouseX < left + ITEM_SIZE && mouseY >= top && mouseY < top + ITEM_SIZE;
    }

    private boolean isReadableBook() {
        return this.content.get(DataComponentTypes.WRITTEN_BOOK_CONTENT) != null;
    }

    private static String key(String name) {
        return NotAGoodModForSurvival.MOD_ID + ".gui.frameContentPreview." + name;
    }

    /**
     * A client-only one slot container that never reaches the server. The slot stack is
     * resolved live so the hover tooltip mirrors what the slot draws: the expected item
     * for an empty frame, and the item actually placed otherwise.
     */
    private static final class PreviewScreenHandler extends ScreenHandler {
        private final ItemStack content;
        private final BlockPos pos;

        PreviewScreenHandler(SimpleInventory inventory, ItemStack content, BlockPos pos) {
            super(ScreenHandlerType.GENERIC_9X1, PREVIEW_SYNC_ID);
            this.content = content;
            this.pos = pos;
            this.addSlot(new Slot(inventory, 0, SLOT_X, SLOT_Y) {
                @Override
                public ItemStack getStack() {
                    return PreviewScreenHandler.this.hoverStack();
                }
            });
        }

        private ItemStack hoverStack() {
            ItemFrameEntity frame = ProjectionAimScanner.findRealItemFrame(
                    MinecraftClient.getInstance(), this.pos);
            ItemStack actual = frame != null ? frame.getHeldItemStack() : ItemStack.EMPTY;
            return FrameContentAvailability.classifyFrameContent(actual, this.content) == Availability.MISSING
                    ? this.content
                    : actual;
        }

        @Override
        public ItemStack quickMove(PlayerEntity player, int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return false;
        }
    }
}
