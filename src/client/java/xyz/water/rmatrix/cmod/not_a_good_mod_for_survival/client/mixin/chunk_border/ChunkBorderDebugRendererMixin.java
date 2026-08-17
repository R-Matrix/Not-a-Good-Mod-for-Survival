package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.chunk_border;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.debug.ChunkBorderDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.render.chunk_border.ChunkBorderLineRenderer;

@Mixin(ChunkBorderDebugRenderer.class)
public abstract class ChunkBorderDebugRendererMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void notAGoodModForSurvival$renderThickRedAndBlueLines(
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            double cameraX,
            double cameraY,
            double cameraZ,
            CallbackInfo ci
    ) {
        ChunkBorderLineRenderer.render(
                matrices,
                vertexConsumers,
                cameraX,
                cameraY,
                cameraZ
        );
    }
}
