package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.mixin.litematica;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import fi.dy.masa.litematica.materials.IMaterialList;
import fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksBase;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.litematica.SchematicRenderRangeManager;

/**
 * Makes the material list count only the rendered part of a projection. Litematica
 * itself only honours layer limits while the list runs in render-layers mode, so the
 * saved display range is applied under exactly the same condition.
 */
@Pseudo
@Mixin(targets = "fi.dy.masa.litematica.scheduler.tasks.TaskCountBlocksPlacement", remap = false)
public abstract class TaskCountBlocksPlacementMixin extends TaskCountBlocksBase {

    @Shadow(remap = false) @Final
    protected SchematicPlacement schematicPlacement;

    protected TaskCountBlocksPlacementMixin(IMaterialList materialList, String nameOnHud) {
        super(materialList, nameOnHud);
    }

    @WrapMethod(method = "countAtPosition", remap = false)
    private void notAGoodModForSurvival$skipOutsideRenderRange(BlockPos pos, Operation<Void> original) {
        if (SchematicRenderRangeManager.isPositionCountedWithinRenderRange(
                this.schematicPlacement, this.materialList, pos.getX(), pos.getY(), pos.getZ())) {
            original.call(pos);
        }
    }
}
