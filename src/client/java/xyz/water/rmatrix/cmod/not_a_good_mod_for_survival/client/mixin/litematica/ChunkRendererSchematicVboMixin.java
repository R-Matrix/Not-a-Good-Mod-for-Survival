package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.util.IntBoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.SchematicRenderRangeManager;

/** Clips only the selected projection's chunk-cache volumes to its independent display range. */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.render.schematic.ChunkRendererSchematicVbo", remap = false)
public abstract class ChunkRendererSchematicVboMixin {
    @WrapOperation(method = "rebuildWorldView",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
                    remap = false), remap = false)
    private boolean notAGoodModForSurvival$clipPlacementPart(
            List<IntBoundingBox> boxes, Object value, Operation<Boolean> original,
            @Local(name = "part") SchematicPlacementManager.PlacementPart part) {
        if (value instanceof IntBoundingBox box) {
            List<IntBoundingBox> clipped = SchematicRenderRangeManager.clipPlacementParts(part, box);
            for (IntBoundingBox result : clipped) {
                original.call(boxes, result);
            }
            return !clipped.isEmpty();
        }
        return original.call(boxes, value);
    }
}
