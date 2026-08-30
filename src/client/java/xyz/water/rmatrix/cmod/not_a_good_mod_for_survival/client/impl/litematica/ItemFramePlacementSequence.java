package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jetbrains.annotations.Nullable;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.tool.ToolMode;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay.GameplayConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FrameContentAvailability;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FrameInteractionMath;

/**
 * Places a projected item frame through ordinary use interactions.
 *
 * <p>Litematica's easy place only handles block states, and item frames are entities in
 * this Minecraft version, so a frame is placed by clicking the support face with it,
 * exactly like a manual placement would.
 *
 * <p>By default that is all a single click does, so frames go in one per click like any
 * other projected block. Auto-printing additionally fills in the framed item and restores
 * its stored rotation. Vanilla only ever turns a frame one eighth of a turn per ordinary
 * use interaction and offers no packet that sets an absolute rotation, so the chain is
 * issued as one burst of ordinary interactions the moment the frame appears: the server
 * applies packets in the order they arrive on the connection, which lets a frame land
 * filled and already turned in a tick or two instead of one confirmed round trip per
 * step. The result is then read back from the server sync and any missing turns are
 * topped up.
 *
 * <p>A frame is only offered at all once Litematica can actually be drawing it (see
 * {@link ProjectionEasyPlaceGate}), so projections that have not rendered yet cannot be
 * built through blind.
 */
public final class ItemFramePlacementSequence {
    /** How long one phase may wait for the server before the rest of the chain is dropped. */
    private static final int TIMEOUT_TICKS = 40;
    private static final int MIN_TICKS_BETWEEN_STARTS = 5;
    /** Extra top-up bursts allowed when the server did not apply every turn. */
    private static final int MAX_ROTATION_CORRECTIONS = 2;
    private static final int NO_SLOT = -1;
    /** A single in-game hint per session that auto-print is what fills and turns frames. */
    private static final AtomicBoolean AUTO_PRINT_HINT_SHOWN = new AtomicBoolean();

    private enum Step {
        /** The place packet is out, waiting for the frame to appear in the client world. */
        WAIT_FRAME,
        /** Issue the fill and every turn as one ordered run of ordinary interactions. */
        BURST,
        /** Reading the result back from the server sync and topping up missing turns. */
        CONFIRM
    }

    @Nullable
    private static ItemFramePlacementSequence active;
    private static int lastStartTick = Integer.MIN_VALUE / 2;

    private final BlockPos frameCell;
    private final int contentSlot;
    private final int targetRotation;
    private final int restoreSlot;

    private Step step = Step.WAIT_FRAME;
    private int waitedTicks;
    private int correctionsLeft = MAX_ROTATION_CORRECTIONS;

    private ItemFramePlacementSequence(BlockPos frameCell, int contentSlot,
            int targetRotation, int restoreSlot) {
        this.frameCell = frameCell;
        this.contentSlot = contentSlot;
        this.targetRotation = targetRotation;
        this.restoreSlot = restoreSlot;
    }

    /**
     * Starts placing the item frame the projection asks for at the aimed block face.
     * Returns true when the click belongs to this feature and must not be handled twice.
     */
    public static boolean tryStart(MinecraftClient client) {
        if (active != null
                || !GameplayConfigs.ProjectionAids.ENABLE_ITEM_FRAME_EASY_PLACE.getBooleanValue()
                || !Configs.Generic.EASY_PLACE_MODE.getBooleanValue()) {
            return false;
        }

        if (client.world == null || client.player == null || client.interactionManager == null
                || !(client.crosshairTarget instanceof BlockHitResult hit)
                || DataManager.getToolMode() == ToolMode.REBUILD) {
            return false;
        }

        int sinceLastStart = client.player.age - lastStartTick;

        // A negative gap means the player clock went backwards, which happens on respawn.
        if (sinceLastStart >= 0 && sinceLastStart < MIN_TICKS_BETWEEN_STARTS) {
            return false;
        }

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null) {
            return false;
        }

