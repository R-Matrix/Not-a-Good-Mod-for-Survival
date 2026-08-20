package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.interaction;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

/** Finds the synthetic hit used for Bedrock-style horizontal forward placement. */
public final class ForwardBridgePlacement {
    private ForwardBridgePlacement() {
    }

    public static BlockHitResult findPlacementHit(MinecraftClient client) {
        if (client.world == null || client.player == null || client.crosshairTarget == null
                || client.crosshairTarget.getType() != HitResult.Type.MISS) {
            return null;
        }

        ClientPlayerEntity player = client.player;
        Direction direction = player.getHorizontalFacing();
        BlockPos supportPos = player.getBlockPos().down();
        BlockPos targetPos = supportPos.offset(direction);

        if (!client.world.getWorldBorder().contains(targetPos)
                || client.world.getBlockState(supportPos).isAir()
                || !client.world.getBlockState(targetPos).isAir()) {
            return null;
        }

        Vec3d cameraPos = player.getCameraPosVec(1.0F);
        Vec3d rayEnd = cameraPos.add(player.getRotationVec(1.0F).multiply(player.getBlockInteractionRange()));
        Optional<Vec3d> targetHit = new Box(targetPos).raycast(cameraPos, rayEnd);
        if (targetHit.isEmpty()) {
            return null;
        }

        return new BlockHitResult(targetHit.get(), direction, supportPos, false);
    }
}
