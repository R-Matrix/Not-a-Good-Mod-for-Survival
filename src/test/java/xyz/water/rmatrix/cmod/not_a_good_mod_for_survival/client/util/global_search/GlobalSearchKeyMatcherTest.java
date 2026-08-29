package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.util.global_search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalSearchKeyMatcherTest {
    @Test
    void exactNamesStillMatch() {
        assertTrue(GlobalSearchKeyMatcher.matches("h", "H"));
        assertTrue(GlobalSearchKeyMatcher.matches("leftshift", "LEFT_SHIFT"));
        assertTrue(GlobalSearchKeyMatcher.matches("f12", "F12"));
        assertTrue(GlobalSearchKeyMatcher.matches("left", "LEFT"));
        assertTrue(GlobalSearchKeyMatcher.matches("1", "1"));
    }

    @Test
    void singleLetterQueriesDoNotMatchLongerNames() {
        assertFalse(GlobalSearchKeyMatcher.matches("h", "LEFT_SHIFT"));
        assertFalse(GlobalSearchKeyMatcher.matches("f", "F1"));
    }

    @Test
    void sidelessCoreMatchesAllSidedKeys() {
        assertTrue(GlobalSearchKeyMatcher.matches("shift", "LEFT_SHIFT"));
        assertTrue(GlobalSearchKeyMatcher.matches("shift", "RIGHT_SHIFT"));
        assertFalse(GlobalSearchKeyMatcher.matches("rightshift", "LEFT_SHIFT"));
        assertFalse(GlobalSearchKeyMatcher.matches("shift", "H"));
    }

    @Test
    void sideQueriesMatchOnlyTheirOwnSide() {
        assertTrue(GlobalSearchKeyMatcher.matches("left", "LEFT_CONTROL"));
        assertTrue(GlobalSearchKeyMatcher.matches("left", "LEFT_ALT"));
        assertTrue(GlobalSearchKeyMatcher.matches("left", "LEFT_SHIFT"));
        assertFalse(GlobalSearchKeyMatcher.matches("left", "RIGHT_SHIFT"));
        assertTrue(GlobalSearchKeyMatcher.matches("right", "RIGHT_ALT"));
    }

    @Test
    void aliasesMatchTheirCanonicalKeys() {
        assertTrue(GlobalSearchKeyMatcher.matches("ctrl", "LEFT_CONTROL"));
        assertTrue(GlobalSearchKeyMatcher.matches("ctrl", "RIGHT_CONTROL"));
        assertFalse(GlobalSearchKeyMatcher.matches("ctrl", "LEFT_ALT"));
        assertTrue(GlobalSearchKeyMatcher.matches("back", "BACKSPACE"));
        assertTrue(GlobalSearchKeyMatcher.matches("esc", "ESCAPE"));
        assertTrue(GlobalSearchKeyMatcher.matches("del", "DELETE"));
        assertTrue(GlobalSearchKeyMatcher.matches("ins", "INSERT"));
        assertTrue(GlobalSearchKeyMatcher.matches("pgup", "PAGE_UP"));
        assertTrue(GlobalSearchKeyMatcher.matches("pgdn", "PAGE_DOWN"));
        assertTrue(GlobalSearchKeyMatcher.matches("print", "PRINT_SCREEN"));
        assertTrue(GlobalSearchKeyMatcher.matches("prtsc", "PRINT_SCREEN"));
        assertTrue(GlobalSearchKeyMatcher.matches("caps", "CAPS_LOCK"));
        assertTrue(GlobalSearchKeyMatcher.matches("win", "LEFT_SUPER"));
        assertTrue(GlobalSearchKeyMatcher.matches("meta", "RIGHT_SUPER"));
        assertTrue(GlobalSearchKeyMatcher.matches("scroll", "SCROLL_LOCK"));
    }

    @Test
    void numpadKeysMatchAllPrefixForms() {
        assertTrue(GlobalSearchKeyMatcher.matches("num1", "KP_1"));
        assertTrue(GlobalSearchKeyMatcher.matches("num9", "KP_9"));
        assertTrue(GlobalSearchKeyMatcher.matches("kp5", "KP_5"));
        assertTrue(GlobalSearchKeyMatcher.matches("numpad9", "KP_9"));
        assertTrue(GlobalSearchKeyMatcher.matches("numenter", "KP_ENTER"));
        assertTrue(GlobalSearchKeyMatcher.matches("kpenter", "KP_ENTER"));
        assertTrue(GlobalSearchKeyMatcher.matches("numplus", "KP_ADD"));
        assertTrue(GlobalSearchKeyMatcher.matches("kpstar", "KP_MULTIPLY"));
        assertTrue(GlobalSearchKeyMatcher.matches("numdot", "KP_DECIMAL"));
    }

    @Test
    void numpadGroupQueriesMatchOnlyNumpadKeys() {
        assertTrue(GlobalSearchKeyMatcher.matches("kp", "KP_3"));
        assertTrue(GlobalSearchKeyMatcher.matches("num", "KP_3"));
        assertTrue(GlobalSearchKeyMatcher.matches("numpad", "KP_3"));
        assertFalse(GlobalSearchKeyMatcher.matches("num", "NUM_LOCK"));
        assertTrue(GlobalSearchKeyMatcher.matches("numlock", "NUM_LOCK"));
        assertFalse(GlobalSearchKeyMatcher.matches("enter", "KP_ENTER"));
        assertTrue(GlobalSearchKeyMatcher.matches("enter", "ENTER"));
    }

    @Test
    void mouseButtonsMatchAliasAndGroupQueries() {
        assertTrue(GlobalSearchKeyMatcher.matches("button1", "BUTTON_1"));
        assertTrue(GlobalSearchKeyMatcher.matches("mouse1", "BUTTON_1"));
        assertTrue(GlobalSearchKeyMatcher.matches("m1", "BUTTON_1"));
        assertTrue(GlobalSearchKeyMatcher.matches("mouse", "BUTTON_3"));
        assertFalse(GlobalSearchKeyMatcher.matches("left", "BUTTON_1"));
        assertFalse(GlobalSearchKeyMatcher.matches("m9", "BUTTON_1"));
    }

    @Test
    void normalizationAndFallbacks() {
        assertTrue(GlobalSearchKeyMatcher.matches("Left-Shift", "LEFT_SHIFT"));
        assertTrue(GlobalSearchKeyMatcher.matches("LEFT SHIFT", "left_shift"));
        assertTrue(GlobalSearchKeyMatcher.matches("foobar", "FOO_BAR"));
        assertFalse(GlobalSearchKeyMatcher.matches("foo", "FOO_BAR"));
        assertFalse(GlobalSearchKeyMatcher.matches("foo", "OTHER"));
        assertFalse(GlobalSearchKeyMatcher.matches(null, "H"));
        assertFalse(GlobalSearchKeyMatcher.matches("h", null));
        assertFalse(GlobalSearchKeyMatcher.matches("", "H"));
        assertFalse(GlobalSearchKeyMatcher.matches("h", " "));
    }
}