package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.mixin.mapcatalog;

import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.server.mapcatalog.MapCatalogIntegratedServer;

/** Keeps the integrated-server map catalog current when a map state is saved or changed. */
@Mixin(ServerWorld.class)
public abstract class ServerWorldMapCatalogMixin {
    @Inject(method = "putMapState", at = @At("TAIL"))
    private void mapCatalog$afterPutMapState(MapIdComponent mapId, MapState mapState, CallbackInfo ci) {
        MapCatalogIntegratedServer.onMapStatePut((ServerWorld) (Object) this, mapId, mapState);
    }
}