        // The aim must land on the projected plate itself, not merely on its support face,
        // and the reach is Litematica's own block reach because this ends in a block click.
        ItemFrameEntity projectedFrame = ProjectionAimScanner.findAimedFrame(client, schematicWorld,
                WorldUtils.getValidBlockRange(client), hit);

        if (projectedFrame == null) {
            return false;
        }

        Direction facing = ProjectionAimScanner.getFrameFacing(projectedFrame);
        BlockPos frameCell = projectedFrame.getBlockPos();

        // The plate the crosshair landed on must be the one hanging in front of the aimed
        // face, so the frame never gets configured from a neighbour's projection.
        if (facing != hit.getSide() || !frameCell.equals(hit.getBlockPos().offset(hit.getSide()))) {
            return false;
        }

        if (ProjectionAimScanner.hasRealItemFrame(client, frameCell)
                || client.world.getBlockState(hit.getBlockPos()).isAir()) {
            return false;
        }

        // A frame Litematica cannot be drawing yet is not built through blind.
        if (!ProjectionEasyPlaceGate.isFramePlaceable(schematicWorld, frameCell)) {
            return false;
        }

        PlayerInventory inventory = client.player.getInventory();
        Item frameItem = projectedFrame.getType() == EntityType.GLOW_ITEM_FRAME
                ? Items.GLOW_ITEM_FRAME
                : Items.ITEM_FRAME;
        int frameSlot = FrameContentAvailability.findSlotForItem(inventory, frameItem);

        if (frameSlot == NO_SLOT) {
            boolean survivalSupply = ProjectionSurvivalSupply.isEnabled(client);

            if (ProjectionFramePicker.isEnabled(client)) {
                // Creative mode: reuse an existing frame from anywhere in the inventory,
                // or create one without swallowing the current hand item.
                frameSlot = ProjectionFramePicker.creativePickItemToMainHand(client,
                        inventory, frameItem);
            } else if (survivalSupply) {
                // Survival mode: bring the frame from anywhere in the inventory into
                // the current hotbar slot, exactly like Litematica's pick-block supply.
                frameSlot = ProjectionSurvivalSupply.bringItemToMainHand(client, inventory, frameItem);
            }

            if (frameSlot == NO_SLOT) {
                ProjectionAidMessages.print(survivalSupply ? "frameItemMissingInv" : "frameItemMissing");
                return false;
            }
        }

        boolean autoPrint = GameplayConfigs.ProjectionAids.ENABLE_ITEM_FRAME_AUTO_PRINT.getBooleanValue();
        int contentSlot = NO_SLOT;

        if (autoPrint) {
            ItemStack projectedItem = projectedFrame.getHeldItemStack();

            if (!projectedItem.isEmpty()) {
                contentSlot = FrameContentAvailability.findSlotForExactStack(inventory, projectedItem);

                // The content must sit in a slot other than the frame slot: the placement
                // and the fill select different hotbar slots, so a frame-in-frame projection
                // must not reuse the slot that is about to be consumed for the placement.
                if (contentSlot == frameSlot) {
                    contentSlot = NO_SLOT;
                }

                if (contentSlot == NO_SLOT) {
                    if (ProjectionFramePicker.isEnabled(client)) {
                        // Creative mode: supply the exact item, components included, into
                        // a hotbar slot other than the frame slot without swallowing what
                        // the target slot holds; the burst selects it for the fill.
                        contentSlot = ProjectionFramePicker.creativeSupplyContent(client,
                                inventory, projectedItem, frameSlot);
                    } else if (ProjectionSurvivalSupply.isEnabled(client)) {
                        // Survival mode: move the exact item, components included, from
                        // the inventory into a hotbar slot other than the main hand so the
                        // burst can select it for the fill.
                        contentSlot = ProjectionSurvivalSupply.supplyContentToHotbar(client,
                                inventory, projectedItem, frameSlot);
                    }

                    if (contentSlot == NO_SLOT) {
                        ProjectionAidMessages.print(
                                ProjectionSurvivalSupply.isEnabled(client)
                                        ? "frameContentMissingInv"
                                        : "frameContentMissing",
                                projectedItem.getName().getString());
                    }
                }
            }
        }

