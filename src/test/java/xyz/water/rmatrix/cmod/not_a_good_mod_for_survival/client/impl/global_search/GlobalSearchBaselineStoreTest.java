package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalSearchBaselineStoreTest {
    private static final String KEY_A = "mod\u0000a";
    private static final String KEY_B = "mod\u0000b";

    @Test
    void freshBaselinesProduceNoChanges() {
        GlobalSearchBaselineStore store = new GlobalSearchBaselineStore();
        store.recordIfAbsent(KEY_A, "1");
        store.recordIfAbsent(KEY_B, "2");

        assertEquals(List.of(), store.diffAndCommit(Map.of(KEY_A, "1", KEY_B, "2")));
    }

    @Test
    void rebuildDoesNotOverwritePendingBaseline() {
        GlobalSearchBaselineStore store = new GlobalSearchBaselineStore();
        store.recordIfAbsent(KEY_A, "1");
        store.recordIfAbsent(KEY_A, "2");

        assertEquals(List.of(KEY_A), store.diffAndCommit(Map.of(KEY_A, "2")));
        assertEquals(List.of(), store.diffAndCommit(Map.of(KEY_A, "2")));
    }

    @Test
    void revertedValueIsNotDirty() {
        GlobalSearchBaselineStore store = new GlobalSearchBaselineStore();
        store.recordIfAbsent(KEY_A, "1");

        assertEquals(List.of(), store.diffAndCommit(Map.of(KEY_A, "1")));
    }

    @Test
    void changedValueIsDetectedOnceAndThenCommitted() {
        GlobalSearchBaselineStore store = new GlobalSearchBaselineStore();
        store.recordIfAbsent(KEY_A, "1");

        assertEquals(List.of(KEY_A), store.diffAndCommit(Map.of(KEY_A, "5")));
        assertEquals(List.of(), store.diffAndCommit(Map.of(KEY_A, "5")));
    }

    @Test
    void unreadableValuesAndMissingBaselinesAreNotDirty() {
        GlobalSearchBaselineStore store = new GlobalSearchBaselineStore();
        store.recordIfAbsent(KEY_A, "1");
        store.recordIfAbsent(KEY_B, null);

        Map<String, String> current = new HashMap<>();
        current.put(KEY_A, null);
        current.put(KEY_B, "x");
        current.put("mod\u0000c", "y");
        assertEquals(List.of(), store.diffAndCommit(current));

        assertEquals(List.of(KEY_B), store.diffAndCommit(Map.of(KEY_B, "z")));
        assertEquals(List.of(KEY_A), store.diffAndCommit(Map.of(KEY_A, "2", KEY_B, "z")));
    }

    @Test
    void retainKeysDropsEntriesThatNoLongerExist() {
        GlobalSearchBaselineStore store = new GlobalSearchBaselineStore();
        store.recordIfAbsent("gone", "1");
        store.recordIfAbsent(KEY_A, "1");

        store.retainKeys(Set.of(KEY_A));
        store.recordIfAbsent("gone", "2");

        assertEquals(List.of(), store.diffAndCommit(Map.of("gone", "2", KEY_A, "1")));
    }
}