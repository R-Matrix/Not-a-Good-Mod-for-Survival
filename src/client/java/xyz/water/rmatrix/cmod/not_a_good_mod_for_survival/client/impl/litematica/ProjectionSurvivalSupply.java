package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import java.util.function.Predicate;

import fi.dy.masa.litematica.config.Configs;
import fi.dy.masa.litematica.util.InventoryUtils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.gameplay.GameplayConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FrameContentAvailability;

/**
 * Survival-mode helpers that bring the item a projected item frame needs into the
 * player's main hand from anywhere in the inventory.
 *
 * <p>Hand switching is delegated to Litematica's pick-block supply path
 * ({@code setPickedItemToHand}), so the target hotbar slot obeys Litematica's
 * {@code PICK_BLOCK_AVOID_DAMAGEABLE} and {@code PICK_BLOCK_AVOID_TOOLS} rules and a
 * matching stack is moved with a {@link SlotActionType#SWAP} container click against
 * the player's own inventory screen handler. When no direct stack is present and
 * Litematica's {@code PICK_BLOCK_SHULKERS} is enabled, a shulker box containing the
 * item is brought to the hand instead, exactly like Litematica's schematic pick-block.
 */
public final class ProjectionSurvivalSupply {
    /** The first inventory index of the main (non-hotbar) area; it equals the player-screen slot id. */
    private static final int MAIN_AREA_START = 9;
    /** One past the last inventory index of the main area. */
    private static final int MAIN_AREA_END = 36;

    private ProjectionSurvivalSupply() {
    }

    /** Returns whether the survival frame supply aids may act right now. */
    public static boolean isEnabled(MinecraftClient client) {
        return GameplayConfigs.ProjectionAids.ENABLE_SURVIVAL_FRAME_SUPPLY.getBooleanValue()
                && client.player != null
                && client.player.isCreative() == false
                && client.interactionManager != null;
    }

    /**
     * Brings a stack of {@code item} (any components) into the main hand and returns
     * the hotbar slot now holding it, or {@link FrameContentAvailability#NO_SLOT}.
     */
    public static int bringItemToMainHand(MinecraftClient client, PlayerInventory inventory, Item item) {
        return bringToMainHand(client, inventory, stack -> stack.isOf(item), new ItemStack(item));
    }

    /**
     * Brings a stack matching {@code expected} on item and components into the main
     * hand and returns the hotbar slot now holding it, or {@link FrameContentAvailability#NO_SLOT}.
     */
    public static int bringStackToMainHand(MinecraftClient client, PlayerInventory inventory, ItemStack expected) {
        return bringToMainHand(client, inventory,
                stack -> ItemStack.areItemsAndComponentsEqual(stack, expected), expected);
    }

    /**
     * Puts the exact {@code expected} stack into a hotbar slot other than the main hand
     * so the auto-print burst can select it for the fill. Returns the hotbar slot, or
     * {@link FrameContentAvailability#NO_SLOT} when the item is nowhere to be found.
     */
    public static int supplyContentToHotbar(MinecraftClient client, PlayerInventory inventory,
            ItemStack expected, int frameSlot) {
        int selected = inventory.selectedSlot;

        // An exact match already in the hotbar can be used as-is, except on the frame
        // slot: the placement and the fill select different slots, so the content must
        // never overwrite the frame the placement is about to consume.
        for (int slot = 0; slot < FrameContentAvailability.HOTBAR_SLOTS; ++slot) {
            if (slot != frameSlot && ItemStack.areItemsAndComponentsEqual(inventory.getStack(slot), expected)) {
                return slot;
            }
        }

        if (client.currentScreen != null || client.player == null || client.interactionManager == null) {
            return FrameContentAvailability.NO_SLOT;
        }

        int targetSlot = findEmptyHotbarSlot(inventory, frameSlot);

        if (targetSlot == FrameContentAvailability.NO_SLOT) {
            // Everything is occupied; displace a slot that is neither the frame slot nor
            // the slot the player currently holds, so the frame stays available for the
            // placement and the hand keeps the item the player put there.
            for (int slot = 0; slot < FrameContentAvailability.HOTBAR_SLOTS; ++slot) {
                if (slot != frameSlot && slot != selected) {
                    targetSlot = slot;
                    break;
                }
            }
        }

        if (targetSlot == FrameContentAvailability.NO_SLOT) {
            return FrameContentAvailability.NO_SLOT;
        }

        int screenSlot = findMainAreaSlot(inventory,
                stack -> ItemStack.areItemsAndComponentsEqual(stack, expected));

        if (screenSlot == FrameContentAvailability.NO_SLOT) {
            return FrameContentAvailability.NO_SLOT;
        }

        client.interactionManager.clickSlot(client.player.playerScreenHandler.syncId,
                screenSlot, targetSlot, SlotActionType.SWAP, client.player);

        return ItemStack.areItemsAndComponentsEqual(inventory.getStack(targetSlot), expected)
                ? targetSlot
                : FrameContentAvailability.NO_SLOT;
    }

