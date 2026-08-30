package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection;

/** Pure maths shared by the projection item-frame helpers. */
public final class FrameInteractionMath {
    /** Item frames have eight discrete item rotations. */
    public static final int ROTATION_STEPS = 8;

    private FrameInteractionMath() {
    }

    /** Wraps an item rotation into the 0..7 range vanilla stores. */
    public static int normalizeRotation(int rotation) {
        int normalized = rotation % ROTATION_STEPS;

        return normalized < 0 ? normalized + ROTATION_STEPS : normalized;
    }

    /** Returns how many empty-hand frame clicks rotate a frame from one rotation to another. */
    public static int requiredRotationClicks(int current, int target) {
        return normalizeRotation(normalizeRotation(target) - normalizeRotation(current));
    }

    /** Returns the 0-based axis index of the dominant component of an offset. */
    public static int dominantAxis(double x, double y, double z) {
        double absoluteX = Math.abs(x);
        double absoluteY = Math.abs(y);
        double absoluteZ = Math.abs(z);

        if (absoluteX >= absoluteY && absoluteX >= absoluteZ) {
            return 0;
        }

        return absoluteY >= absoluteZ ? 1 : 2;
    }

    /** Returns whether an offset points towards the positive end of its axis. */
    public static boolean isPositiveAxisDirection(double value) {
        return value >= 0.0D;
    }
}
