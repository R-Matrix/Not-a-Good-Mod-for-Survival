package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.entity_view;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {

    @Inject(method = "renderHitbox", at = @At("TAIL"))
    private static void notAGoodModForSurvival$drawViewArrow(
            MatrixStack matrices,
            VertexConsumer vertices,
            Entity entity,
            float tickDelta,
            float red,
            float green,
            float blue,
            CallbackInfo info) {
        if (!RenderConfigs.DebugRender.ENTITY_VIEW_ARROW.getBooleanValue()) {
            return;
        }

        Vec3d direction = entity.getRotationVec(tickDelta).normalize();
        Vec3d origin = new Vec3d(0.0, entity.getStandingEyeHeight(), 0.0);
        Vec3d tip = origin.add(direction.multiply(2.0));
        Vec3d base = tip.subtract(direction.multiply(0.35));

        Vec3d reference = Math.abs(direction.y) < 0.9
                ? new Vec3d(0.0, 1.0, 0.0)
                : new Vec3d(0.0, 0.0, 1.0);
        Vec3d side = direction.crossProduct(reference).normalize().multiply(0.16);
        Vec3d vertical = side.normalize().crossProduct(direction).normalize().multiply(0.16);

        notAGoodModForSurvival$drawArrowSegment(matrices, vertices, tip, base.add(side).add(vertical));
        notAGoodModForSurvival$drawArrowSegment(matrices, vertices, tip, base.add(side).subtract(vertical));
        notAGoodModForSurvival$drawArrowSegment(matrices, vertices, tip, base.subtract(side).add(vertical));
        notAGoodModForSurvival$drawArrowSegment(matrices, vertices, tip, base.subtract(side).subtract(vertical));
    }

    @Unique
    private static void notAGoodModForSurvival$drawArrowSegment(
            MatrixStack matrices, VertexConsumer vertices, Vec3d start, Vec3d end) {
        VertexRendering.drawVector(
                matrices,
                vertices,
                new Vector3f((float) start.x, (float) start.y, (float) start.z),
                end.subtract(start),
                -16776961
        );
    }
}
