package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.server.mapcatalog;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapBannerMarker;
import net.minecraft.item.map.MapState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogBanner;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogClassification;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogMapInfo;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogPacketCodecs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncBatchS2C;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncEndS2C;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncMode;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncRequestC2S;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncStartS2C;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Integrated-server implementation of the MapCatalogSync protocol.
 *
 * <p>The dedicated-server implementation is intentionally kept separate. This
 * class only serves a logical server running inside a client, which lets the
 * client mod support singleplayer without requiring a second installed mod.</p>
 */
public final class MapCatalogIntegratedServer {
    public static final int PROTOCOL_VERSION = MapCatalogPacketCodecs.PROTOCOL_VERSION;

    private static final Pattern MAP_FILE_PATTERN = Pattern.compile("map_(\\d+)\\.dat");
    private static final UUID EMPTY_SESSION = new UUID(0L, 0L);
    private static final int REQUEST_COOLDOWN_TICKS = 20;

    private static final Map<Integer, MapCatalogMapInfo> MAPS = new HashMap<>();
    private static final Map<UUID, Integer> LAST_REQUEST_TICKS = new HashMap<>();

    private static MinecraftServer server;
    private static UUID worldSessionId = EMPTY_SESSION;
    private static int highestMapId = -1;
    private static boolean initialized;
    private static boolean receiverRegistered;

