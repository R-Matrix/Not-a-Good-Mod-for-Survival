package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

import net.minecraft.client.MinecraftClient;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.ItemFramePlacementSequence;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.ProjectionContentPreview;

/**
 * Extends Litematica's easy place with projected item frames, which easy place cannot
 * handle because frames are entities in this Minecraft version.
 *
 * <p>Litematica consumes the use click once this wrapper reports a handled action, so
 * the frame placement never ends up being performed a second time by vanilla. A sneak
 * gesture that only inspects projected content is likewise kept away from easy place,
 * which would otherwise place a block on the same click.
 */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.util.WorldUtils", remap = false)
public abstract class WorldUtilsProjectionAidMixin {
    @WrapMethod(method = "handleEasyPlace", remap = false)
    private static boolean notAGoodModForSurvival$handleEasyPlace(MinecraftClient mc,
            Operation<Boolean> original) {
        // Only the actual use click lands here, which keeps projected frames at one per click.
        if (ProjectionContentPreview.handleUseClick(mc, true, false)
                || ItemFramePlacementSequence.tryInsertContent(mc)
                || ItemFramePlacementSequence.tryStart(mc)) {
            return true;
        }

        return original.call(mc);
    }

    /**
     * The hold-to-place path runs every client tick, so it only ever gets the preview
     * suppression here. Placing projected frames stays strictly one click per frame.
     */
    @WrapMethod(method = "easyPlaceOnUseTick", remap = false)
    private static void notAGoodModForSurvival$easyPlaceOnUseTick(MinecraftClient mc,
            Operation<Void> original) {
        if (ProjectionContentPreview.handleUseClick(mc, true, false)) {
            return;
        }

        original.call(mc);
    }
}
