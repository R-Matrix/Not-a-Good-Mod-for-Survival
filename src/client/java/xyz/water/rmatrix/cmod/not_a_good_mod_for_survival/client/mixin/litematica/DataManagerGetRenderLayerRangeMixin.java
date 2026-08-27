package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fi.dy.masa.malilib.util.LayerRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.SchematicRenderRangeManager;

/**
 * Serves a range-aware view of Litematica's global layer range, so every query
 * that consults it (easy place, ghost picking, paste validation, ...) respects
 * the saved projection display ranges.
 */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.data.DataManager", remap = false)
public abstract class DataManagerGetRenderLayerRangeMixin {
    @ModifyReturnValue(method = "getRenderLayerRange", at = @At("RETURN"), remap = false)
    private static LayerRange notAGoodModForSurvival$wrapRenderLayerRange(LayerRange original) {
        return SchematicRenderRangeManager.wrapLayerRangeIfApplicable(original);
    }
}
