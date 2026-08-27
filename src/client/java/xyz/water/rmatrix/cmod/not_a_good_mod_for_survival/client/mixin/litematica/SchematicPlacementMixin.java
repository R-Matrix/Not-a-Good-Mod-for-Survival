package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.SchematicRenderRangeManager;

/**
 * Applies the saved display ranges at Litematica's data layer so verification,
 * paste/delete tasks and the per-chunk volume cache all observe the same limits
 * as the renderer.
 */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.schematic.placement.SchematicPlacement", remap = false)
public abstract class SchematicPlacementMixin {
    @ModifyReturnValue(method = "getBoxesWithinChunk", at = @At("RETURN"), remap = false)
    private ImmutableMap<String, IntBoundingBox> notAGoodModForSurvival$filterBoxesWithinChunk(
            ImmutableMap<String, IntBoundingBox> original
    ) {
        return SchematicRenderRangeManager.filterBoxesForData((SchematicPlacement) (Object)this, original);
    }

    /** Switching between ALL and RENDER_LAYERS changes the cached data volumes. */
    @Inject(method = "setSchematicVerifierType", at = @At("TAIL"), remap = false)
    private void notAGoodModForSurvival$onVerifierTypeChanged(BlockInfoListType type, CallbackInfo ci) {
        SchematicRenderRangeManager.onPlacementDataModeChanged((SchematicPlacement) (Object) this);
    }
}
