package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica;

import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Exposes Litematica's per-chunk volume cache rebuild for range refreshes. */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.schematic.placement.SchematicPlacementManager", remap = false)
public interface SchematicPlacementManagerCacheAccess {
    @Invoker(value = "updateTouchedBoxesInChunk", remap = false)
    void notAGoodModForSurvival$invokeUpdateTouchedBoxesInChunk(ChunkPos pos);
}
