package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.UUID;

public record MapCatalogSyncEndS2C(
        UUID worldSessionId,
        int highestMapId
) implements CustomPayload {
    public static final CustomPayload.Id<MapCatalogSyncEndS2C> ID =
            new CustomPayload.Id<>(Identifier.of("mapcatalog", "sync_end"));

    public static final PacketCodec<RegistryByteBuf, MapCatalogSyncEndS2C> CODEC = PacketCodec.tuple(
            MapCatalogPacketCodecs.UUID_CODEC,
            MapCatalogSyncEndS2C::worldSessionId,
            PacketCodecs.VAR_INT,
            MapCatalogSyncEndS2C::highestMapId,
            MapCatalogSyncEndS2C::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
