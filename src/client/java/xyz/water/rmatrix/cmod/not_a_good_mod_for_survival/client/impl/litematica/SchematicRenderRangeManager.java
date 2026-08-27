package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.util.BlockInfoListType;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.JsonUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.CornerEditMode;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica.SchematicPlacementManagerCacheAccess;

/** Owns the per-projection display range and the small Litematica-style range editor. */
public final class SchematicRenderRangeManager implements IMouseInputHandler {
    private static final SchematicRenderRangeManager INSTANCE = new SchematicRenderRangeManager();
    private static final String FILE_NAME = NotAGoodModForSurvival.MOD_ID + ".schematic-render-ranges.json";

    private static final String MESSAGE_NO_PLACEMENT_SELECTED =
            NotAGoodModForSurvival.MOD_ID + ".message.renderRange.noPlacementSelected";
    private static final String MESSAGE_EDITOR_DISABLED =
            NotAGoodModForSurvival.MOD_ID + ".message.renderRange.editorDisabled";
    private static final String MESSAGE_EDITOR_ENABLED =
            NotAGoodModForSurvival.MOD_ID + ".message.renderRange.editorEnabled";
    private static final String MESSAGE_NO_EDITABLE_RANGE =
            NotAGoodModForSurvival.MOD_ID + ".message.renderRange.noEditableRange";
    private static final String MESSAGE_CORNER_EDIT_MODE =
            NotAGoodModForSurvival.MOD_ID + ".message.renderRange.cornerEditMode";
    private static final String MESSAGE_FEATURE_DISABLED =
            NotAGoodModForSurvival.MOD_ID + ".message.renderRange.featureDisabled";
    private static final String MESSAGE_RANGE_RESET =
            NotAGoodModForSurvival.MOD_ID + ".message.renderRange.rangeReset";

    private final Map<UUID, RangeSelection> ranges = new HashMap<>();
    private final Map<UUID, ClipModel> clipModels = new ConcurrentHashMap<>();
    private boolean loaded;
    private boolean editing;
    private UUID editingPlacementId;
    private int selectedCorner = -1;
    private UUID lastObservedPlacementId;
    private String lastObservedState;
    private String lastObservedClipConfig;
    private LayerRange cachedLayerRangeDelegate;
    private RenderRangeFilteringLayerRange cachedLayerRangeWrapper;
    private boolean loggedWrapState;

    private SchematicRenderRangeManager() {
    }

    public static SchematicRenderRangeManager getInstance() {
        return INSTANCE;
    }

    public static void tick(MinecraftClient client) {
        INSTANCE.tickInternal();
    }

    private void tickInternal() {
        ensureLoaded();

        String clipConfig = String.valueOf(RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue());
        if (!Objects.equals(this.lastObservedClipConfig, clipConfig)) {
            this.lastObservedClipConfig = clipConfig;
            this.clipModels.clear();
            for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
                refreshPlacementVolumes(placement);
            }
            fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.clearActiveVerifiers();
            markAllPlacementsForRebuild();
        }

        SchematicPlacement placement = getSelectedPlacement();
        UUID placementId = placement == null ? null : placement.getHashId();
        String state = placement == null ? "none" : placement.getOrigin() + ":" + placement.getRotation()
                + ":" + placement.getMirror() + ":" + RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()
                + ":" + ranges.get(placementId);

        if (!Objects.equals(this.lastObservedState, state)) {
            if (this.lastObservedPlacementId != null) {
                markChunksForRebuild(this.lastObservedPlacementId);
            }
            if (placementId != null) {
                markChunksForRebuild(placementId);
            }
            this.lastObservedPlacementId = placementId;
            this.lastObservedState = state;
        }

