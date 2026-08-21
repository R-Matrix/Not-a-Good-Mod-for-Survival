package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Parser and matcher for the global Malilib search query language. */
public final class GlobalSearchQuery {
    private final List<List<Condition>> groups;

    private GlobalSearchQuery(List<List<Condition>> groups) {
        this.groups = groups;
    }

    public static GlobalSearchQuery parse(String input) {
        if (input == null || input.isBlank()) {
            return new GlobalSearchQuery(Collections.emptyList());
        }

        List<List<Condition>> groups = new ArrayList<>();

        for (String groupText : input.split("\\|", -1)) {
            List<Condition> conditions = new ArrayList<>();

            for (String token : groupText.trim().split("\\s+")) {
                if (!token.isEmpty()) {
                    conditions.add(Condition.parse(token));
                }
            }

            if (!conditions.isEmpty()) {
                groups.add(List.copyOf(conditions));
            }
        }

        return new GlobalSearchQuery(List.copyOf(groups));
    }

    public boolean matches(GlobalSearchOption entry) {
        if (this.groups.isEmpty()) {
            return true;
        }

        for (List<Condition> group : this.groups) {
            boolean matches = true;

            for (Condition condition : group) {
                if (!condition.matches(entry)) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return true;
            }
        }

        return false;
    }

    /** Returns the plain-text terms that can be highlighted in a visible config name. */
    public List<String> getTextTerms() {
        return this.groups.stream()
                .flatMap(List::stream)
                .filter(condition -> condition.kind() == Kind.TEXT && !condition.value().isBlank())
                .map(Condition::value)
                .distinct()
                .toList();
    }

    private enum Kind {
        TEXT,
        MOD,
        KEY,
        CATEGORY
    }

    private record Condition(Kind kind, String value) {
        private static Condition parse(String token) {
            Kind kind = Kind.TEXT;
            String value = token;

            if (token.length() > 1) {
                kind = switch (token.charAt(0)) {
                    case '@' -> Kind.MOD;
                    case '#' -> Kind.KEY;
                    case '%' -> Kind.CATEGORY;
                    default -> Kind.TEXT;
                };

                if (kind != Kind.TEXT) {
                    value = token.substring(1);
                }
            }

            return new Condition(kind, value.toLowerCase(Locale.ROOT));
        }

        private boolean matches(GlobalSearchOption entry) {
            GlobalSearchMetadata metadata = entry.getMetadata();

            return switch (this.kind) {
                case MOD -> metadata.matchesMod(this.value);
                case KEY -> metadata.matchesKey(this.value);
                case CATEGORY -> metadata.matchesCategory(this.value);
                case TEXT -> metadata.matchesText(entry.getConfig(), this.value);
            };
        }
    }
}
