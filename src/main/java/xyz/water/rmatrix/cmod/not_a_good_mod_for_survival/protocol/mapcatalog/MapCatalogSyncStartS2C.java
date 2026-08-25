package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record MapCatalogSyncStartS2C(
        MapCatalogSyncMode syncMode,
        UUID worldSessionId,
        long catalogRevision,
        int highestMapId,
        int entryCount
) implements CustomPayload {
    public static final CustomPayload.Id<MapCatalogSyncStartS2C> ID =
            new CustomPayload.Id<>(Identifier.of("mapcatalog", "sync_start"));

    public static final PacketCodec<RegistryByteBuf, MapCatalogSyncStartS2C> CODEC = PacketCodec.tuple(
            MapCatalogPacketCodecs.SYNC_MODE_CODEC,
            MapCatalogSyncStartS2C::syncMode,
            MapCatalogPacketCodecs.UUID_CODEC,
            MapCatalogSyncStartS2C::worldSessionId,
            MapCatalogPacketCodecs.LONG_CODEC,
            MapCatalogSyncStartS2C::catalogRevision,
            PacketCodecs.VAR_INT,
            MapCatalogSyncStartS2C::highestMapId,
            PacketCodecs.VAR_INT,
            MapCatalogSyncStartS2C::entryCount,
            MapCatalogSyncStartS2C::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
