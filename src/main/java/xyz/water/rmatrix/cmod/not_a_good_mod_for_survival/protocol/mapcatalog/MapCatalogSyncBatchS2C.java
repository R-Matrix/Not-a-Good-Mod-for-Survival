package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.List;

public record MapCatalogSyncBatchS2C(List<MapCatalogMapInfo> maps) implements CustomPayload {
    public static final CustomPayload.Id<MapCatalogSyncBatchS2C> ID =
            new CustomPayload.Id<>(Identifier.of("mapcatalog", "sync_batch"));

    public static final PacketCodec<RegistryByteBuf, MapCatalogSyncBatchS2C> CODEC = PacketCodec.tuple(
            MapCatalogPacketCodecs.MAP_LIST_CODEC,
            MapCatalogSyncBatchS2C::maps,
            MapCatalogSyncBatchS2C::new
    );

    public MapCatalogSyncBatchS2C {
        maps = maps == null ? List.of() : List.copyOf(maps);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
