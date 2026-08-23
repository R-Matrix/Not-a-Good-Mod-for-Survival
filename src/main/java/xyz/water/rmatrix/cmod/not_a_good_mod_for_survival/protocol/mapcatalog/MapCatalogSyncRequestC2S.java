package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record MapCatalogSyncRequestC2S(
        int protocolVersion,
        UUID worldSessionId,
        int knownMaxMapId,
        boolean forceFullSync
) implements CustomPayload {
    public static final CustomPayload.Id<MapCatalogSyncRequestC2S> ID =
            new CustomPayload.Id<>(Identifier.of("mapcatalog", "sync_request"));

    public static final PacketCodec<RegistryByteBuf, MapCatalogSyncRequestC2S> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            MapCatalogSyncRequestC2S::protocolVersion,
            MapCatalogPacketCodecs.UUID_CODEC,
            MapCatalogSyncRequestC2S::worldSessionId,
            PacketCodecs.VAR_INT,
            MapCatalogSyncRequestC2S::knownMaxMapId,
            PacketCodecs.BOOLEAN,
            MapCatalogSyncRequestC2S::forceFullSync,
            MapCatalogSyncRequestC2S::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
