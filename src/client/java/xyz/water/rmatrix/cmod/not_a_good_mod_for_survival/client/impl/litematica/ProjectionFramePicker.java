package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay.GameplayConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FrameContentAvailability;

/**
 * Supply helpers that put the item a projected item frame needs into the player's
 * hand, in creative mode by creating it and in survival mode by moving an existing
 * stack from the inventory.
 *
 * <p>Creative supply follows vanilla pick-block semantics: a matching stack already in
 * the inventory is selected or swapped to the hand instead of being duplicated, and a
 * newly created stack never swallows whatever the target slot already holds (the old
 * stack is first moved to a free hotbar or inventory slot; only a completely full
 * inventory is overwritten). Creation is mirrored to the server with the creative
 * inventory packet; the rest goes through plain SWAP container clicks against the
 * player's own inventory screen handler.
 */
public final class ProjectionFramePicker {
    /** The player inventory screen slot offset of the first hotbar slot. */
    private static final int HOTBAR_SCREEN_SLOT_OFFSET = 36;
    /** The first screen slot of the non-hotbar main inventory area. */
    private static final int MAIN_AREA_START = 9;
    /** One past the last screen slot of the non-hotbar main inventory area. */
    private static final int MAIN_AREA_END = 36;
    private static final int NO_SLOT = -1;

    private ProjectionFramePicker() {
    }

    /** Returns whether the creative frame supply aids may act right now. */
    public static boolean isEnabled(MinecraftClient client) {
        return GameplayConfigs.ProjectionAids.ENABLE_CREATIVE_FRAME_SUPPLY.getBooleanValue()
                && client.player != null
                && client.player.isCreative()
                && client.interactionManager != null;
    }

    /**
     * Picks the aimed projected item frame into the current hotbar slot: the frame item
     * itself when the projected frame is empty, or the framed item with its components
     * otherwise. Returns true when the click belonged to this feature.
     */
    public static boolean tryPick(MinecraftClient client) {
        // A screen owns the middle mouse button (quick move in containers, creative pick
        // in the creative inventory); never intercept it while one is open.
        if (client.currentScreen != null) {
            return false;
        }

        boolean creative = isEnabled(client);
        boolean survival = ProjectionSurvivalSupply.isEnabled(client);

        if ((!creative && !survival) || client.world == null) {
            return false;
        }

        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null) {
            return false;
        }

        BlockHitResult aimedBlock = client.crosshairTarget instanceof BlockHitResult hit ? hit : null;
        ItemFrameEntity frame = ProjectionAimScanner.findAimedFrame(client, schematicWorld,
                ProjectionAimScanner.entityPickReach(client, aimedBlock), aimedBlock);

        if (frame == null) {
            return false;
        }

        ItemStack content = frame.getHeldItemStack();
        ItemStack pick = content.isEmpty()
                ? new ItemStack(frame.getType() == EntityType.GLOW_ITEM_FRAME
                        ? Items.GLOW_ITEM_FRAME
                        : Items.ITEM_FRAME)
                : content.copy();

        PlayerInventory inventory = client.player.getInventory();

        if (creative) {
            creativePickToMainHand(client, inventory, pick);
            return true;
        }

        // Survival supply: bring an existing stack (or the frame itself) from the
        // inventory into the main hand, matching the creative middle-click behaviour.
        // A plain frame matches any frame item in the inventory; a framed item must
        // match exactly, components included.
        int slot = content.isEmpty()
                ? ProjectionSurvivalSupply.bringItemToMainHand(client, inventory,
                        frame.getType() == EntityType.GLOW_ITEM_FRAME
                                ? Items.GLOW_ITEM_FRAME
                                : Items.ITEM_FRAME)
                : ProjectionSurvivalSupply.bringStackToMainHand(client, inventory, pick);

        if (slot == NO_SLOT) {
            if (content.isEmpty()) {
                ProjectionAidMessages.print("frameItemMissingInv");
            } else {
                ProjectionAidMessages.print("frameContentMissingInv", pick.getName().getString());
            }
        }

