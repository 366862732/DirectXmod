/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.scores;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Score;
import org.jspecify.annotations.Nullable;

class PlayerScores {
    private final Reference2ObjectOpenHashMap<Objective, Score> scores = new Reference2ObjectOpenHashMap(16, 0.5f);

    PlayerScores() {
    }

    public @Nullable Score get(Objective objective) {
        return (Score)this.scores.get((Object)objective);
    }

    public Score getOrCreate(Objective objective, Consumer<Score> newResultCallback) {
        return (Score)this.scores.computeIfAbsent((Object)objective, obj -> {
            Score newScore = new Score();
            newResultCallback.accept(newScore);
            return newScore;
        });
    }

    public boolean remove(Objective objective) {
        return this.scores.remove((Object)objective) != null;
    }

    public boolean hasScores() {
        return !this.scores.isEmpty();
    }

    public Object2IntMap<Objective> listScores() {
        Object2IntOpenHashMap result = new Object2IntOpenHashMap();
        this.scores.forEach((arg_0, arg_1) -> PlayerScores.lambda$listScores$0((Object2IntMap)result, arg_0, arg_1));
        return result;
    }

    void setScore(Objective objective, Score score) {
        this.scores.put((Object)objective, (Object)score);
    }

    Map<Objective, Score> listRawScores() {
        return Collections.unmodifiableMap(this.scores);
    }

    private static /* synthetic */ void lambda$listScores$0(Object2IntMap result, Objective objective, Score score) {
        result.put((Object)objective, score.value());
    }
}

