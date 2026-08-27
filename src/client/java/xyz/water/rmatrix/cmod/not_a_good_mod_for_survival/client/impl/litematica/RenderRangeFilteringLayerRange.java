package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica;

import com.google.gson.JsonObject;
import fi.dy.masa.malilib.interfaces.IRangeChangeListener;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.LayerMode;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.SubChunkPos;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * Thin LayerRange view that additionally requires saved projection ranges to
 * accept a position. Only the position queries apply the extra test; every other
 * accessor and mutator forwards to the delegate, so Litematica keeps its own
 * semantics and its layer hotkeys still mutate the original instance.
 */
public final class RenderRangeFilteringLayerRange extends LayerRange {
    private final LayerRange delegate;

    RenderRangeFilteringLayerRange(LayerRange delegate) {
        super(null);
        this.delegate = delegate;
    }

    /**
     * Copies the delegate's boundaries into the fields inherited from LayerRange.
     * Some inherited accessors and {@code LayerRange.CODEC} read those fields
     * directly instead of going through the getters, so an unsynchronised wrapper
     * would report the ALL/Y defaults of this instance.
     */
    void syncStateFromDelegate() {
        this.layerMode = this.delegate.getLayerMode();
        this.axis = this.delegate.getAxis();
        this.layerSingle = this.delegate.getLayerSingle();
        this.layerAbove = this.delegate.getLayerAbove();
        this.layerBelow = this.delegate.getLayerBelow();
        this.layerRangeMin = this.delegate.getLayerRangeMin();
        this.layerRangeMax = this.delegate.getLayerRangeMax();
        this.hotkeyRangeMin = this.delegate.getMoveLayerRangeMin();
        this.hotkeyRangeMax = this.delegate.getMoveLayerRangeMax();
    }

    // ---- Queries that additionally honour the saved projection ranges. ----

    @Override
    public boolean isPositionWithinRange(BlockPos pos) {
        return this.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean isPositionWithinRange(long posLong) {
        BlockPos pos = BlockPos.fromLong(posLong);
        return this.isPositionWithinRange(pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public boolean isPositionWithinRange(int x, int y, int z) {
        return this.delegate.isPositionWithinRange(x, y, z)
                && SchematicRenderRangeManager.isPositionWithinRenderedContent(x, y, z);
    }

    // ---- Read-only forwarding: box level clipping is applied per placement in
    // SchematicRenderRangeManager, so these must keep Litematica's own verdict. ----

    @Override
    public IntBoundingBox getClampedArea(BlockPos posMin, BlockPos posMax) {
        return this.delegate.getClampedArea(posMin, posMax);
    }

    @Override
    public IntBoundingBox getClampedArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.delegate.getClampedArea(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean intersects(IntBoundingBox box) {
        return this.delegate.intersects(box);
    }

    @Override
    public boolean intersects(SubChunkPos pos) {
        return this.delegate.intersects(pos);
    }

    @Override
    public boolean intersectsBox(BlockPos posMin, BlockPos posMax) {
        return this.delegate.intersectsBox(posMin, posMax);
    }

    @Override
    public boolean intersectsBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return this.delegate.intersectsBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public boolean isPositionAtRenderEdgeOnSide(BlockPos pos, Direction side) {
        return this.delegate.isPositionAtRenderEdgeOnSide(pos, side);
    }

    @Override
    public int getClampedValue(int value, Direction.Axis axis) {
        return this.delegate.getClampedValue(value, axis);
    }

    @Override
    public IntBoundingBox getClampedRenderBoundingBox(IntBoundingBox box) {
        return this.delegate.getClampedRenderBoundingBox(box);
    }

    @Override
    public IntBoundingBox getExpandedBox(World world, int expandAmount) {
        return this.delegate.getExpandedBox(world, expandAmount);
    }

    @Override
    public JsonObject toJson() {
        return this.delegate.toJson();
    }

    @Override
    public LayerMode getLayerMode() {
        return this.delegate.getLayerMode();
    }

    @Override
    public Direction.Axis getAxis() {
        return this.delegate.getAxis();
    }

    @Override
    public boolean getMoveLayerRangeMin() {
        return this.delegate.getMoveLayerRangeMin();
    }

    @Override
    public boolean getMoveLayerRangeMax() {
        return this.delegate.getMoveLayerRangeMax();
    }

    @Override
    public int getLayerSingle() {
        return this.delegate.getLayerSingle();
    }

    @Override
    public int getLayerAbove() {
        return this.delegate.getLayerAbove();
    }

    @Override
    public int getLayerBelow() {
        return this.delegate.getLayerBelow();
    }

    @Override
    public int getLayerRangeMin() {
        return this.delegate.getLayerRangeMin();
    }

    @Override
    public int getLayerRangeMax() {
        return this.delegate.getLayerRangeMax();
    }

    @Override
    public int getLayerMin() {
        return this.delegate.getLayerMin();
    }

    @Override
    public int getLayerMax() {
        return this.delegate.getLayerMax();
    }

    @Override
    public int getCurrentLayerValue(boolean isSecondValue) {
        return this.delegate.getCurrentLayerValue(isSecondValue);
    }

    @Override
    public String getCurrentLayerString() {
        return this.delegate.getCurrentLayerString();
    }

    // ---- Mutator forwarding: the parent implementations would otherwise change
    // this wrapper's snapshot and its null refresher instead of Litematica's. ----

    @Override
    public LayerRange setRefresher(IRangeChangeListener refresher) {
        this.delegate.setRefresher(refresher);
        return this;
    }

    @Override
    public void setLayerMode(LayerMode mode) {
        this.delegate.setLayerMode(mode);
    }

    @Override
    public void setLayerMode(LayerMode mode, boolean printMessage) {
        this.delegate.setLayerMode(mode, printMessage);
    }

    @Override
    public void setAxis(Direction.Axis axis) {
        this.delegate.setAxis(axis);
    }

    @Override
    public void setLayerSingle(int layer) {
        this.delegate.setLayerSingle(layer);
    }

    @Override
    public void setLayerAbove(int layer) {
        this.delegate.setLayerAbove(layer);
    }

    @Override
    public void setLayerBelow(int layer) {
        this.delegate.setLayerBelow(layer);
    }

    @Override
    public boolean setLayerRangeMin(int layer) {
        return this.delegate.setLayerRangeMin(layer);
    }

    @Override
    public boolean setLayerRangeMax(int layer) {
        return this.delegate.setLayerRangeMax(layer);
    }


    @Override
    public void toggleHotkeyMoveRangeMin() {
        this.delegate.toggleHotkeyMoveRangeMin();
    }

    @Override
    public void toggleHotkeyMoveRangeMax() {
        this.delegate.toggleHotkeyMoveRangeMax();
    }

    @Override
    public boolean moveLayer(int amount) {
        return this.delegate.moveLayer(amount);
    }

    @Override
    public void setSingleBoundaryToPosition(Entity entity) {
        this.delegate.setSingleBoundaryToPosition(entity);
    }

    @Override
    public void setToPosition(Entity entity) {
        this.delegate.setToPosition(entity);
    }

    @Override
    public void fromJson(JsonObject obj) {
        this.delegate.fromJson(obj);
    }
}
