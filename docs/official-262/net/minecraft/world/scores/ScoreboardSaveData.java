/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.scores;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class ScoreboardSaveData
extends SavedData {
    public static final SavedDataType<ScoreboardSaveData> TYPE = new SavedDataType<ScoreboardSaveData>(Identifier.withDefaultNamespace("scoreboard"), ScoreboardSaveData::new, Packed.CODEC.xmap(ScoreboardSaveData::new, ScoreboardSaveData::getData), DataFixTypes.SAVED_DATA_SCOREBOARD);
    private Packed data;

    private ScoreboardSaveData() {
        this(Packed.EMPTY);
    }

    public ScoreboardSaveData(Packed data) {
        this.data = data;
    }

    public Packed getData() {
        return this.data;
    }

    public void setData(Packed data) {
        if (!data.equals(this.data)) {
            this.data = data;
            this.setDirty();
        }
    }

    public record Packed(List<Objective.Packed> objectives, List<Scoreboard.PackedScore> scores, Map<DisplaySlot, String> displaySlots, List<PlayerTeam.Packed> teams) {
        public static final Packed EMPTY = new Packed(List.of(), List.of(), Map.of(), List.of());
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)Objective.Packed.CODEC.listOf().optionalFieldOf("Objectives", List.of()).forGetter(Packed::objectives), (App)Scoreboard.PackedScore.CODEC.listOf().optionalFieldOf("PlayerScores", List.of()).forGetter(Packed::scores), (App)Codec.unboundedMap(DisplaySlot.CODEC, (Codec)Codec.STRING).optionalFieldOf("DisplaySlots", Map.of()).forGetter(Packed::displaySlots), (App)PlayerTeam.Packed.CODEC.listOf().optionalFieldOf("Teams", List.of()).forGetter(Packed::teams)).apply((Applicative)i, Packed::new));
    }
}

