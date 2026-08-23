package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.mapcatalog;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.NotAGoodModForSurvival;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogMapInfo;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogPacketCodecs;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncBatchS2C;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncEndS2C;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncMode;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncRequestC2S;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogSyncStartS2C;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Client-side MapCatalogSync registration, transaction handling, and cache. */
public final class MapCatalogSyncClient {
    private static final UUID EMPTY_SESSION = new UUID(0L, 0L);
    private static final long REQUEST_INTERVAL_MS = 1_500L;
    private static final long DENIED_RETRY_INTERVAL_MS = 5_000L;
    private static final int MAX_TRANSACTION_ENTRIES = 2_000_000;

    private static Map<Integer, MapCatalogMapInfo> maps = Map.of();
    private static UUID worldSessionId = EMPTY_SESSION;
    private static int highestMapId = -1;
    private static Identifier syncedDimension;
    private static Identifier lastRequestedDimension;
    private static boolean completed;
    private static long lastRequestMillis;
    private static long deniedUntilMillis;
    private static Transaction transaction;
    private static boolean initialized;

    private MapCatalogSyncClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        PayloadTypeRegistry.playC2S().register(MapCatalogSyncRequestC2S.ID, MapCatalogSyncRequestC2S.CODEC);
        PayloadTypeRegistry.playS2C().register(MapCatalogSyncStartS2C.ID, MapCatalogSyncStartS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(MapCatalogSyncBatchS2C.ID, MapCatalogSyncBatchS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(MapCatalogSyncEndS2C.ID, MapCatalogSyncEndS2C.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(MapCatalogSyncStartS2C.ID,
                (payload, context) -> context.client().execute(() -> onStart(payload)));
        ClientPlayNetworking.registerGlobalReceiver(MapCatalogSyncBatchS2C.ID,
                (payload, context) -> context.client().execute(() -> onBatch(payload)));
        ClientPlayNetworking.registerGlobalReceiver(MapCatalogSyncEndS2C.ID,
                (payload, context) -> context.client().execute(() -> onEnd(payload)));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                client.execute(() -> request(true, null)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
    }

    public static void requestInitial() {
        request(true, null);
    }

    public static void requestIfNeeded(Identifier viewedDimension) {
        if (viewedDimension == null) {
            return;
        }
        boolean dimensionChanged = syncedDimension == null || !syncedDimension.equals(viewedDimension);
        request(!completed || dimensionChanged, viewedDimension);
    }

    public static List<MapCatalogMapInfo> snapshot() {
        return List.copyOf(maps.values());
    }

    public static boolean isCompleted() {
        return completed;
    }

    public static UUID worldSessionId() {
        return worldSessionId;
    }

    public static int highestMapId() {
        return highestMapId;
    }

    private static void request(boolean forceFull, Identifier requestedDimension) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null || client.player == null
                || !ClientPlayNetworking.canSend(MapCatalogSyncRequestC2S.ID)) {
            return;
        }

        long now = System.currentTimeMillis();
        if (transaction != null || now < deniedUntilMillis || now - lastRequestMillis < REQUEST_INTERVAL_MS) {
            return;
        }

        boolean effectiveFull = forceFull || !completed;
        UUID knownSession = completed ? worldSessionId : EMPTY_SESSION;
        int knownMax = completed ? highestMapId : -1;
        lastRequestedDimension = requestedDimension != null
                ? requestedDimension
                : client.world == null ? null : client.world.getRegistryKey().getValue();
        ClientPlayNetworking.send(new MapCatalogSyncRequestC2S(
                MapCatalogPacketCodecs.PROTOCOL_VERSION,
                knownSession,
                knownMax,
                effectiveFull
        ));
        lastRequestMillis = now;
    }

    private static void onStart(MapCatalogSyncStartS2C payload) {
        if (payload.syncMode() == MapCatalogSyncMode.DENIED) {
            transaction = null;
            deniedUntilMillis = System.currentTimeMillis() + DENIED_RETRY_INTERVAL_MS;
            NotAGoodModForSurvival.LOGGER.debug(
                    "MapCatalogSync server denied the map synchronization request.");
            return;
        }

        if (payload.entryCount() < 0 || payload.entryCount() > MAX_TRANSACTION_ENTRIES
                || payload.highestMapId() < -1) {
            abortTransaction("invalid synchronization header");
            return;
        }

        if (payload.syncMode() == MapCatalogSyncMode.DELTA
                && (!completed || !worldSessionId.equals(payload.worldSessionId()))) {
            abortTransaction("delta synchronization used an unknown world session");
            request(true, syncedDimension);
            return;
        }

        transaction = new Transaction(
                payload.syncMode(),
                payload.worldSessionId(),
                payload.highestMapId(),
                payload.entryCount(),
                new HashMap<>(),
                lastRequestedDimension
        );
    }

    private static void onBatch(MapCatalogSyncBatchS2C payload) {
        Transaction current = transaction;
        if (current == null || current.mode() == MapCatalogSyncMode.NO_CHANGE) {
            return;
        }

        if (payload.maps() == null || current.receivedCount() + payload.maps().size() > current.expectedCount()) {
            abortTransaction("synchronization batch exceeds the declared entry count");
            return;
        }

        for (MapCatalogMapInfo map : payload.maps()) {
            if (!isValidMapInfo(map) || current.entries().put(map.mapId(), map) != null) {
                abortTransaction("synchronization batch contains invalid or duplicate map data");
                return;
            }
        }
        current.setReceivedCount(current.receivedCount() + payload.maps().size());
    }

    private static void onEnd(MapCatalogSyncEndS2C payload) {
        Transaction current = transaction;
        if (current == null) {
            return;
        }

        transaction = null;
        boolean valid = current.worldSessionId().equals(payload.worldSessionId())
                && current.highestMapId() == payload.highestMapId()
                && current.receivedCount() == current.expectedCount();
        if (!valid) {
            abortTransaction("synchronization end packet failed validation");
            return;
        }

        if (current.mode() == MapCatalogSyncMode.FULL) {
            maps = Map.copyOf(current.entries());
        } else if (current.mode() == MapCatalogSyncMode.DELTA) {
            Map<Integer, MapCatalogMapInfo> merged = new HashMap<>(maps);
            merged.putAll(current.entries());
            maps = Map.copyOf(merged);
        }

        worldSessionId = current.worldSessionId();
        highestMapId = current.highestMapId();
        syncedDimension = current.requestedDimension();
        lastRequestedDimension = syncedDimension;
        completed = true;
    }

    private static boolean isValidMapInfo(MapCatalogMapInfo map) {
        return map != null
                && map.mapId() >= 0
                && map.dimension() != null
                && map.scale() >= 0
                && map.scale() <= 4
                && map.classification() != null;
    }

    private static void abortTransaction(String reason) {
        transaction = null;
        NotAGoodModForSurvival.LOGGER.debug("Discarded MapCatalogSync transaction: {}", reason);
    }

    private static void reset() {
        maps = Map.of();
        worldSessionId = EMPTY_SESSION;
        highestMapId = -1;
        syncedDimension = null;
        lastRequestedDimension = null;
        completed = false;
        lastRequestMillis = 0L;
        deniedUntilMillis = 0L;
        transaction = null;
    }

    private static final class Transaction {
        private final MapCatalogSyncMode mode;
        private final UUID worldSessionId;
        private final int highestMapId;
        private final int expectedCount;
        private final Map<Integer, MapCatalogMapInfo> entries;
        private final Identifier requestedDimension;
        private int receivedCount;

        private Transaction(
                MapCatalogSyncMode mode,
                UUID worldSessionId,
                int highestMapId,
                int expectedCount,
                Map<Integer, MapCatalogMapInfo> entries,
                Identifier requestedDimension
        ) {
            this.mode = mode;
            this.worldSessionId = worldSessionId;
            this.highestMapId = highestMapId;
            this.expectedCount = expectedCount;
            this.entries = entries;
            this.requestedDimension = requestedDimension;
        }

        private MapCatalogSyncMode mode() {
            return mode;
        }

        private UUID worldSessionId() {
            return worldSessionId;
        }

        private int highestMapId() {
            return highestMapId;
        }

        private int expectedCount() {
            return expectedCount;
        }

        private Map<Integer, MapCatalogMapInfo> entries() {
            return entries;
        }

        private Identifier requestedDimension() {
            return requestedDimension;
        }

        private int receivedCount() {
            return receivedCount;
        }

        private void setReceivedCount(int receivedCount) {
            this.receivedCount = receivedCount;
        }
    }
}
