package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import fi.dy.masa.litematica.util.RayTraceUtils;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper;
import fi.dy.masa.litematica.util.RayTraceUtils.RayTraceWrapper.HitType;
import fi.dy.masa.litematica.util.WorldUtils;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.projection.FrameInteractionMath;

/**
 * Resolves which projected item frame or lectern the player is aiming at.
 *
 * <p>All data comes from Litematica's schematic world, which already stores the
 * placements with mirror, rotation and sub-region offsets applied, so no schematic
 * position maths is duplicated here.
 *
 * <p>A frame counts as aimed at only when the view ray crosses the entity's own bounding
 * box, which for an item frame is the thin plate in front of its support block rather
 * than the whole block cell. That mirrors how vanilla picks entities, so looking at the
 * edge of a wall no longer selects a frame that is nowhere near the crosshair.
 *
 * <p>The plate is rebuilt from the entity position instead of {@code getBoundingBox()},
 * because Litematica moves schematic decorations with {@code refreshPositionAndAngles}
 * after loading them from NBT and never refreshes their bounding box; a box-based test
 * would aim at the schematic-local coordinates instead of the placed position.
 */
public final class ProjectionAimScanner {
    private static final Predicate<Entity> ITEM_FRAMES = entity -> entity instanceof ItemFrameEntity;
    /** The bounding box offset of an item frame is about 0.47 blocks towards its support. */
    private static final double MIN_FACING_OFFSET = 0.125D;
    /** Vanilla widens the candidate search area by one block before the precise box test. */
    private static final double CANDIDATE_EXPANSION = 1.0D;
    private static final int NO_AXIS = -1;

    /** The kinds of projected content this mod can inspect. */
    public enum Kind {
        ITEM_FRAME,
        LECTERN
    }

    /**
     * @param kind  the kind of projected content that was found
     * @param cell  the block cell the projected content occupies
     * @param frame the projected item frame, or null for a lectern
     * @param book  the book stored on the projected lectern, empty when there is none
     */
    public record Target(Kind kind, BlockPos cell, @Nullable ItemFrameEntity frame, ItemStack book) {
    }

    private ProjectionAimScanner() {
    }

    /** Returns the projected content the player aims at, or null when there is none. */
    @Nullable
    public static Target findTarget(MinecraftClient client) {
        WorldSchematic schematicWorld = SchematicWorldHandler.getSchematicWorld();

        if (schematicWorld == null || client.world == null || client.player == null) {
            return null;
        }

        BlockHitResult traceHit = traceAimedBlock(client, WorldUtils.getValidBlockRange(client));
        double reach = entityPickReach(client, traceHit);
        ItemFrameEntity frame = findAimedFrame(client, schematicWorld, reach, traceHit);

        if (frame != null) {
            return new Target(Kind.ITEM_FRAME, frame.getBlockPos(), frame, ItemStack.EMPTY);
        }

        if (traceHit != null && isProjectedLectern(schematicWorld, traceHit.getBlockPos())) {
            BlockPos cell = traceHit.getBlockPos();
            return new Target(Kind.LECTERN, cell, null, readLecternBook(schematicWorld, cell));
        }

        return null;
    }

    /**
     * Returns the projected item frame whose plate the view ray hits, or null when the
     * crosshair is not on one. {@code reach} bounds how far an aim still counts, and
     * {@code aimedBlock} is the block the player is looking at, which hides anything
     * behind it; null skips that comparison.
     */
    @Nullable
    public static ItemFrameEntity findAimedFrame(MinecraftClient client, WorldSchematic schematicWorld,
            double reach, @Nullable BlockHitResult aimedBlock) {
        if (client.player == null || reach <= 0.0D) {
            return null;
        }

        Vec3d eye = client.player.getCameraPosVec(1.0F);
        Vec3d lookEnd = eye.add(client.player.getRotationVec(1.0F).multiply(reach));
        double budget = reach * reach;

        if (aimedBlock != null && aimedBlock.getType() != HitResult.Type.MISS) {
            budget = Math.min(budget, eye.squaredDistanceTo(aimedBlock.getPos()));
        }

        List<? extends Entity> candidates = schematicWorld.getOtherEntities(null,
                new Box(eye, lookEnd).expand(CANDIDATE_EXPANSION), ITEM_FRAMES);
        ItemFrameEntity closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity entity : candidates) {
            if (!(entity instanceof ItemFrameEntity frame)) {
                continue;
            }

            int axis = facingAxis(frame);

            if (axis == NO_AXIS) {
                continue;
            }

            Box box = framePlateBox(frame, axis);
            Optional<Vec3d> hit = box.raycast(eye, lookEnd);
            double distance;

            if (hit.isPresent()) {
                distance = eye.squaredDistanceTo(hit.get());
            } else if (box.contains(eye)) {
                // Standing right inside the thin plate still counts as aiming at it.
                distance = 0.0D;
            } else {
                continue;
            }

            if (distance <= budget && distance < closestDistance) {
                closestDistance = distance;
                closest = frame;
            }
        }