    private MapCatalogIntegratedServer() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ServerLifecycleEvents.SERVER_STARTING.register(MapCatalogIntegratedServer::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(MapCatalogIntegratedServer::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(MapCatalogIntegratedServer::onServerStopping);
    }

    private static void onServerStarting(MinecraftServer startedServer) {
        if (startedServer.isDedicated()) {
            return;
        }

        server = startedServer;
        worldSessionId = UUID.randomUUID();
        LAST_REQUEST_TICKS.clear();
        registerReceiverOnce();
    }

    private static void onServerStarted(MinecraftServer startedServer) {
        if (server != startedServer) {
            return;
        }

        scanMaps(startedServer);
        NotAGoodModForSurvival.LOGGER.info(
                "MapCatalogSync integrated server initialized: {} maps, highest map id {}, session {}",
                MAPS.size(), highestMapId, worldSessionId);
    }

    private static void onServerStopping(MinecraftServer stoppingServer) {
        if (server != stoppingServer) {
            return;
        }

        MAPS.clear();
        LAST_REQUEST_TICKS.clear();
        highestMapId = -1;
        worldSessionId = EMPTY_SESSION;
        server = null;
    }

    private static void registerReceiverOnce() {
        if (receiverRegistered) {
            return;
        }

        // Payload types are registered by the client initializer. Registering
        // them again here would duplicate the IDs in an integrated server.
        ServerPlayNetworking.registerGlobalReceiver(
                MapCatalogSyncRequestC2S.ID,
                MapCatalogIntegratedServer::handleRequest);
        receiverRegistered = true;
    }

    public static void onMapStatePut(ServerWorld world, MapIdComponent mapId, MapState mapState) {
        if (server == null || world.getServer() != server || mapId == null || mapState == null) {
            return;
        }

        MapCatalogMapInfo mapInfo = fromMapState(mapId.id(), mapState);
        MAPS.put(mapInfo.mapId(), mapInfo);
        highestMapId = Math.max(highestMapId, mapInfo.mapId());
    }

    private static void handleRequest(MapCatalogSyncRequestC2S payload, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        if (server == null || context.server() != server) {
            return;
        }
        if (!ServerPlayNetworking.canSend(player, MapCatalogSyncStartS2C.ID)) {
            return;
        }
        if (payload.protocolVersion() != PROTOCOL_VERSION) {
            sendDenied(player);
            return;
        }

        int currentTick = context.server().getTicks();
        Integer lastRequestTick = LAST_REQUEST_TICKS.get(player.getUuid());
        if (lastRequestTick != null && currentTick - lastRequestTick < REQUEST_COOLDOWN_TICKS) {
            sendDenied(player);
            return;
        }
        LAST_REQUEST_TICKS.put(player.getUuid(), currentTick);

        boolean fullSync = payload.forceFullSync()
                || !worldSessionId.equals(payload.worldSessionId());
        Identifier playerDimension = player.getServerWorld().getRegistryKey().getValue();
        List<MapCatalogMapInfo> maps = snapshotMaps(playerDimension);
        if (!fullSync) {
            maps = maps.stream()
                    .filter(map -> map.mapId() > payload.knownMaxMapId())
                    .toList();
        }

        MapCatalogSyncMode mode = fullSync
                ? MapCatalogSyncMode.FULL
                : maps.isEmpty() ? MapCatalogSyncMode.NO_CHANGE : MapCatalogSyncMode.DELTA;
        ServerPlayNetworking.send(player, new MapCatalogSyncStartS2C(
                mode,
                worldSessionId,
                highestMapId,
                maps.size()));

        for (int start = 0; start < maps.size(); start += MapCatalogPacketCodecs.MAX_BATCH_ENTRIES) {
            int end = Math.min(start + MapCatalogPacketCodecs.MAX_BATCH_ENTRIES, maps.size());
            ServerPlayNetworking.send(player, new MapCatalogSyncBatchS2C(maps.subList(start, end)));
        }
        ServerPlayNetworking.send(player, new MapCatalogSyncEndS2C(worldSessionId, highestMapId));
    }

    private static void sendDenied(ServerPlayerEntity player) {
        if (ServerPlayNetworking.canSend(player, MapCatalogSyncStartS2C.ID)) {
            ServerPlayNetworking.send(player, new MapCatalogSyncStartS2C(
                    MapCatalogSyncMode.DENIED,
                    worldSessionId,
                    highestMapId,
                    0));
        }
    }

    private static List<MapCatalogMapInfo> snapshotMaps(Identifier playerDimension) {
        return MAPS.values().stream()
                .filter(map -> map.dimension().equals(playerDimension))
                .sorted(Comparator.comparingInt(MapCatalogMapInfo::mapId))
                .toList();
    }

    private static void scanMaps(MinecraftServer currentServer) {
        MAPS.clear();
        highestMapId = -1;
        Path dataDirectory = currentServer.getSavePath(WorldSavePath.ROOT).resolve("data");
        if (!Files.isDirectory(dataDirectory)) {
            return;
        }

        ServerWorld overworld = currentServer.getOverworld();
        try (Stream<Path> paths = Files.list(dataDirectory)) {
            paths.map(path -> new MapFile(path, parseMapId(path)))
                    .filter(mapFile -> mapFile.mapId() >= 0 && Files.isRegularFile(mapFile.path()))
                    .sorted(Comparator.comparingInt(MapFile::mapId))
                    .forEach(mapFile -> {
                        try {
                            MapState mapState = getMapState(overworld, mapFile.mapId());
                            if (mapState != null) {
                                MapCatalogMapInfo mapInfo = fromMapState(mapFile.mapId(), mapState);
                                MAPS.put(mapInfo.mapId(), mapInfo);
                                highestMapId = Math.max(highestMapId, mapInfo.mapId());
                            }
                        } catch (RuntimeException exception) {
                            NotAGoodModForSurvival.LOGGER.warn(
                                    "Could not load map catalog file {}.", mapFile.path(), exception);
                        }
                    });
        } catch (IOException exception) {
            NotAGoodModForSurvival.LOGGER.warn(
                    "Could not scan the integrated server map data directory {}.", dataDirectory, exception);
        }
    }

    private static MapState getMapState(ServerWorld world, int mapId) {
        return world.getPersistentStateManager().get(
                MapState.getPersistentStateType(), "map_" + mapId);
    }

    private static int parseMapId(Path path) {
        Matcher matcher = MAP_FILE_PATTERN.matcher(path.getFileName().toString());
        if (!matcher.matches()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static MapCatalogMapInfo fromMapState(int mapId, MapState mapState) {
        List<MapCatalogBanner> banners = new ArrayList<>();
        for (MapBannerMarker banner : mapState.getBanners()) {
            banners.add(new MapCatalogBanner(
                    banner.pos().getX(),
                    banner.pos().getZ(),
                    banner.color(),
                    banner.name()));
        }
        banners.sort(Comparator.comparingInt(MapCatalogBanner::worldX)
                .thenComparingInt(MapCatalogBanner::worldZ)
                .thenComparing(banner -> banner.name().map(Object::toString).orElse("")));

        return new MapCatalogMapInfo(
                mapId,
                mapState.dimension.getValue(),
                mapState.centerX,
                mapState.centerZ,
                mapState.scale,
                mapState.locked,
                new MapCatalogClassification(mapState.hasExplorationMapDecoration(), banners));
    }

    private record MapFile(Path path, int mapId) {
    }
}
