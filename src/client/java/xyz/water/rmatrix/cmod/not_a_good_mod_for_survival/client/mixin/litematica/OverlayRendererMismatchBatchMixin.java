package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Works around Litematica submitting an empty vertex batch while drawing verifier
 * mismatch markers. When the selected mismatch list holds only the block under the
 * crosshair, the batched-outline pass has no vertices, vanilla's BufferBuilder.end()
 * throws "BufferBuilder was empty" and Litematica logs it once per frame. That batch
 * had nothing to draw, so only the repeated warning is dropped; every other failure
 * still reports, and the hovered marker (drawn separately at a wider line width) is
 * untouched.
 */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.render.OverlayRenderer", remap = false)
public abstract class OverlayRendererMismatchBatchMixin {
    @WrapOperation(method = "renderSchematicMismatches", remap = false,
            at = @At(value = "INVOKE", remap = false,
                    target = "Lorg/apache/logging/log4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V"))
    private void notAGoodModForSurvival$ignoreEmptyBatchWarning(Logger logger, String message, Object argument,
            Operation<Void> original) {
        if ("BufferBuilder was empty".equals(argument)) {
            return;
        }

        original.call(logger, message, argument);
    }
}
