package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Codec definitions for the MapCatalogSync protocol. */
public final class MapCatalogPacketCodecs {
    public static final int PROTOCOL_VERSION = 2;
    public static final int MAX_BATCH_ENTRIES = 256;
    private static final int MAX_BANNERS_PER_MAP = 256;

    public static final PacketCodec<ByteBuf, Long> LONG_CODEC = PacketCodec.ofStatic(
            ByteBuf::writeLong,
            ByteBuf::readLong
    );

    public static final PacketCodec<ByteBuf, UUID> UUID_CODEC = PacketCodec.tuple(
            LONG_CODEC,
            UUID::getMostSignificantBits,
            LONG_CODEC,
            UUID::getLeastSignificantBits,
            UUID::new
    );

    public static final PacketCodec<ByteBuf, MapCatalogSyncMode> SYNC_MODE_CODEC = PacketCodecs.indexed(
            index -> MapCatalogSyncMode.values()[index],
            MapCatalogSyncMode::ordinal
    );

    private static final PacketCodec<RegistryByteBuf, MapCatalogBanner> BANNER_MARKER_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            MapCatalogBanner::worldX,
            PacketCodecs.INTEGER,
            MapCatalogBanner::worldZ,
            DyeColor.PACKET_CODEC,
            MapCatalogBanner::color,
            TextCodecs.OPTIONAL_PACKET_CODEC,
            MapCatalogBanner::name,
            MapCatalogBanner::new
    );

    private static final PacketCodec<RegistryByteBuf, MapCatalogClassification> CLASSIFICATION_CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN,
            MapCatalogClassification::hasExplorationMarker,
            PacketCodecs.collection(ArrayList::new, BANNER_MARKER_CODEC, MAX_BANNERS_PER_MAP),
            MapCatalogClassification::banners,
            MapCatalogClassification::new
    );

    public static final PacketCodec<RegistryByteBuf, MapCatalogMapInfo> MAP_INFO_CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT,
            MapCatalogMapInfo::mapId,
            Identifier.PACKET_CODEC,
            MapCatalogMapInfo::dimension,
            PacketCodecs.INTEGER,
            MapCatalogMapInfo::centerX,
            PacketCodecs.INTEGER,
            MapCatalogMapInfo::centerZ,
            PacketCodecs.BYTE,
            MapCatalogMapInfo::scale,
            PacketCodecs.BOOLEAN,
            MapCatalogMapInfo::locked,
            CLASSIFICATION_CODEC,
            MapCatalogMapInfo::classification,
            MapCatalogMapInfo::new
    );

    public static final PacketCodec<RegistryByteBuf, List<MapCatalogMapInfo>> MAP_LIST_CODEC =
            PacketCodecs.collection(ArrayList::new, MAP_INFO_CODEC, MAX_BATCH_ENTRIES);

    private MapCatalogPacketCodecs() {
    }
}