        int targetRotation = FrameInteractionMath.normalizeRotation(projectedFrame.getRotation());

        // An empty frame cannot be turned, and only auto-print makes the turns worth it.
        if (!autoPrint || contentSlot == NO_SLOT
                || !GameplayConfigs.ProjectionAids.PLACE_ITEM_FRAME_ROTATION.getBooleanValue()) {
            targetRotation = 0;
        }

        int restoreSlot = inventory.selectedSlot;
        lastStartTick = client.player.age;
        inventory.setSelectedSlot(frameSlot);
        ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hit);

        if (!result.isAccepted()) {
            inventory.setSelectedSlot(restoreSlot);
            ProjectionAidMessages.print("framePlaceFailed");
            return true;
        }

        active = new ItemFramePlacementSequence(frameCell, contentSlot, targetRotation, restoreSlot);

        // With auto-print off a placed frame stays empty, which the player usually only
        // notices after the click; one hint per session makes the dependency visible.
        if (!autoPrint && AUTO_PRINT_HINT_SHOWN.compareAndSet(false, true)) {
            ProjectionAidMessages.print("frameAutoPrintHint");
        }

        return true;
    }

    /**
     * Puts the held item into the item frame the player is aiming at, but only when the
     * projection asks for exactly that item including its components. Without this the
     * easy place click would place a block through the frame instead of filling it.
     */
    public static boolean tryInsertContent(MinecraftClient client) {
        if (!GameplayConfigs.ProjectionAids.ENABLE_ITEM_FRAME_EASY_PLACE.getBooleanValue()
                || !Configs.Generic.EASY_PLACE_MODE.getBooleanValue()) {
            return false;
        }

        if (client.world == null || client.player == null || client.interactionManager == null
                || DataManager.getToolMode() == ToolMode.REBUILD
                || !(client.crosshairTarget instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof ItemFrameEntity frame)
                || frame.isRemoved()
                || !frame.getHeldItemStack().isEmpty()) {
            return false;
        }

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null) {
            return false;
        }

        ItemFrameEntity projectedFrame = ProjectionAimScanner.findFrameAt(schematicWorld, frame.getBlockPos());

        if (projectedFrame == null) {
            return false;
        }

        if (!ProjectionEasyPlaceGate.isFramePlaceable(schematicWorld, frame.getBlockPos())) {
            return false;
        }

        ItemStack expected = projectedFrame.getHeldItemStack();

        if (expected.isEmpty()) {
            // The projection wants an empty frame, so this is not a projected action.
            return false;
        }

        ItemStack held = client.player.getMainHandStack();

        if (!ItemStack.areItemsAndComponentsEqual(held, expected)) {
            // Survival supply may bring the exact item from the inventory into the main
            // hand, after which the fill can go ahead; otherwise fall back to the checks
            // that decide whether the wrong hand should block the interaction.
            if (ProjectionSurvivalSupply.isEnabled(client)
                    && ProjectionSurvivalSupply.bringStackToMainHand(client,
                            client.player.getInventory(), expected) != NO_SLOT) {
                held = client.player.getMainHandStack();

                if (ItemStack.areItemsAndComponentsEqual(held, expected)) {
                    return client.interactionManager.interactEntity(client.player, frame, Hand.MAIN_HAND).isAccepted();
                }
            }

            if (!held.isOf(expected.getItem())) {
                return false;
            }

            // Same item but the wrong components, for example a different map or book.
            ProjectionAidMessages.print("frameContentMismatch", expected.getName().getString());
            return true;
        }

        return client.interactionManager.interactEntity(client.player, frame, Hand.MAIN_HAND).isAccepted();
    }

    /** Advances a running sequence. Safe to call every client tick. */
    public static void tick(MinecraftClient client) {
        ItemFramePlacementSequence sequence = active;

        if (sequence == null) {
            return;
        }

        if (client.world == null || client.player == null || client.interactionManager == null) {
            finish(client);
            return;
        }

        sequence.tickStep(client);
    }

    private void tickStep(MinecraftClient client) {
        if (++this.waitedTicks >= TIMEOUT_TICKS) {
            ProjectionAidMessages.print("frameSequenceTimeout");
            finish(client);
            return;
        }

        ItemFrameEntity frame = ProjectionAimScanner.findRealItemFrame(client, this.frameCell);

        if (frame == null) {
            return;
        }

        switch (this.step) {
            // The burst goes out in the same tick the frame first shows up, so the player
            // never sees an empty or unrotated intermediate state.
            case WAIT_FRAME, BURST -> this.sendBurst(client, frame);
            case CONFIRM -> this.confirm(client, frame);
        }
    }

    /**
     * Fills the frame and turns it to the stored rotation in one tick. Turns only take
     * effect on a frame that already holds an item and the server applies packets in the
     * order they arrive on the connection, so the fill simply goes out first and the turns
     * follow it immediately. A freshly placed frame starts unrotated, which makes the turn
     * count known up front instead of something to be probed one round trip at a time.
     */
    private void sendBurst(MinecraftClient client, ItemFrameEntity frame) {
        this.waitedTicks = 0;
        PlayerInventory inventory = client.player.getInventory();

        if (this.contentSlot != NO_SLOT) {
            inventory.setSelectedSlot(this.contentSlot);
            client.interactionManager.interactEntity(client.player, frame, Hand.MAIN_HAND);
        }

        this.rotate(client, frame, this.targetRotation);
        this.step = Step.CONFIRM;
    }

    /**
     * Finishes once the server sync shows the framed item and the stored rotation. Missing
     * turns are re-sent in one burst, because a dropped or rejected interaction is the only
     * way the count can end up short; a frame that never shows its item at all is dropped by
     * the phase timeout instead.
     */
    private void confirm(MinecraftClient client, ItemFrameEntity frame) {
        if (this.contentSlot != NO_SLOT && frame.getHeldItemStack().isEmpty()) {
            return;
        }

        int missing = FrameInteractionMath.requiredRotationClicks(
                FrameInteractionMath.normalizeRotation(frame.getRotation()), this.targetRotation);

        if (missing == 0) {
            finish(client);
            return;
        }

        if (this.correctionsLeft-- <= 0) {
            ProjectionAidMessages.print("frameSequenceTimeout");
            finish(client);
            return;
        }

        this.waitedTicks = 0;
        this.rotate(client, frame, missing);
    }

    /**
     * Turns the frame the given number of eighths. Each turn is an ordinary use
     * interaction; since 1.21.4 rotates a frame that already holds an item no matter
     * what the hand carries, the turns simply go out with the hand the fill just used
     * and nothing has to be reserved or restored for them.
     */
    private void rotate(MinecraftClient client, ItemFrameEntity frame, int clicks) {
        if (clicks <= 0) {
            return;
        }

        for (int click = 0; click < clicks; ++click) {
            client.interactionManager.interactEntity(client.player, frame, Hand.MAIN_HAND);
        }
    }

    private static void finish(MinecraftClient client) {
        ItemFramePlacementSequence sequence = active;
        active = null;

        if (sequence == null || client.player == null) {
            return;
        }

        PlayerInventory inventory = client.player.getInventory();

        if (inventory.selectedSlot != sequence.restoreSlot) {
            inventory.setSelectedSlot(sequence.restoreSlot);

            // The vanilla manager only syncs the selection on the next interaction, so
            // tell the server about the restored hotbar slot right away when it can hear.
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(sequence.restoreSlot));
            }
        }
    }
}
