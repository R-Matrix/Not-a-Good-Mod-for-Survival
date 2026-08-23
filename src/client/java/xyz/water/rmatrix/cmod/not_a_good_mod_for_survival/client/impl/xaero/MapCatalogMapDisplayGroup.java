package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.minecraft.client.resource.language.I18n;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogMapInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A single visual coverage area containing one or more server map IDs. */
public record MapCatalogMapDisplayGroup(
        MapCatalogMapInfo representative,
        List<Integer> mapIds
) {
    private static final String MORE_MAPS_TRANSLATION_KEY =
            "not-a-good-mod-for-survival.gui.xaero.map_catalog.more_maps";

    public MapCatalogMapDisplayGroup {
        mapIds = mapIds == null ? List.of() : mapIds.stream().sorted().distinct().toList();
    }

    public int coverageBlocks() {
        int scale = Math.max(0, Math.min(4, representative.scale()));
        return 128 << scale;
    }

    public String label() {
        if (mapIds.isEmpty()) {
            return "";
        }
        if (mapIds.size() == 1) {
            return "#" + mapIds.getFirst();
        }
        int first = mapIds.getFirst();
        int last = mapIds.getLast();
        if (last - first + 1 == mapIds.size()) {
            return "#" + first + "-#" + last;
        }
        return I18n.translate(MORE_MAPS_TRANSLATION_KEY, first, mapIds.size());
    }

    public String fullIdRanges() {
        if (mapIds.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int rangeStart = mapIds.getFirst();
        int previous = rangeStart;
        for (int index = 1; index < mapIds.size(); index++) {
            int current = mapIds.get(index);
            if (current == previous + 1) {
                previous = current;
                continue;
            }
            appendRange(result, rangeStart, previous);
            result.append(", ");
            rangeStart = previous = current;
        }
        appendRange(result, rangeStart, previous);
        return result.toString();
    }

    private static void appendRange(StringBuilder result, int first, int last) {
        result.append('#').append(first);
        if (last != first) {
            result.append("-#").append(last);
        }
    }

    public static List<MapCatalogMapDisplayGroup> fromMaps(
            List<MapCatalogMapInfo> maps,
            net.minecraft.util.Identifier dimension,
            boolean onlyLevelOne,
            boolean onlyPlayerMaps
    ) {
        java.util.Map<Key, List<MapCatalogMapInfo>> grouped = new java.util.HashMap<>();
        for (MapCatalogMapInfo map : maps) {
            if (!map.dimension().equals(dimension)) {
                continue;
            }
            if (onlyLevelOne && map.scale() != 0) {
                continue;
            }
            if (onlyPlayerMaps && map.classification().hasExplorationMarker()) {
                continue;
            }
            grouped.computeIfAbsent(new Key(map.dimension(), map.centerX(), map.centerZ(), map.scale()), ignored -> new ArrayList<>())
                    .add(map);
        }

        return grouped.values().stream()
                .map(group -> new MapCatalogMapDisplayGroup(
                        group.stream().min(Comparator.comparingInt(MapCatalogMapInfo::mapId)).orElseThrow(),
                        group.stream().map(MapCatalogMapInfo::mapId).toList()))
                .sorted(Comparator.comparingInt((MapCatalogMapDisplayGroup group) -> group.representative().centerX())
                        .thenComparingInt(group -> group.representative().centerZ()))
                .toList();
    }

    private record Key(net.minecraft.util.Identifier dimension, int centerX, int centerZ, byte scale) {
    }
}
