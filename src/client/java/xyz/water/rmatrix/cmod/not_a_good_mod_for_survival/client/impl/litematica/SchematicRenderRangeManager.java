package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager.PlacementPart;
import fi.dy.masa.litematica.schematic.placement.SubRegionPlacement.RequiredEnabled;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.util.EntityUtils;
import fi.dy.masa.litematica.util.PositionUtils;
import fi.dy.masa.malilib.hotkeys.IMouseInputHandler;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.gui.Message;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.config.render.RenderConfigs;

/** Owns the per-projection display range and the small Litematica-style range editor. */
public final class SchematicRenderRangeManager implements IMouseInputHandler {
    private static final SchematicRenderRangeManager INSTANCE = new SchematicRenderRangeManager();
    private static final String FILE_NAME = NotAGoodModForSurvival.MOD_ID + ".schematic-render-ranges.json";

    private final Map<UUID, RangeSelection> ranges = new HashMap<>();
    private boolean loaded;
    private boolean editing;
    private UUID editingPlacementId;
    private int selectedCorner = -1;
    private boolean dragging;
    private UUID lastObservedPlacementId;
    private String lastObservedState;

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
            this.dragging = false;
            this.selectedCorner = -1;
        }
    }

    public boolean toggleEditor() {
        ensureLoaded();
        SchematicPlacement placement = getSelectedPlacement();

        if (placement == null) {
            InfoUtils.showInGameMessage(Message.MessageType.WARNING, "No Litematica projection is selected.");
            return true;
        }

        if (this.editing) {
            this.editing = false;
            this.dragging = false;
            this.selectedCorner = -1;
            markChunksForRebuild(placement.getHashId());
            this.lastObservedState = null;
            InfoUtils.showInGameMessage(Message.MessageType.INFO, "Projection display-range editor disabled.");
            return true;
        }

        RangeSelection range = this.ranges.get(placement.getHashId());
        if (range == null) {
            range = createDefaultRange(placement);
            if (range == null) {
                InfoUtils.showInGameMessage(Message.MessageType.ERROR, "The selected projection has no editable range.");
                return true;
            }
            this.ranges.put(placement.getHashId(), range);
            save();
        }

        this.editing = true;
        this.editingPlacementId = placement.getHashId();
        this.selectedCorner = -1;
        this.dragging = false;
        markChunksForRebuild(placement.getHashId());
        this.lastObservedState = null;
        InfoUtils.showInGameMessage(Message.MessageType.INFO, "Projection display-range editor enabled.");
        return true;
    }

    public boolean isEditing() {
        return this.editing;
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

    /** Called by the Litematica chunk cache mixin for every placement part. */
    @Nullable
    public static IntBoundingBox clipPlacementPart(Object partObject, IntBoundingBox box) {
        if (!(partObject instanceof PlacementPart part)) {
            return box;
        }

        if (!RenderConfigs.SchematicRenderRange.ENABLE.getBooleanValue()) {
            return box;
        }

        // Show the complete selected projection while editing so the range can be
        // adjusted against the actual schematic instead of its previous crop.
        if (INSTANCE.isEditing()) {
            return box;
        }

        SchematicPlacement selected = getSelectedPlacement();
        if (selected == null || !selected.getHashId().equals(part.getPlacement().getHashId())
                || !part.getPlacement().isEnabled() || !part.getPlacement().isRenderingEnabled()) {
            return box;
        }

        IntBoundingBox range = getWorldBox(selected);
        if (range == null || !range.intersects(box)) {
            return range == null ? box : null;
        }

        return new IntBoundingBox(
                Math.max(box.minX, range.minX), Math.max(box.minY, range.minY), Math.max(box.minZ, range.minZ),
                Math.min(box.maxX, range.maxX), Math.min(box.maxY, range.maxY), Math.min(box.maxZ, range.maxZ));
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

    @Nullable
    private static RangeSelection createDefaultRange(SchematicPlacement placement) {
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
        if (client.world == null || client.player == null) {
            return null;
        }

        BlockPos schematicTarget = fi.dy.masa.litematica.util.RayTraceUtils.getSchematicWorldTraceIfClosest(
                client.world, client.player, 64.0D);
        if (schematicTarget != null) {
            return schematicTarget;
        }

        HitResult hit = fi.dy.masa.litematica.util.RayTraceUtils.getRayTraceFromEntity(
                client.world, client.player, false, 64.0D);
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
        this.ranges.put(placement.getHashId(), old.withCorner(local, corner));
        this.selectedCorner = corner;
        save();
        markChunksForRebuild(placement.getHashId());
        this.lastObservedState = null;
    }

    private int findCorner(BlockPos worldPos, SchematicPlacement placement, RangeSelection range) {
        BlockPos first = range.toWorldCorner(placement, 0);
        BlockPos second = range.toWorldCorner(placement, 1);
        if (first.getSquaredDistance(worldPos) <= 9.0D) {
            return 0;
        }
        if (second.getSquaredDistance(worldPos) <= 9.0D) {
            return 1;
        }
        return -1;
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
            this.dragging = false;
            return true;
        }

        if (eventButton != 0 && eventButton != 1) {
            return true;
        }

        BlockPos target = getTargetedBlock();
        if (target != null) {
            int corner = findCorner(target, placement, range);
            if (corner < 0) {
                corner = eventButton == 1 ? 1 : 0;
            }
            updateCorner(target, corner);
        }
        if (eventButton == 0 || eventButton == 1) {
            this.dragging = true;
        }

        return true;
    }

    @Override
    public void onMouseMove(int mouseX, int mouseY) {
        if (!this.editing || !this.dragging || GuiUtils.getCurrentScreen() != null) {
            return;
        }

        BlockPos target = getTargetedBlock();
        if (target != null && this.selectedCorner >= 0) {
            updateCorner(target, this.selectedCorner);
        }
    }

    @Override
    public boolean onMouseScroll(int mouseX, int mouseY, double amount) {
        if (!this.editing || GuiUtils.getCurrentScreen() != null || this.selectedCorner < 0) {
            return false;
        }

        SchematicPlacement placement = getSelectedPlacement();
        RangeSelection range = placement == null ? null : this.ranges.get(placement.getHashId());
        MinecraftClient client = MinecraftClient.getInstance();
        if (placement == null || range == null || client.player == null) {
            return false;
        }

        int steps = amount > 0 ? 1 : -1;
        BlockPos corner = range.toWorldCorner(placement, this.selectedCorner)
                .offset(EntityUtils.getClosestLookingDirection(client.player), steps);
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

    private record RangeSelection(BlockPos pos1, BlockPos pos2) {
        private RangeSelection withCorner(BlockPos pos, int corner) {
            return corner == 0 ? new RangeSelection(pos, this.pos2) : new RangeSelection(this.pos1, pos);
        }

        private BlockPos toWorldCorner(SchematicPlacement placement, int corner) {
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
