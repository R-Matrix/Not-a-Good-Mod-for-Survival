package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Matches a {@code #} keybind query against a Malilib key name such as LEFT_SHIFT or KP_1. */
public final class GlobalSearchKeyMatcher {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern MOUSE_BUTTON = Pattern.compile("button(\\d+)");

    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String KP = "kp";

    private static final Set<String> NUMPAD_PREFIXES = Set.of("kp", "num", "numpad");

    private static final Map<String, Set<String>> BASE_ALIASES = Map.ofEntries(
            Map.entry("control", Set.of("ctrl")),
            Map.entry("escape", Set.of("esc")),
            Map.entry("delete", Set.of("del")),
            Map.entry("insert", Set.of("ins")),
            Map.entry("backspace", Set.of("back", "bksp")),
            Map.entry("printscreen", Set.of("print", "prtsc")),
            Map.entry("pageup", Set.of("pgup")),
            Map.entry("pagedown", Set.of("pgdn", "pagedn")),
            Map.entry("capslock", Set.of("caps")),
            Map.entry("scrolllock", Set.of("scroll")),
            Map.entry("super", Set.of("win", "windows", "meta")),
            Map.entry("space", Set.of("spacebar")),
            Map.entry("menu", Set.of("contextmenu", "apps")),
            Map.entry("pause", Set.of("break")),
            Map.entry("add", Set.of("plus")),
            Map.entry("subtract", Set.of("minus", "dash")),
            Map.entry("multiply", Set.of("star", "asterisk")),
            Map.entry("divide", Set.of("slash")),
            Map.entry("decimal", Set.of("dot", "period"))
    );

    private GlobalSearchKeyMatcher() {
    }

    public static boolean matches(String query, String keyName) {
        String wanted = normalize(query);
        String name = normalize(keyName);

        if (wanted.isEmpty() || name.isEmpty()) {
            return false;
        }

        if (wanted.equals(name)) {
            return true;
        }

        Matcher mouseButton = MOUSE_BUTTON.matcher(name);

        if (mouseButton.matches()) {
            String index = mouseButton.group(1);

            return wanted.equals("mouse")
                    || wanted.equals("mouse" + index)
                    || wanted.equals("m" + index);
        }

        String side = null;
        String base = name;

        if (name.startsWith(LEFT) && name.length() > LEFT.length()) {
            side = LEFT;
            base = name.substring(LEFT.length());
        } else if (name.startsWith(RIGHT) && name.length() > RIGHT.length()) {
            side = RIGHT;
            base = name.substring(RIGHT.length());
        }

        if (wanted.equals(side)) {
            return true;
        }

        if (base.startsWith(KP) && base.length() > KP.length()) {
            if (NUMPAD_PREFIXES.contains(wanted)) {
                return true;
            }

            String suffix = base.substring(KP.length());
            Set<String> suffixAliases = BASE_ALIASES.getOrDefault(suffix, Set.of());

            for (String prefix : NUMPAD_PREFIXES) {
                if (wanted.equals(prefix + suffix)) {
                    return true;
                }

                for (String alias : suffixAliases) {
                    if (wanted.equals(prefix + alias)) {
                        return true;
                    }
                }
            }
        }

        return wanted.equals(base) || BASE_ALIASES.getOrDefault(base, Set.of()).contains(wanted);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return NON_ALPHANUMERIC.matcher(value).replaceAll("").toLowerCase(Locale.ROOT);
    }
}