package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.xaero;

import net.minecraft.text.Text;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogBanner;
import xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.protocol.mapcatalog.MapCatalogMapInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A deduplicated banner marker collected from the synchronized map states. */
public record MapCatalogBannerDisplay(
        Identifier dimension,
        MapCatalogBanner banner,
        List<Integer> mapIds
) {
    public MapCatalogBannerDisplay {
        mapIds = mapIds == null ? List.of() : mapIds.stream().sorted().distinct().toList();
    }

    public DyeColor color() {
        return banner.color() == null ? DyeColor.WHITE : banner.color();
    }

    public String nameString() {
        return banner.name().map(Text::getString).orElse("");
    }

    public String mapIdsText() {
        if (mapIds.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        int start = mapIds.getFirst();
        int previous = start;
        for (int index = 1; index < mapIds.size(); index++) {
            int current = mapIds.get(index);
            if (current == previous + 1) {
                previous = current;
                continue;
            }
            appendRange(result, start, previous);
            result.append(", ");
            start = previous = current;
        }
        appendRange(result, start, previous);
        return result.toString();
    }

    public static List<MapCatalogBannerDisplay> fromMaps(
            List<MapCatalogMapInfo> maps,
            Identifier dimension,
            boolean onlyLevelOne,
            boolean onlyPlayerMaps
    ) {
        java.util.Map<Key, List<Integer>> grouped = new java.util.HashMap<>();
        for (MapCatalogMapInfo map : maps) {
            if (!map.dimension().equals(dimension)
                    || onlyLevelOne && map.scale() != 0
                    || onlyPlayerMaps && map.classification().hasExplorationMarker()) {
                continue;
            }
            for (MapCatalogBanner banner : map.classification().banners()) {
                if (banner == null) {
                    continue;
                }
                Key key = new Key(
                        map.dimension(),
                        banner.worldX(),
                        banner.worldZ(),
                        banner.color(),
                        banner.name().map(Text::getString).orElse("")
                );
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(map.mapId());
            }
        }
        return grouped.entrySet().stream()
                .map(entry -> new MapCatalogBannerDisplay(
                        entry.getKey().dimension(),
                        new MapCatalogBanner(
                                entry.getKey().worldX(),
                                entry.getKey().worldZ(),
                                entry.getKey().color(),
                                entry.getKey().name().isEmpty()
                                        ? java.util.Optional.empty()
                                        : java.util.Optional.of(Text.literal(entry.getKey().name()))
                        ),
                        entry.getValue()
                ))
                .sorted(Comparator.comparingInt((MapCatalogBannerDisplay display) -> display.banner().worldX())
                        .thenComparingInt(display -> display.banner().worldZ()))
                .toList();
    }

    private static void appendRange(StringBuilder result, int first, int last) {
        result.append('#').append(first);
        if (last != first) {
            result.append("-#").append(last);
        }
    }

    private record Key(
            Identifier dimension,
            int worldX,
            int worldZ,
            DyeColor color,
            String name
    ) {
    }
}