    /**
     * Looks the requested stack up in the order a player would expect: the main hand,
     * then the other hotbar slots, then the main inventory area. A swap from the main
     * area only happens while no screen is open, because the SWAP click targets the
     * player's own inventory screen handler.
     */
    private static int bringToMainHand(MinecraftClient client, PlayerInventory inventory,
            Predicate<ItemStack> matches, ItemStack reference) {
        if (client.player == null || client.interactionManager == null) {
            return FrameContentAvailability.NO_SLOT;
        }

        int selected = inventory.selectedSlot;

        if (matches.test(inventory.getMainHandStack())) {
            return selected;
        }

        for (int slot = 0; slot < FrameContentAvailability.HOTBAR_SLOTS; ++slot) {
            if (slot != selected && matches.test(inventory.getStack(slot))) {
                inventory.setSelectedSlot(slot);
                return slot;
            }
        }

        if (client.currentScreen != null) {
            return FrameContentAvailability.NO_SLOT;
        }

        ItemStack toHand = findDirectOrShulkerStack(client, inventory, matches, reference);

        if (toHand.isEmpty()) {
            return FrameContentAvailability.NO_SLOT;
        }

        try {
            InventoryUtils.setPickedItemToHand(toHand, client);
        } catch (LinkageError | RuntimeException exception) {
            return FrameContentAvailability.NO_SLOT;
        }

        // The pick-block swap only succeeds when the stack ends up in a hotbar slot;
        // a failure leaves it in the main area, which is not a valid result.
        int slot = inventory.getSlotWithStack(toHand);

        return PlayerInventory.isValidHotbarIndex(slot) ? slot : FrameContentAvailability.NO_SLOT;
    }

    /**
     * Returns the stack Litematica should bring to the hand: a direct match from the
     * main inventory area, or when none exists and Litematica's {@code PICK_BLOCK_SHULKERS}
     * is enabled, the first shulker box containing the requested item. Empty when there
     * is nothing to bring.
     */
    private static ItemStack findDirectOrShulkerStack(MinecraftClient client, PlayerInventory inventory,
            Predicate<ItemStack> matches, ItemStack reference) {
        int screenSlot = findMainAreaSlot(inventory, matches);

        if (screenSlot != FrameContentAvailability.NO_SLOT) {
            return inventory.getStack(screenSlot);
        }

        try {
            if (client.player != null && Configs.Generic.PICK_BLOCK_SHULKERS.getBooleanValue()) {
                int boxSlot = InventoryUtils.findSlotWithBoxWithItem(
                        client.player.playerScreenHandler, reference, false);

                if (boxSlot >= 0) {
                    return client.player.playerScreenHandler.slots.get(boxSlot).getStack();
                }
            }
        } catch (LinkageError | RuntimeException exception) {
            return ItemStack.EMPTY;
        }

        return ItemStack.EMPTY;
    }

    /** Returns the first empty hotbar slot other than {@code excluded}, or {@link FrameContentAvailability#NO_SLOT}. */
    private static int findEmptyHotbarSlot(PlayerInventory inventory, int excluded) {
        for (int slot = 0; slot < FrameContentAvailability.HOTBAR_SLOTS; ++slot) {
            if (slot != excluded && inventory.getStack(slot).isEmpty()) {
                return slot;
            }
        }

        return FrameContentAvailability.NO_SLOT;
    }

    /** Returns the first main-area inventory index matching, which equals the player-screen slot id. */
    private static int findMainAreaSlot(PlayerInventory inventory, Predicate<ItemStack> matches) {
        for (int index = MAIN_AREA_START; index < MAIN_AREA_END; ++index) {
            if (matches.test(inventory.getStack(index))) {
                return index;
            }
        }

        return FrameContentAvailability.NO_SLOT;
    }
}
