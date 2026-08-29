package xyz.water.rmatrix.cmod.not_a_good_mod_for_survival.client.impl.global_search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Value baselines that identify which indexed options were modified in a GUI session. */
public final class GlobalSearchBaselineStore {
    private final Map<String, String> baselines = new HashMap<>();

    /** Registers a baseline without overwriting a value that is still pending a diff. */
    public void recordIfAbsent(String key, String value) {
        if (value != null) {
            this.baselines.putIfAbsent(key, value);
        }
    }

    /** Drops baselines for keys that are no longer part of the searchable snapshot. */
    public void retainKeys(Set<String> liveKeys) {
        this.baselines.keySet().retainAll(liveKeys);
    }

    /**
     * Returns the keys whose current values differ from their baselines and commits the
     * new values. Keys without a baseline yet are recorded silently, and keys with an
     * unreadable (null) current value are treated as unchanged.
     */
    public List<String> diffAndCommit(Map<String, String> currentValues) {
        List<String> changedKeys = new ArrayList<>();

        for (Map.Entry<String, String> entry : currentValues.entrySet()) {
            String key = entry.getKey();
            String current = entry.getValue();
            String baseline = this.baselines.get(key);

            if (current == null) {
                continue;
            }

            if (baseline == null) {
                this.baselines.put(key, current);
            } else if (!baseline.equals(current)) {
                changedKeys.add(key);
                this.baselines.put(key, current);
            }
        }

        return List.copyOf(changedKeys);
    }
}