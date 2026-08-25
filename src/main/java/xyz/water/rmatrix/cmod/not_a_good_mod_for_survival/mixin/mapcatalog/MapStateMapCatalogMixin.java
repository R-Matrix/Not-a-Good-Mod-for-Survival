package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.mixin.mapcatalog;

import net.minecraft.item.map.MapState;
import net.minecraft.item.map.MapBannerMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.server.mapcatalog.MapCatalogIntegratedServer;

import java.util.Objects;

/** Notifies the map catalog when a map's banner marker collection changes. */
@Mixin(MapState.class)
public abstract class MapStateMapCatalogMixin {
    @Unique
    private MapBannerMarker mapCatalog$bannerBeforeRemoval;

    @Inject(method = "addBanner", at = @At("RETURN"))
    private void mapCatalog$afterAddBanner(
            net.minecraft.world.WorldAccess world,
            net.minecraft.util.math.BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValue()) {
            MapCatalogIntegratedServer.onMapStateBannerChanged((MapState) (Object) this);
        }
    }

    @Inject(method = "removeBanner", at = @At("TAIL"))
    private void mapCatalog$afterRemoveBanner(
            net.minecraft.world.BlockView world,
            int x,
            int z,
            CallbackInfo ci
    ) {
        MapState mapState = (MapState) (Object) this;
        MapBannerMarker after = mapState.getBanners().stream()
                .filter(banner -> banner.pos().getX() == x && banner.pos().getZ() == z)
                .findFirst()
                .orElse(null);
        if (!Objects.equals(mapCatalog$bannerBeforeRemoval, after)) {
            MapCatalogIntegratedServer.onMapStateBannerChanged(mapState);
        }
        mapCatalog$bannerBeforeRemoval = null;
    }

    @Inject(method = "removeBanner", at = @At("HEAD"))
    private void mapCatalog$beforeRemoveBanner(
            net.minecraft.world.BlockView world,
            int x,
            int z,
            CallbackInfo ci
    ) {
        MapState mapState = (MapState) (Object) this;
        mapCatalog$bannerBeforeRemoval = mapState.getBanners().stream()
                .filter(banner -> banner.pos().getX() == x && banner.pos().getZ() == z)
                .findFirst()
                .orElse(null);
    }
}