        return true;
    }

    /**
     * Brings {@code stack} into a hotbar slot and selects it without swallowing or
     * moving what the player was holding, following vanilla pick-block semantics.
     * Returns the hotbar slot now holding the stack, or {@link #NO_SLOT} when creative
     * supply cannot act.
     */
    public static int creativePickToMainHand(MinecraftClient client, PlayerInventory inventory, ItemStack stack) {
        if (!isEnabled(client) || stack.isEmpty() || client.player == null || client.interactionManager == null
                || client.currentScreen != null) {
            return NO_SLOT;
        }

        int selected = inventory.selectedSlot;
        int existing = inventory.getSlotWithStack(stack);

        if (existing >= 0) {
            if (existing < FrameContentAvailability.HOTBAR_SLOTS) {
                inventory.setSelectedSlot(existing);
                syncSelectedSlot(client, existing);
                return existing;
            }

            // A backpack copy is moved into the hand when the hand is empty, or into a
            // free hotbar slot otherwise; the items already in the hotbar stay exactly
            // where they are. Only when every hotbar slot is taken does the stack take
            // over the selected slot.
            int free = inventory.getStack(selected).isEmpty()
                    ? selected
                    : findFreeHotbarSlot(inventory, selected, NO_SLOT);

            if (free != NO_SLOT) {
                if (client.currentScreen == null) {
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                            existing, free, SlotActionType.SWAP, client.player);
                }

                inventory.setSelectedSlot(free);
                syncSelectedSlot(client, free);
                return free;
            }

            if (client.currentScreen == null) {
                client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                        existing, selected, SlotActionType.SWAP, client.player);
            }

            return selected;
        }

        // No matching stack in the inventory: creative creates one, moving the old hand
        // item out of the way first so nothing is lost.
        makeHotbarSlotEmptyNoSwallow(client, inventory, selected);

        inventory.setStack(selected, stack.copy());
        client.interactionManager.clickCreativeStack(inventory.getStack(selected),
                HOTBAR_SCREEN_SLOT_OFFSET + selected);
        return selected;
    }

    /**
     * Brings a stack of {@code item} (any components) into a hotbar slot without
     * swallowing or moving what the player was holding, used when placing a projected
     * frame so an existing frame item anywhere in the inventory is reused before a new
     * one is created. Returns the hotbar slot now holding the item, or {@link #NO_SLOT}.
     */
    public static int creativePickItemToMainHand(MinecraftClient client, PlayerInventory inventory, Item item) {
        if (!isEnabled(client) || client.player == null || client.interactionManager == null
                || client.currentScreen != null) {
            return NO_SLOT;
        }

        int selected = inventory.selectedSlot;
        int existing = findSlotForItem(inventory, item);

        if (existing >= 0) {
            if (existing < FrameContentAvailability.HOTBAR_SLOTS) {
                inventory.setSelectedSlot(existing);
                syncSelectedSlot(client, existing);
                return existing;
            }

            // A backpack copy is moved into the hand when the hand is empty, or into a
            // free hotbar slot otherwise; the items already in the hotbar stay exactly
            // where they are. Only when every hotbar slot is taken does the stack take
            // over the selected slot.
            int free = inventory.getStack(selected).isEmpty()
                    ? selected
                    : findFreeHotbarSlot(inventory, selected, NO_SLOT);

            if (free != NO_SLOT) {
                if (client.currentScreen == null) {
                    client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                            existing, free, SlotActionType.SWAP, client.player);
                }

                inventory.setSelectedSlot(free);
                syncSelectedSlot(client, free);
                return free;
            }

            if (client.currentScreen == null) {
                client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                        existing, selected, SlotActionType.SWAP, client.player);
            }

            return selected;
        }

        makeHotbarSlotEmptyNoSwallow(client, inventory, selected);

        inventory.setStack(selected, new ItemStack(item));
        client.interactionManager.clickCreativeStack(inventory.getStack(selected),
                HOTBAR_SCREEN_SLOT_OFFSET + selected);
        return selected;
    }

    /**
     * Supplies the exact content stack into a hotbar slot other than {@code excluded}
     * (the frame slot) so the auto-print burst can select it for the fill, without
     * swallowing whatever the target slot holds. Returns the hotbar slot, or
     * {@link #NO_SLOT} when creative supply cannot act.
     */
    public static int creativeSupplyContent(MinecraftClient client, PlayerInventory inventory,
            ItemStack stack, int excluded) {
        if (!isEnabled(client) || stack.isEmpty() || client.player == null || client.interactionManager == null
                || client.currentScreen != null) {
            return NO_SLOT;
        }

        int selected = inventory.selectedSlot;
        int target = findFreeHotbarSlot(inventory, excluded, selected);

        if (target == NO_SLOT) {
            target = (excluded + 1) % FrameContentAvailability.HOTBAR_SLOTS;

            if (target == selected) {
                target = (excluded + 2) % FrameContentAvailability.HOTBAR_SLOTS;
            }
        }

        makeHotbarSlotEmptyNoSwallow(client, inventory, target);

        inventory.setStack(target, stack.copy());
        client.interactionManager.clickCreativeStack(inventory.getStack(target),
                HOTBAR_SCREEN_SLOT_OFFSET + target);
        return target;
    }

    /**
     * Moves the content of hotbar slot {@code hotbarSlot} to a free hotbar slot, then a
     * free inventory slot, so the slot can be reused without losing its item. When the
     * slot is empty or no other slot is free, the inventory is left unchanged; a full
     * inventory is the caller's overwrite case.
     */
    private static void makeHotbarSlotEmptyNoSwallow(MinecraftClient client, PlayerInventory inventory,
            int hotbarSlot) {
        if (inventory.getStack(hotbarSlot).isEmpty()
                || client.currentScreen != null || client.player == null || client.interactionManager == null) {
            return;
        }

        int freeHotbar = findFreeHotbarSlot(inventory, hotbarSlot, NO_SLOT);

        if (freeHotbar != NO_SLOT) {
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                    HOTBAR_SCREEN_SLOT_OFFSET + hotbarSlot, freeHotbar, SlotActionType.SWAP, client.player);
            return;
        }

        int freeBackpack = findFreeBackpackSlot(inventory);

        if (freeBackpack != NO_SLOT) {
            client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                    freeBackpack, hotbarSlot, SlotActionType.SWAP, client.player);
        }
    }

    /** Sends the current selected hotbar slot to the server. */
    private static void syncSelectedSlot(MinecraftClient client, int slot) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    /** Returns the first inventory slot holding {@code item}, ignoring components, or {@link #NO_SLOT}. */
    private static int findSlotForItem(PlayerInventory inventory, Item item) {
        for (int index = 0; index < inventory.main.size(); ++index) {
            if (inventory.getStack(index).isOf(item)) {
                return index;
            }
        }

        return NO_SLOT;
    }

    /** Returns the first free hotbar slot other than the excluded slots, or {@link #NO_SLOT}. */
    private static int findFreeHotbarSlot(PlayerInventory inventory, int excluded, int alsoExcluded) {
        for (int slot = 0; slot < FrameContentAvailability.HOTBAR_SLOTS; ++slot) {
            if (slot != excluded && slot != alsoExcluded && inventory.getStack(slot).isEmpty()) {
                return slot;
            }
        }

        return NO_SLOT;
    }

    /** Returns the first free non-hotbar inventory slot, or {@link #NO_SLOT}. */
    private static int findFreeBackpackSlot(PlayerInventory inventory) {
        for (int index = MAIN_AREA_START; index < MAIN_AREA_END; ++index) {
            if (inventory.getStack(index).isEmpty()) {
                return index;
            }
        }

        return NO_SLOT;
    }
}