package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Answers how the item actually sitting in an item frame relates to the item a
 * projected frame asks for.
 *
 * <p>The same rules back the preview tint and the auto-print chain, so a preview can
 * never promise something the following click would refuse.
 */
public final class FrameContentAvailability {
    /** The number of quick use slots a player has. */
    public static final int HOTBAR_SLOTS = 9;
    /** Returned instead of a slot index when nothing matched. */
    public static final int NO_SLOT = -1;

    /** How well the hotbar relates to one projected item. */
    public enum Availability {
        /** The same item with the same components is present. */
        PRESENT,
        /** The item is present but carries different components, such as another map id. */
        WRONG_COMPONENTS,
        /** The item is not in the hotbar at all. */
        MISSING
    }

    private FrameContentAvailability() {
    }

    /** Returns the first hotbar slot holding {@code item}, ignoring components. */
    public static int findSlotForItem(PlayerInventory inventory, Item item) {
        for (int slot = 0; slot < HOTBAR_SLOTS; ++slot) {
            if (inventory.getStack(slot).isOf(item)) {
                return slot;
            }
        }

        return NO_SLOT;
    }

    /** Returns the first hotbar slot matching {@code expected} on item and components. */
    public static int findSlotForExactStack(PlayerInventory inventory, ItemStack expected) {
        if (expected.isEmpty()) {
            return NO_SLOT;
        }

        for (int slot = 0; slot < HOTBAR_SLOTS; ++slot) {
            if (ItemStack.areItemsAndComponentsEqual(inventory.getStack(slot), expected)) {
                return slot;
            }
        }

        return NO_SLOT;
    }

    /**
     * Classifies how the item actually placed in a frame relates to the expected item.
     * An empty actual slot is missing, anything that is not the expected item with the
     * same components is wrong, and everything else is present.
     */
    public static Availability classifyFrameContent(ItemStack actual, ItemStack expected) {
        if (expected.isEmpty()) {
            return Availability.PRESENT;
        }

        if (actual.isEmpty()) {
            return Availability.MISSING;
        }

        return ItemStack.areItemsAndComponentsEqual(actual, expected)
                ? Availability.PRESENT
                : Availability.WRONG_COMPONENTS;
    }
}