        if (this.editing && (placement == null || !placement.getHashId().equals(this.editingPlacementId))) {
            this.editing = false;
            this.selectedCorner = -1;
        }
    }

    public boolean toggleEditor() {
        ensureLoaded();
        SchematicPlacement placement = getSelectedPlacement();

        if (placement == null) {
            InfoUtils.printActionbarMessage(MESSAGE_NO_PLACEMENT_SELECTED);
            return true;
        }

        if (this.editing) {
            this.editing = false;
            this.selectedCorner = -1;
            markChunksForRebuild(placement.getHashId());
            this.lastObservedState = null;
            InfoUtils.printActionbarMessage(MESSAGE_EDITOR_DISABLED);
            return true;
        }

        RangeSelection range = this.ranges.get(placement.getHashId());
        if (range == null) {
            range = createDefaultRange(placement);
            if (range == null) {
                InfoUtils.printActionbarMessage(MESSAGE_NO_EDITABLE_RANGE);
                return true;
            }
            this.ranges.put(placement.getHashId(), range);
            save();
        }

        this.editing = true;
        this.editingPlacementId = placement.getHashId();
        this.selectedCorner = -1;
        markChunksForRebuild(placement.getHashId());
        this.lastObservedState = null;
        InfoUtils.printActionbarMessage(MESSAGE_EDITOR_ENABLED);
        return true;
    }

    public boolean isEditing() {
        return this.editing;
    }

    /** Hotkey entry point to cycle the corner edit mode (corners <-> expand). */
    public boolean cycleCornerEditMode() {
        CornerEditMode next = RenderConfigs.SchematicRenderRange.getCornerEditMode().cycle(true);
        RenderConfigs.SchematicRenderRange.CORNER_EDIT_MODE.setOptionListValue(next);
        InfoUtils.printActionbarMessage(MESSAGE_CORNER_EDIT_MODE, next.getDisplayName());
        return true;
    }

    /** Hotkey entry point: resets the selected projection's display range. */
    public boolean resetSelectedRangeToProjectionBox() {
        ensureLoaded();
        SchematicPlacement placement = getSelectedPlacement();

        if (!RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()) {
            InfoUtils.printActionbarMessage(MESSAGE_FEATURE_DISABLED);
            return true;
        }
        if (placement == null) {
            InfoUtils.printActionbarMessage(MESSAGE_NO_PLACEMENT_SELECTED);
            return true;
        }

        return resetRangeToProjectionBox(placement);
    }

    /** Resets the given projection's display range back to its enclosing box. */
    public boolean resetRangeToProjectionBox(SchematicPlacement placement) {
        ensureLoaded();
        RangeSelection fresh = createDefaultRange(placement);
        if (fresh == null) {
            InfoUtils.printActionbarMessage(MESSAGE_NO_EDITABLE_RANGE);
            return false;
        }

        this.ranges.put(placement.getHashId(), fresh);
        this.selectedCorner = -1;
        save();
        refreshPlacementVolumes(placement);
        fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.clearActiveVerifiers();
        markChunksForRebuild(placement.getHashId());
        this.lastObservedState = null;
        InfoUtils.printActionbarMessage(MESSAGE_RANGE_RESET);
        return true;
    }

    /**
     * Data-layer view of the saved ranges, consumed by schematic verification,
     * paste/delete tasks and the volume cache. Unlike the renderer path this
     * clips every volume to its intersection with the saved range, but only
     * while the projection verifies in render-layers mode.
     */
    public static ImmutableMap<String, IntBoundingBox> filterBoxesForData(
            SchematicPlacement placement, java.util.Map<String, IntBoundingBox> boxes) {
        if (!isPlacementDataClipped(placement)) {
            return ImmutableMap.copyOf(boxes);
        }

        IntBoundingBox range = getPlacementClipBox(placement);
        LinkedHashMap<String, IntBoundingBox> filtered = new LinkedHashMap<>();
        boolean droppedAny = false;

        for (Map.Entry<String, IntBoundingBox> entry : boxes.entrySet()) {
            IntBoundingBox box = entry.getValue();

            if (range != null && range.intersects(box)) {
                filtered.put(entry.getKey(), intersectBoxes(range, box));
            } else {
                droppedAny = true;
            }
        }

        if (droppedAny) {
            NotAGoodModForSurvival.LOGGER.debug(
                    "[render-range] Data-layer filter clipped {} of {} chunk volumes.",
                    boxes.size() - filtered.size(), boxes.size());
        }

        return ImmutableMap.copyOf(filtered);
    }

    /**
     * Forces Litematica to rebuild its cached per-chunk volumes for one placement,
     * so data-side consumers observe range changes without a placement reload.
     */
    public static void refreshPlacementVolumes(SchematicPlacement placement) {
        fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager manager =
                DataManager.getSchematicPlacementManager();
        SchematicPlacementManagerCacheAccess access = (SchematicPlacementManagerCacheAccess) manager;

        for (ChunkPos pos : placement.getTouchedChunks()) {
            access.notAGoodModForSurvival$invokeUpdateTouchedBoxesInChunk(pos);
        }
    }

    /** True while tool queries should respect the saved display ranges. */
    public static boolean shouldRestrictQueries() {
        return RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()
                && !INSTANCE.isEditing();
    }

    /**
     * Strict "rendered content" test behind Litematica's global layer range
     * queries: easy place, ghost picking, paste and delete all consult it before
     * touching a position. A saved partial render range is enough to restrict
     * these content queries: unlike the data-side gates (verifier, paste/delete,
     * material list) this intentionally does not depend on the projection's
     * verifier type, so easy place behaves exactly like the rendered content.
     */
    public static boolean isPositionWithinRenderedContent(int x, int y, int z) {
        if (!shouldRestrictQueries()) {
            return true;
        }

        List<SchematicPlacement> placements =
                DataManager.getSchematicPlacementManager().getAllSchematicsPlacements();
        boolean anyClipped = false;

        for (SchematicPlacement placement : placements) {
            ClipModel model = resolveClipModel(placement);
            if (!isContentClipped(model)) {
                continue;
            }

            anyClipped = true;
            if (containsPosition(model.clipBox(), x, y, z)) {
                return true;
            }
        }

        if (!anyClipped) {
            return true;
        }

        // Projections that are not clipped are still rendered in full, so they stay usable.
        for (SchematicPlacement placement : placements) {
            ClipModel model = resolveClipModel(placement);
            if (model.rendered() && !isContentClipped(model) && containsPosition(model.envelopeBox(), x, y, z)) {
                return true;
            }
        }

        return false;
    }

    /** Content-query (easy place, ghost picking) clipping: a partial render range is enough. */
    private static boolean isContentClipped(ClipModel model) {
        return model.rendered() && model.rangeIsPartial();
    }

    /**
     * Serves a cached, range-aware view of the delegate layer range while the
     * feature restricts queries; otherwise returns the original instance.
     */
    public static LayerRange wrapLayerRangeIfApplicable(LayerRange original) {
        if (!shouldRestrictQueries()) {
            if (INSTANCE.loggedWrapState) {
                INSTANCE.loggedWrapState = false;
                NotAGoodModForSurvival.LOGGER.info(
                        "[render-range] LayerRange queries unrestricted (feature off, editing, or no ranges).");
            }
            return original;
        }

        if (INSTANCE.cachedLayerRangeDelegate != original || INSTANCE.cachedLayerRangeWrapper == null) {
            INSTANCE.cachedLayerRangeDelegate = original;
            INSTANCE.cachedLayerRangeWrapper = new RenderRangeFilteringLayerRange(original);
        }

        // Several inherited LayerRange accessors, plus LayerRange.CODEC, read the
        // boundary fields directly, so refresh this wrapper's snapshot on every hand-off.
        INSTANCE.cachedLayerRangeWrapper.syncStateFromDelegate();

        if (!INSTANCE.loggedWrapState) {
            INSTANCE.loggedWrapState = true;
            NotAGoodModForSurvival.LOGGER.info(
                    "[render-range] LayerRange queries now restricted by saved projection ranges.");
        }

        return INSTANCE.cachedLayerRangeWrapper;
    }

    /**
     * Data-side gate for one projection: the saved display range only clamps
     * Litematica's data queries while that projection verifies in render-layers
     * mode, which is exactly when Litematica itself honours a layer range.
     */
    public static boolean isPlacementDataClipped(@Nullable SchematicPlacement placement) {
        if (placement == null
                || !RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()
                || INSTANCE.isEditing()) {
            return false;
        }

        return isClippedModel(resolveClipModel(placement));
    }

    /** The saved display range of a projection, or null when it covers everything. */
    @Nullable
    public static IntBoundingBox getPlacementClipBox(SchematicPlacement placement) {
        ClipModel model = resolveClipModel(placement);
        return model.rangeIsPartial() ? model.clipBox() : null;
    }

    /**
     * Material list gate: a counted position only contributes while the list runs in
     * render-layers mode and the position lies inside the projection's saved range.
     */
    public static boolean isPositionCountedWithinRenderRange(SchematicPlacement placement,
            IMaterialList materialList, int x, int y, int z) {
        if (placement == null
                || materialList == null
                || !RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()
                || INSTANCE.isEditing()
                || materialList.getMaterialListType() != BlockInfoListType.RENDER_LAYERS) {
            return true;
        }

        ClipModel model = resolveClipModel(placement);
        return !model.rangeIsPartial() || containsPosition(model.clipBox(), x, y, z);
    }

    /** Rebuilds the cached volumes when a projection switches its verification mode. */
    public static void onPlacementDataModeChanged(@Nullable SchematicPlacement placement) {
        if (placement == null) {
            return;
        }

        INSTANCE.clipModels.remove(placement.getHashId());

        if (!RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()) {
            return;
        }

        refreshPlacementVolumes(placement);
        fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.clearActiveVerifiers();
        markChunksForRebuild(placement.getHashId());
    }

    private static boolean isClippedModel(ClipModel model) {
        return model.rendered() && model.rangeIsPartial()
                && model.verifierType() == BlockInfoListType.RENDER_LAYERS;
    }

    private static ClipModel resolveClipModel(SchematicPlacement placement) {
        INSTANCE.ensureLoaded();
        UUID id = placement.getHashId();
        RangeSelection range = INSTANCE.ranges.get(id);
        ClipModel cached = INSTANCE.clipModels.get(id);

        if (cached != null && cached.matches(placement, range)) {
            return cached;
        }

        ClipModel model = ClipModel.of(placement, range);
        INSTANCE.clipModels.put(id, model);
        return model;
    }

    /** World-space envelope of a projection's enabled sub-regions. */
    @Nullable
    private static IntBoundingBox computeRenderedEnvelope(SchematicPlacement placement) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Box box : placement.getSubRegionBoxes(RequiredEnabled.PLACEMENT_ENABLED).values()) {
            BlockPos pos1 = box.getPos1();
            BlockPos pos2 = box.getPos2();
            if (pos1 == null || pos2 == null) {
                continue;
            }

            minX = Math.min(minX, Math.min(pos1.getX(), pos2.getX()));
            minY = Math.min(minY, Math.min(pos1.getY(), pos2.getY()));
            minZ = Math.min(minZ, Math.min(pos1.getZ(), pos2.getZ()));
            maxX = Math.max(maxX, Math.max(pos1.getX(), pos2.getX()));
            maxY = Math.max(maxY, Math.max(pos1.getY(), pos2.getY()));
            maxZ = Math.max(maxZ, Math.max(pos1.getZ(), pos2.getZ()));
        }

        if (minX == Integer.MAX_VALUE) {
            Box enclosing = placement.getEclosingBox();
            if (enclosing == null || enclosing.getPos1() == null || enclosing.getPos2() == null) {
                return null;
            }

            return computeRenderedEnvelope(enclosing.getPos1(), enclosing.getPos2());
        }

        return new IntBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static IntBoundingBox computeRenderedEnvelope(BlockPos pos1, BlockPos pos2) {
        return new IntBoundingBox(
                Math.min(pos1.getX(), pos2.getX()), Math.min(pos1.getY(), pos2.getY()), Math.min(pos1.getZ(), pos2.getZ()),
                Math.max(pos1.getX(), pos2.getX()), Math.max(pos1.getY(), pos2.getY()), Math.max(pos1.getZ(), pos2.getZ()));
    }

    private static boolean containsPosition(@Nullable IntBoundingBox box, int x, int y, int z) {
        return box != null && x >= box.minX && x <= box.maxX
                && y >= box.minY && y <= box.maxY && z >= box.minZ && z <= box.maxZ;
    }

    @Nullable
    public static IntBoundingBox getWorldBox(@Nullable SchematicPlacement placement) {
        INSTANCE.ensureLoaded();
        if (placement == null) {
            return null;
        }

        RangeSelection range = INSTANCE.ranges.get(placement.getHashId());
        return range == null ? null : range.toWorldBox(placement);
    }

    /** World position of the given editable corner, or null without a saved range. */
    @Nullable
    public static BlockPos getCornerMarker(@Nullable SchematicPlacement placement, int cornerIndex) {
        INSTANCE.ensureLoaded();
        if (placement == null || (cornerIndex != 0 && cornerIndex != 1)) {
            return null;
        }

        RangeSelection range = INSTANCE.ranges.get(placement.getHashId());
        return range == null ? null : range.toWorldCorner(placement, cornerIndex);
    }

    /** True when the saved range still equals the projection's default enclosing box. */
    public static boolean isFullProjectionRange(@Nullable SchematicPlacement placement) {
        INSTANCE.ensureLoaded();
        if (placement == null) {
            return false;
        }

        RangeSelection range = INSTANCE.ranges.get(placement.getHashId());
        if (range == null) {
            return false;
        }

        // Litematica only keeps its enclosing box while "render enclosing box" is
        // enabled, so derive the full extent from the enabled sub-region boxes.
        IntBoundingBox envelope = computeRenderedEnvelope(placement);
        if (envelope == null) {
            return false;
        }

        IntBoundingBox worldBox = range.toWorldBox(placement);
        return worldBox.minX == envelope.minX && worldBox.minY == envelope.minY && worldBox.minZ == envelope.minZ
                && worldBox.maxX == envelope.maxX && worldBox.maxY == envelope.maxY && worldBox.maxZ == envelope.maxZ;
    }

    /** Called by the Litematica chunk cache mixin for every placement part. */
    public static List<IntBoundingBox> clipPlacementParts(Object partObject, IntBoundingBox box) {
        if (!RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()) {
            return List.of(box);
        }

        // Show the complete selected projection while editing so the range can be
        // adjusted against the actual schematic instead of its previous crop.
        if (INSTANCE.isEditing()) {
            return List.of(box);
        }

        if (!(partObject instanceof PlacementPart part)) {
            return List.of(box);
        }
        if (!part.getPlacement().isEnabled() || !part.getPlacement().isRenderingEnabled()) {
            return List.of(box);
        }

        SchematicPlacement placement = part.getPlacement();
        if (isFullProjectionRange(placement)) {
            return List.of(box);
        }

        IntBoundingBox range = getWorldBox(placement);
        if (range == null) {
            return List.of(box);
        }

        if (!range.intersects(box)) {
            return List.of();
        }
        if (containsBox(range, box)) {
            return List.of(box);
        }

        return List.of(intersectBoxes(range, box));
    }

    private static boolean containsBox(IntBoundingBox outer, IntBoundingBox inner) {
        return inner.minX >= outer.minX && inner.minY >= outer.minY && inner.minZ >= outer.minZ
                && inner.maxX <= outer.maxX && inner.maxY <= outer.maxY && inner.maxZ <= outer.maxZ;
    }

    private static IntBoundingBox intersectBoxes(IntBoundingBox first, IntBoundingBox second) {
        return new IntBoundingBox(
                Math.max(first.minX, second.minX), Math.max(first.minY, second.minY), Math.max(first.minZ, second.minZ),
                Math.min(first.maxX, second.maxX), Math.min(first.maxY, second.maxY), Math.min(first.maxZ, second.maxZ));
    }

    @Nullable
    private static SchematicPlacement getSelectedPlacement() {
        return DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();
    }

    private static void markChunksForRebuild(UUID placementId) {
        for (SchematicPlacement placement : DataManager.getSchematicPlacementManager().getAllSchematicsPlacements()) {
            if (placement.getHashId().equals(placementId)) {
                DataManager.getSchematicPlacementManager().markChunksForRebuild(placement);
                return;
            }
        }
    }

    private static void markAllPlacementsForRebuild() {
        SchematicPlacementManager manager = DataManager.getSchematicPlacementManager();
        for (SchematicPlacement placement : manager.getAllSchematicsPlacements()) {
            manager.markChunksForRebuild(placement);
        }
    }

    private static @NotNull RangeSelection createDefaultRange(SchematicPlacement placement) {
        Box enclosing = placement.getEclosingBox();
        if (enclosing != null && enclosing.getPos1() != null && enclosing.getPos2() != null) {
            BlockPos local1 = toLocal(placement, enclosing.getPos1());
            BlockPos local2 = toLocal(placement, enclosing.getPos2());
            return new RangeSelection(local1, local2);
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Box box : placement.getSubRegionBoxes(RequiredEnabled.ANY).values()) {
            if (box.getPos1() == null || box.getPos2() == null) {
                continue;
            }
            minX = Math.min(minX, Math.min(box.getPos1().getX(), box.getPos2().getX()));
            minY = Math.min(minY, Math.min(box.getPos1().getY(), box.getPos2().getY()));
            minZ = Math.min(minZ, Math.min(box.getPos1().getZ(), box.getPos2().getZ()));
            maxX = Math.max(maxX, Math.max(box.getPos1().getX(), box.getPos2().getX()));
            maxY = Math.max(maxY, Math.max(box.getPos1().getY(), box.getPos2().getY()));
            maxZ = Math.max(maxZ, Math.max(box.getPos1().getZ(), box.getPos2().getZ()));
        }

        if (minX == Integer.MAX_VALUE) {
            return new RangeSelection(BlockPos.ORIGIN, BlockPos.ORIGIN);
        }

        return new RangeSelection(
                toLocal(placement, new BlockPos(minX, minY, minZ)),
                toLocal(placement, new BlockPos(maxX, maxY, maxZ)));
    }

    @Nullable
    private BlockPos getTargetedBlock() {
        MinecraftClient client = MinecraftClient.getInstance();
        Entity cameraEntity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        if (client.world == null || cameraEntity == null) {
            return null;
        }

        // Using the camera entity keeps selection working under Tweakeroo free-cam,
        // because Tweakeroo replaces the vanilla camera entity while active.
        BlockPos schematicTarget = fi.dy.masa.litematica.util.RayTraceUtils.getSchematicWorldTraceIfClosest(
                client.world, cameraEntity, 64.0D);
        if (schematicTarget != null) {
            return schematicTarget;
        }

        HitResult hit = fi.dy.masa.litematica.util.RayTraceUtils.getRayTraceFromEntity(
                client.world, cameraEntity, false, 64.0D);
        return hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos() : null;
    }

    private void updateCorner(BlockPos worldPos, int corner) {
        SchematicPlacement placement = getSelectedPlacement();
        if (!this.editing || placement == null || !placement.getHashId().equals(this.editingPlacementId)) {
            return;
        }

        RangeSelection old = this.ranges.get(placement.getHashId());
        if (old == null) {
            return;
        }

        BlockPos local = toLocal(placement, worldPos);
        commitRange(placement, old.withCorner(local, corner), corner);
    }

    private void commitRange(SchematicPlacement placement, RangeSelection updated, int selectedCornerIndex) {
        this.ranges.put(placement.getHashId(), updated);
        this.selectedCorner = selectedCornerIndex;
        save();
        refreshPlacementVolumes(placement);
        fi.dy.masa.litematica.schematic.verifier.SchematicVerifier.clearActiveVerifiers();
        markChunksForRebuild(placement.getHashId());
        this.lastObservedState = null;
    }

    @Override
    public boolean onMouseClick(int mouseX, int mouseY, int eventButton, boolean eventButtonState) {
        if (!this.editing || GuiUtils.getCurrentScreen() != null) {
            return false;
        }

        SchematicPlacement placement = getSelectedPlacement();
        RangeSelection range = placement == null ? null : this.ranges.get(placement.getHashId());
        if (placement == null || range == null) {
            return true;
        }

        if (!eventButtonState) {
            return true;
        }

        if (eventButton != 0 && eventButton != 1) {
            return true;
        }

        BlockPos target = getTargetedBlock();
        if (target != null) {
            int cornerIndex = eventButton == 1 ? 1 : 0;
            CornerEditMode editMode = RenderConfigs.SchematicRenderRange.getCornerEditMode();
            if (editMode == CornerEditMode.EXPAND) {
                BlockPos local = toLocal(placement, target);
                if (eventButton == 1) {
                    commitRange(placement, new RangeSelection(local, local), cornerIndex);
                } else {
                    BlockPos pos1 = new BlockPos(
                            Math.min(range.pos1().getX(), local.getX()),
                            Math.min(range.pos1().getY(), local.getY()),
                            Math.min(range.pos1().getZ(), local.getZ()));
                    BlockPos pos2 = new BlockPos(
                            Math.max(range.pos2().getX(), local.getX()),
                            Math.max(range.pos2().getY(), local.getY()),
                            Math.max(range.pos2().getZ(), local.getZ()));
                    commitRange(placement, new RangeSelection(pos1, pos2), cornerIndex);
                }
            } else {
                updateCorner(target, cornerIndex);
            }
        }

        return true;
    }

    @Override
    public boolean onMouseScroll(int mouseX, int mouseY, double amount) {
        if (!this.editing || GuiUtils.getCurrentScreen() != null || this.selectedCorner < 0) {
            return false;
        }

        SchematicPlacement placement = getSelectedPlacement();
        RangeSelection range = placement == null ? null : this.ranges.get(placement.getHashId());
        MinecraftClient client = MinecraftClient.getInstance();
        Entity cameraEntity = fi.dy.masa.malilib.util.EntityUtils.getCameraEntity();
        if (placement == null || range == null || cameraEntity == null) {
            return false;
        }

        int steps = amount > 0 ? 1 : -1;
        BlockPos corner = range.toWorldCorner(placement, this.selectedCorner)
                .offset(EntityUtils.getClosestLookingDirection(cameraEntity), steps);
        updateCorner(corner, this.selectedCorner);
        return true;
    }

    private static BlockPos toLocal(SchematicPlacement placement, BlockPos worldPos) {
        BlockPos relative = new BlockPos(
                worldPos.getX() - placement.getOrigin().getX(),
                worldPos.getY() - placement.getOrigin().getY(),
                worldPos.getZ() - placement.getOrigin().getZ());
        return PositionUtils.getReverseTransformedBlockPos(relative, placement.getMirror(), placement.getRotation());
    }

    private void ensureLoaded() {
        if (this.loaded) {
            return;
        }

        this.loaded = true;
        Path file = FileUtils.getConfigDirectoryAsPath().resolve(FILE_NAME);
        if (!Files.exists(file) || !Files.isReadable(file)) {
            return;
        }

        JsonElement element = JsonUtils.parseJsonFileAsPath(file);
        if (element == null || !element.isJsonObject()) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            try {
                this.ranges.put(UUID.fromString(entry.getKey()), RangeSelection.fromJson(entry.getValue().getAsJsonObject()));
            } catch (Exception exception) {
                NotAGoodModForSurvival.LOGGER.warn("Ignoring invalid schematic render range '{}'.", entry.getKey());
            }
        }
    }

    private void save() {
        Path directory = FileUtils.getConfigDirectoryAsPath();
        FileUtils.createDirectoriesIfMissing(directory);
        JsonObject root = new JsonObject();
        this.ranges.forEach((id, range) -> root.add(id.toString(), range.toJson()));
        JsonUtils.writeJsonToFileAsPath(root, directory.resolve(FILE_NAME));
    }

    /** Placement-derived facts behind the render-layers clamping decisions. */
    private record ClipModel(BlockPos origin, Object rotation, Object mirror, boolean enabled,
                             boolean renderingEnabled, BlockInfoListType verifierType, RangeSelection range,
                             boolean rangeIsPartial, @Nullable IntBoundingBox clipBox,
                             @Nullable IntBoundingBox envelopeBox) {
        static ClipModel of(SchematicPlacement placement, @Nullable RangeSelection range) {
            IntBoundingBox envelope = computeRenderedEnvelope(placement);
            IntBoundingBox clip = range == null ? null : range.toWorldBox(placement);
            boolean partial = clip != null && !isFullProjectionRange(placement);

            return new ClipModel(placement.getOrigin(), placement.getRotation(), placement.getMirror(),
                    placement.isEnabled(), placement.isRenderingEnabled(), placement.getSchematicVerifierType(),
                    range, partial, partial ? clip : null, envelope);
        }

        boolean matches(SchematicPlacement placement, @Nullable RangeSelection current) {
            return this.enabled == placement.isEnabled()
                    && this.renderingEnabled == placement.isRenderingEnabled()
                    && this.verifierType == placement.getSchematicVerifierType()
                    && this.rotation == placement.getRotation()
                    && this.mirror == placement.getMirror()
                    && this.origin.equals(placement.getOrigin())
                    && Objects.equals(this.range, current)
                    && sameBox(this.envelopeBox, computeRenderedEnvelope(placement));
        }

        private static boolean sameBox(@Nullable IntBoundingBox first, @Nullable IntBoundingBox second) {
            return first == second || (first != null && second != null
                    && first.minX == second.minX && first.minY == second.minY && first.minZ == second.minZ
                    && first.maxX == second.maxX && first.maxY == second.maxY && first.maxZ == second.maxZ);
        }

        boolean rendered() {
            return this.enabled && this.renderingEnabled;
        }
    }

    private record RangeSelection(BlockPos pos1, BlockPos pos2) {
        private RangeSelection withCorner(BlockPos pos, int corner) {
            return corner == 0 ? new RangeSelection(pos, this.pos2) : new RangeSelection(this.pos1, pos);
        }

        BlockPos toWorldCorner(SchematicPlacement placement, int corner) {
            BlockPos local = corner == 0 ? this.pos1 : this.pos2;
            return PositionUtils.getTransformedBlockPos(local, placement.getMirror(), placement.getRotation())
                    .add(placement.getOrigin());
        }

        private IntBoundingBox toWorldBox(SchematicPlacement placement) {
            BlockPos[] corners = new BlockPos[] {
                    new BlockPos(pos1.getX(), pos1.getY(), pos1.getZ()),
                    new BlockPos(pos1.getX(), pos1.getY(), pos2.getZ()),
                    new BlockPos(pos1.getX(), pos2.getY(), pos1.getZ()),
                    new BlockPos(pos1.getX(), pos2.getY(), pos2.getZ()),
                    new BlockPos(pos2.getX(), pos1.getY(), pos1.getZ()),
                    new BlockPos(pos2.getX(), pos1.getY(), pos2.getZ()),
                    new BlockPos(pos2.getX(), pos2.getY(), pos1.getZ()),
                    new BlockPos(pos2.getX(), pos2.getY(), pos2.getZ())
            };

            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockPos local : corners) {
                BlockPos world = PositionUtils.getTransformedBlockPos(local, placement.getMirror(), placement.getRotation())
                        .add(placement.getOrigin());
                minX = Math.min(minX, world.getX());
                minY = Math.min(minY, world.getY());
                minZ = Math.min(minZ, world.getZ());
                maxX = Math.max(maxX, world.getX());
                maxY = Math.max(maxY, world.getY());
                maxZ = Math.max(maxZ, world.getZ());
            }
            return new IntBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
        }

        private JsonObject toJson() {
            JsonObject object = new JsonObject();
            object.add("pos1", toJson(pos1));
            object.add("pos2", toJson(pos2));
            return object;
        }

        private static RangeSelection fromJson(JsonObject object) {
            return new RangeSelection(fromJsonToBlockPos(object.getAsJsonObject("pos1")), fromJsonToBlockPos(object.getAsJsonObject("pos2")));
        }

        private static JsonObject toJson(BlockPos pos) {
            JsonObject object = new JsonObject();
            object.addProperty("x", pos.getX());
            object.addProperty("y", pos.getY());
            object.addProperty("z", pos.getZ());
            return object;
        }

        private static BlockPos fromJsonToBlockPos(JsonObject object) {
            return new BlockPos(object.get("x").getAsInt(), object.get("y").getAsInt(), object.get("z").getAsInt());
        }
    }
}