        return closest;
    }

    /** Returns the projected item frame hanging in {@code cell}, if any. */
    @Nullable
    public static ItemFrameEntity findFrameAt(WorldSchematic schematicWorld, BlockPos cell) {
        for (Entity entity : schematicWorld.getOtherEntities(null, new Box(cell), ITEM_FRAMES)) {
            if (entity.getBlockPos().equals(cell)) {
                return (ItemFrameEntity) entity;
            }
        }

        return null;
    }

    /**
     * Returns how far a projected frame may sit for a hover to count, which is vanilla's
     * entity interaction range and never further than the block being looked at.
     */
    public static double entityPickReach(MinecraftClient client, @Nullable BlockHitResult aimedBlock) {
        if (client.player == null) {
            return 0.0D;
        }

        double reach = client.player.getEntityInteractionRange();

        if (aimedBlock != null && aimedBlock.getType() != HitResult.Type.MISS) {
            reach = Math.min(reach, client.player.getCameraPosVec(1.0F).distanceTo(aimedBlock.getPos()));
        }

        return reach;
    }

    /**
     * Returns the direction a projected item frame faces. The frame entity sits offset
     * from its cell centre towards its support block, and Litematica does transform
     * that position with the placement while leaving the bounding box stale, so the
     * facing is read from the position offset rather than from the box.
     */
    @Nullable
    public static Direction getFrameFacing(ItemFrameEntity frame) {
        Vec3d offset = Vec3d.ofCenter(frame.getBlockPos()).subtract(frame.getPos());
        int axis = FrameInteractionMath.dominantAxis(offset.x, offset.y, offset.z);
        double dominant = axis == 0 ? offset.x : (axis == 1 ? offset.y : offset.z);

        if (Math.abs(dominant) < MIN_FACING_OFFSET) {
            return null;
        }

        return Direction.from(Direction.Axis.values()[axis],
                FrameInteractionMath.isPositiveAxisDirection(dominant)
                        ? Direction.AxisDirection.POSITIVE
                        : Direction.AxisDirection.NEGATIVE);
    }

    /** Returns the axis a frame plate is thin on, or {@link #NO_AXIS} when undeterminable. */
    private static int facingAxis(ItemFrameEntity frame) {
        Vec3d offset = Vec3d.ofCenter(frame.getBlockPos()).subtract(frame.getPos());
        int axis = FrameInteractionMath.dominantAxis(offset.x, offset.y, offset.z);
        double dominant = axis == 0 ? offset.x : (axis == 1 ? offset.y : offset.z);

        return Math.abs(dominant) >= MIN_FACING_OFFSET ? axis : NO_AXIS;
    }

    /**
     * Returns the thin plate box of a frame centred on its entity position, with the
     * vanilla thickness along {@code axis}, widened by the vanilla targeting margin.
     */
    private static Box framePlateBox(ItemFrameEntity frame, int axis) {
        double sizeX = axis == 0 ? 0.0625D : 0.75D;
        double sizeY = axis == 1 ? 0.0625D : 0.75D;
        double sizeZ = axis == 2 ? 0.0625D : 0.75D;

        return Box.of(frame.getPos(), sizeX, sizeY, sizeZ).expand(frame.getTargetingMargin());
    }

    /** Returns whether the projection wants a lectern in {@code cell}. */
    public static boolean isProjectedLectern(WorldSchematic schematicWorld, BlockPos cell) {
        return schematicWorld.getBlockEntity(cell) instanceof LecternBlockEntity
                || schematicWorld.getBlockState(cell).isOf(Blocks.LECTERN);
    }

    /** Returns the book stored on the projected lectern, or {@link ItemStack#EMPTY}. */
    public static ItemStack readLecternBook(WorldSchematic schematicWorld, BlockPos cell) {
        if (schematicWorld.getBlockEntity(cell) instanceof LecternBlockEntity lectern) {
            return lectern.getBook();
        }

        return ItemStack.EMPTY;
    }

    /** Returns the item frame the client world already holds in {@code cell}, if any. */
    @Nullable
    public static ItemFrameEntity findRealItemFrame(MinecraftClient client, BlockPos cell) {
        if (client.world == null) {
            return null;
        }

        for (Entity entity : client.world.getOtherEntities(null, new Box(cell), ITEM_FRAMES)) {
            if (entity.getBlockPos().equals(cell)) {
                return (ItemFrameEntity) entity;
            }
        }

        return null;
    }

    /** Returns whether an item frame already stands in that cell of the real world. */
    public static boolean hasRealItemFrame(MinecraftClient client, BlockPos cell) {
        return findRealItemFrame(client, cell) != null;
    }

    @Nullable
    private static BlockHitResult traceAimedBlock(MinecraftClient client, double range) {
        RayTraceWrapper wrapper = RayTraceUtils.getGenericTrace(client.world, client.player, range);

        if (wrapper == null) {
            return null;
        }

        HitType type = wrapper.getHitType();

        if (type != HitType.SCHEMATIC_BLOCK && type != HitType.VANILLA_BLOCK) {
            return null;
        }

        return wrapper.getBlockHitResult();
    }
}
