package io.github.devskycore.faunareborn.animal.chicken.hostility;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import org.bukkit.entity.Chicken;

final class ChickenTracker {

    private final Int2ObjectOpenHashMap<ChickenHostilityBrain> brains = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<Chicken> trackedChickens = new Int2ObjectOpenHashMap<>();

    private IntIterator processingCursor;
    private IntIterator cleanupCursor;

    Int2ObjectOpenHashMap<ChickenHostilityBrain> brains() {
        return brains;
    }

    Int2ObjectOpenHashMap<Chicken> trackedChickens() {
        return trackedChickens;
    }

    boolean hasTrackedChickens() {
        return !trackedChickens.isEmpty();
    }

    int size() {
        return trackedChickens.size();
    }

    Chicken chicken(int chickenId) {
        return trackedChickens.get(chickenId);
    }

    ChickenHostilityBrain brain(int chickenId) {
        return brains.get(chickenId);
    }

    void putBrain(int chickenId, ChickenHostilityBrain brain) {
        brains.put(chickenId, brain);
    }

    void removeBrain(int chickenId) {
        brains.remove(chickenId);
    }

    boolean isTracked(int chickenId) {
        return trackedChickens.containsKey(chickenId);
    }

    void track(int chickenId, Chicken chicken) {
        trackedChickens.put(chickenId, chicken);
        resetCursors();
    }

    void untrack(int chickenId) {
        trackedChickens.remove(chickenId);
        resetCursors();
    }

    void resetCursors() {
        processingCursor = null;
        cleanupCursor = null;
    }

    void clear() {
        trackedChickens.clear();
        brains.clear();
        resetCursors();
    }

    int nextProcessingChickenId() {
        if (trackedChickens.isEmpty()) {
            return Integer.MIN_VALUE;
        }
        if (processingCursor == null || !processingCursor.hasNext()) {
            processingCursor = trackedChickens.keySet().iterator();
        }
        if (!processingCursor.hasNext()) {
            return Integer.MIN_VALUE;
        }
        return processingCursor.nextInt();
    }

    void prepareCleanupCursor() {
        if (trackedChickens.isEmpty()) {
            cleanupCursor = null;
            return;
        }
        if (cleanupCursor == null || !cleanupCursor.hasNext()) {
            cleanupCursor = trackedChickens.keySet().iterator();
        }
    }

    boolean hasCleanupCandidate() {
        return cleanupCursor != null && cleanupCursor.hasNext();
    }

    int nextCleanupChickenId() {
        if (!cleanupCursor.hasNext()) {
            return Integer.MIN_VALUE;
        }
        return cleanupCursor.nextInt();
    }
}
