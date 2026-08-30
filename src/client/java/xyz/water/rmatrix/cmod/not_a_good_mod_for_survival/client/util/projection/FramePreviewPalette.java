package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection;

/**
 * Slot overlay colours for the projected item frame preview.
 *
 * <p>The colour values and their alpha and lighten steps follow the design of
 * yiyihehe's QuickCraft container verifier palette (MIT license); only the idea
 * and the numbers are referenced, no code was copied.
 * Source: https://github.com/yiyihehe/quickcraft
 */
public final class FramePreviewPalette {
    /** How the hotbar relates to the item a projected frame wants. */
    public enum PreviewState {
        PRESENT,
        MISSING,
        WRONG_COMPONENTS
    }

    private static final int RGB_MISSING = 0x2979FF;
    private static final int RGB_WRONG = 0xFF1744;
    private static final int SLOT_FILL_ALPHA = 0x48;
    private static final int SLOT_BORDER_ALPHA = 0xD8;
    private static final int GHOST_MASK_ALPHA = 0x78;
    private static final float BORDER_LIGHTEN = 0.45F;
    private static final float MASK_LIGHTEN = 0.78F;
    private static final float GHOST_ITEM_ALPHA = 0.80F;

    private FramePreviewPalette() {
    }

    /** Returns the ARGB wash drawn over the slot, or 0 when nothing is wrong. */
    public static int fillColor(PreviewState state) {
        return state == PreviewState.PRESENT ? 0 : withAlpha(rgb(state), SLOT_FILL_ALPHA);
    }

    /** Returns the ARGB slot outline colour, or 0 when nothing is wrong. */
    public static int borderColor(PreviewState state) {
        return state == PreviewState.PRESENT
                ? 0
                : withAlpha(mixTowardWhite(rgb(state), BORDER_LIGHTEN), SLOT_BORDER_ALPHA);
    }

    /** Returns the ARGB mask washed over the translucent ghost item of a missing slot. */
    public static int ghostMaskColor(PreviewState state) {
        return state == PreviewState.PRESENT
                ? 0
                : withAlpha(mixTowardWhite(rgb(state), MASK_LIGHTEN), GHOST_MASK_ALPHA);
    }

    /** Returns the alpha the ghost item icon is blitted back with. */
    public static float ghostItemAlpha() {
        return GHOST_ITEM_ALPHA;
    }

    private static int rgb(PreviewState state) {
        return state == PreviewState.MISSING ? RGB_MISSING : RGB_WRONG;
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0x00FFFFFF);
    }

    private static int mixTowardWhite(int rgb, float amount) {
        float clamped = Math.max(0.0F, Math.min(1.0F, amount));
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        int mixedRed = red + Math.round((255 - red) * clamped);
        int mixedGreen = green + Math.round((255 - green) * clamped);
        int mixedBlue = blue + Math.round((255 - blue) * clamped);
        return (mixedRed << 16) | (mixedGreen << 8) | mixedBlue;
    }
}
