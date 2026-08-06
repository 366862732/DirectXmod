/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2IntMap
 *  it.unimi.dsi.fastutil.objects.Object2IntMaps
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
 *  org.apache.commons.lang3.mutable.MutableBoolean
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.scores;

import com.google.common.collect.Lists;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerScores;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Scoreboard {
    public static final String HIDDEN_SCORE_PREFIX = "#";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Object2ObjectMap<String, Objective> objectivesByName = new Object2ObjectOpenHashMap(16, 0.5f);
    private final Reference2ObjectMap<ObjectiveCriteria, List<Objective>> objectivesByCriteria = new Reference2ObjectOpenHashMap();
    private final Map<String, PlayerScores> playerScores = new Object2ObjectOpenHashMap(16, 0.5f);
    private final Map<DisplaySlot, Objective> displayObjectives = new EnumMap<DisplaySlot, Objective>(DisplaySlot.class);
    private final Object2ObjectMap<String, PlayerTeam> teamsByName = new Object2ObjectOpenHashMap();
    private final Object2ObjectMap<String, PlayerTeam> teamsByPlayer = new Object2ObjectOpenHashMap();

    public @Nullable Objective getObjective(@Nullable String name) {
        return (Objective)this.objectivesByName.get((Object)name);
    }

    public Objective addObjective(String name, ObjectiveCriteria criteria, Component displayName, ObjectiveCriteria.RenderType renderType, boolean displayAutoUpdate, @Nullable NumberFormat numberFormat) {
        if (this.objectivesByName.containsKey((Object)name)) {
            throw new IllegalArgumentException("An objective with the name '" + name + "' already exists!");
        }
        Objective objective = new Objective(this, name, criteria, displayName, renderType, displayAutoUpdate, numberFormat);
        ((List)this.objectivesByCriteria.computeIfAbsent((Object)criteria, k -> Lists.newArrayList())).add(objective);
        this.objectivesByName.put((Object)name, (Object)objective);
        this.onObjectiveAdded(objective);
        return objective;
    }

    public final void forAllObjectives(ObjectiveCriteria criteria, ScoreHolder name, Consumer<ScoreAccess> operation) {
        ((List)this.objectivesByCriteria.getOrDefault((Object)criteria, Collections.emptyList())).forEach(o -> operation.accept(this.getOrCreatePlayerScore(name, (Objective)o, true)));
    }

    private PlayerScores getOrCreatePlayerInfo(String name) {
        return this.playerScores.computeIfAbsent(name, k -> new PlayerScores());
    }

    public ScoreAccess getOrCreatePlayerScore(ScoreHolder holder, Objective objective) {
        return this.getOrCreatePlayerScore(holder, objective, false);
    }

    public ScoreAccess getOrCreatePlayerScore(final ScoreHolder scoreHolder, final Objective objective, boolean forceWritable) {
        final boolean canModify = forceWritable || !objective.getCriteria().isReadOnly();
        PlayerScores playerScore = this.getOrCreatePlayerInfo(scoreHolder.getScoreboardName());
        final MutableBoolean requiresSync = new MutableBoolean();
        final Score score = playerScore.getOrCreate(objective, newScore -> requiresSync.setTrue());
        return new ScoreAccess(){
            final /* synthetic */ Scoreboard this$0;
            {
                Scoreboard scoreboard = this$0;
                Objects.requireNonNull(scoreboard);
                this.this$0 = scoreboard;
            }

            @Override
            public int get() {
                return score.value();
            }

            @Override
            public void set(int value) {
                Component newDisplay;
                if (!canModify) {
                    throw new IllegalStateException("Cannot modify read-only score");
                }
                boolean hasChanged = requiresSync.isTrue();
                if (objective.displayAutoUpdate() && (newDisplay = scoreHolder.getDisplayName()) != null && !newDisplay.equals(score.display())) {
                    score.display(newDisplay);
                    hasChanged = true;
                }
                if (value != score.value()) {
                    score.value(value);
                    hasChanged = true;
                }
                if (hasChanged) {
                    this.sendScoreToPlayers();
                }
            }

            @Override
            public @Nullable Component display() {
                return score.display();
            }

            @Override
            public void display(@Nullable Component display) {
                if (requiresSync.isTrue() || !Objects.equals(display, score.display())) {
                    score.display(display);
                    this.sendScoreToPlayers();
                }
            }

            @Override
            public void numberFormatOverride(@Nullable NumberFormat numberFormat) {
                score.numberFormat(numberFormat);
                this.sendScoreToPlayers();
            }

            @Override
            public boolean locked() {
                return score.isLocked();
            }

            @Override
            public void unlock() {
                this.setLocked(false);
            }

            @Override
            public void lock() {
                this.setLocked(true);
            }

            private void setLocked(boolean locked) {
                score.setLocked(locked);
                if (requiresSync.isTrue()) {
                    this.sendScoreToPlayers();
                }
                this.this$0.onScoreLockChanged(scoreHolder, objective);
            }

            private void sendScoreToPlayers() {
                this.this$0.onScoreChanged(scoreHolder, objective, score);
                requiresSync.setFalse();
            }
        };
    }

    public @Nullable ReadOnlyScoreInfo getPlayerScoreInfo(ScoreHolder name, Objective objective) {
        PlayerScores playerScore = this.playerScores.get(name.getScoreboardName());
        if (playerScore != null) {
            return playerScore.get(objective);
        }
        return null;
    }

    public Collection<PlayerScoreEntry> listPlayerScores(Objective objective) {
        ArrayList<PlayerScoreEntry> result = new ArrayList<PlayerScoreEntry>();
        this.playerScores.forEach((player, scores) -> {
            Score score = scores.get(objective);
            if (score != null) {
                result.add(new PlayerScoreEntry((String)player, score.value(), score.display(), score.numberFormat()));
            }
        });
        return result;
    }

    public Collection<Objective> getObjectives() {
        return this.objectivesByName.values();
    }

    public Collection<String> getObjectiveNames() {
        return this.objectivesByName.keySet();
    }

    public Collection<ScoreHolder> getTrackedPlayers() {
        return this.playerScores.keySet().stream().map(ScoreHolder::forNameOnly).toList();
    }

    public void resetAllPlayerScores(ScoreHolder player) {
        PlayerScores removed = this.playerScores.remove(player.getScoreboardName());
        if (removed != null) {
            this.onPlayerRemoved(player);
        }
    }

    public void resetSinglePlayerScore(ScoreHolder player, Objective objective) {
        PlayerScores scores = this.playerScores.get(player.getScoreboardName());
        if (scores != null) {
            boolean hasRemoved = scores.remove(objective);
            if (!scores.hasScores()) {
                PlayerScores removedPlayer = this.playerScores.remove(player.getScoreboardName());
                if (removedPlayer != null) {
                    this.onPlayerRemoved(player);
                }
            } else if (hasRemoved) {
                this.onPlayerScoreRemoved(player, objective);
            }
        }
    }

    public Object2IntMap<Objective> listPlayerScores(ScoreHolder player) {
        PlayerScores scores = this.playerScores.get(player.getScoreboardName());
        return scores != null ? scores.listScores() : Object2IntMaps.emptyMap();
    }

    public void removeObjective(Objective objective) {
        this.objectivesByName.remove((Object)objective.getName());
        for (DisplaySlot value : DisplaySlot.values()) {
            if (this.getDisplayObjective(value) != objective) continue;
            this.setDisplayObjective(value, null);
        }
        List objectives = (List)this.objectivesByCriteria.get((Object)objective.getCriteria());
        if (objectives != null) {
            objectives.remove(objective);
        }
        for (PlayerScores playerScore : this.playerScores.values()) {
            playerScore.remove(objective);
        }
        this.onObjectiveRemoved(objective);
    }

    public void setDisplayObjective(DisplaySlot slot, @Nullable Objective objective) {
        this.displayObjectives.put(slot, objective);
    }

    public @Nullable Objective getDisplayObjective(DisplaySlot slot) {
        return this.displayObjectives.get(slot);
    }

    public @Nullable PlayerTeam getPlayerTeam(String name) {
        return (PlayerTeam)this.teamsByName.get((Object)name);
    }

    public PlayerTeam addPlayerTeam(String name) {
        PlayerTeam team = this.getPlayerTeam(name);
        if (team != null) {
            LOGGER.warn("Requested creation of existing team '{}'", (Object)name);
            return team;
        }
        team = new PlayerTeam(this, name);
        this.teamsByName.put((Object)name, (Object)team);
        this.onTeamAdded(team);
        return team;
    }

    public void removePlayerTeam(PlayerTeam team) {
        this.teamsByName.remove((Object)team.getName());
        for (String player : team.getPlayers()) {
            this.teamsByPlayer.remove((Object)player);
        }
        this.onTeamRemoved(team);
    }

    public boolean addPlayerToTeam(String player, PlayerTeam team) {
        if (this.getPlayersTeam(player) != null) {
            this.removePlayerFromTeam(player);
        }
        this.teamsByPlayer.put((Object)player, (Object)team);
        return team.getPlayers().add(player);
    }

    public boolean removePlayerFromTeam(String player) {
        PlayerTeam team = this.getPlayersTeam(player);
        if (team != null) {
            this.removePlayerFromTeam(player, team);
            return true;
        }
        return false;
    }

    public void removePlayerFromTeam(String player, PlayerTeam team) {
        if (this.getPlayersTeam(player) != team) {
            throw new IllegalStateException("Player is either on another team or not on any team. Cannot remove from team '" + team.getName() + "'.");
        }
        this.teamsByPlayer.remove((Object)player);
        team.getPlayers().remove(player);
    }

    public Collection<String> getTeamNames() {
        return this.teamsByName.keySet();
    }

    public Collection<PlayerTeam> getPlayerTeams() {
        return this.teamsByName.values();
    }

    public @Nullable PlayerTeam getPlayersTeam(String name) {
        return (PlayerTeam)this.teamsByPlayer.get((Object)name);
    }

    public void onObjectiveAdded(Objective objective) {
    }

    public void onObjectiveChanged(Objective objective) {
    }

    public void onObjectiveRemoved(Objective objective) {
    }

    protected void onScoreChanged(ScoreHolder owner, Objective objective, Score score) {
    }

    protected void onScoreLockChanged(ScoreHolder owner, Objective objective) {
    }

    public void onPlayerRemoved(ScoreHolder player) {
    }

    public void onPlayerScoreRemoved(ScoreHolder player, Objective objective) {
    }

    public void onTeamAdded(PlayerTeam team) {
    }

    public void onTeamChanged(PlayerTeam team) {
    }

    public void onTeamRemoved(PlayerTeam team) {
    }

    public void entityRemoved(Entity entity) {
        if (entity instanceof Player || entity.isAlive()) {
            return;
        }
        this.resetAllPlayerScores(entity);
        this.removePlayerFromTeam(entity.getScoreboardName());
    }

    protected List<PackedScore> packPlayerScores() {
        return this.playerScores.entrySet().stream().flatMap(playerEntry -> {
            String player = (String)playerEntry.getKey();
            return ((PlayerScores)playerEntry.getValue()).listRawScores().entrySet().stream().map(entry -> new PackedScore(player, ((Objective)entry.getKey()).getName(), ((Score)entry.getValue()).pack()));
        }).toList();
    }

    protected void loadPlayerScore(PackedScore score) {
        Objective objective = this.getObjective(score.objective);
        if (objective == null) {
            LOGGER.error("Unknown objective {} for name {}, ignoring", (Object)score.objective, (Object)score.owner);
            return;
        }
        this.getOrCreatePlayerInfo(score.owner).setScore(objective, new Score(score.score));
    }

    protected List<PlayerTeam.Packed> packPlayerTeams() {
        return this.getPlayerTeams().stream().map(PlayerTeam::pack).toList();
    }

    protected void loadPlayerTeam(PlayerTeam.Packed packed) {
        PlayerTeam team = this.addPlayerTeam(packed.name());
        packed.displayName().ifPresent(team::setDisplayName);
        team.setColor(packed.color());
        team.setAllowFriendlyFire(packed.allowFriendlyFire());
        team.setSeeFriendlyInvisibles(packed.seeFriendlyInvisibles());
        team.setPlayerPrefix(packed.memberNamePrefix());
        team.setPlayerSuffix(packed.memberNameSuffix());
        team.setNameTagVisibility(packed.nameTagVisibility());
        team.setDeathMessageVisibility(packed.deathMessageVisibility());
        team.setCollisionRule(packed.collisionRule());
        for (String player : packed.players()) {
            this.addPlayerToTeam(player, team);
        }
    }

    protected List<Objective.Packed> packObjectives() {
        return this.getObjectives().stream().map(Objective::pack).toList();
    }

    protected void loadObjective(Objective.Packed objective) {
        this.addObjective(objective.name(), objective.criteria(), objective.displayName(), objective.renderType(), objective.displayAutoUpdate(), objective.numberFormat().orElse(null));
    }

    protected Map<DisplaySlot, String> packDisplaySlots() {
        EnumMap<DisplaySlot, String> displaySlots = new EnumMap<DisplaySlot, String>(DisplaySlot.class);
        for (DisplaySlot slot : DisplaySlot.values()) {
            Objective objective = this.getDisplayObjective(slot);
            if (objective == null) continue;
            displaySlots.put(slot, objective.getName());
        }
        return displaySlots;
    }

    public record PackedScore(String owner, String objective, Score.Packed score) {
        public static final Codec<PackedScore> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.fieldOf("Name").forGetter(PackedScore::owner), (App)Codec.STRING.fieldOf("Objective").forGetter(PackedScore::objective), (App)Score.Packed.MAP_CODEC.forGetter(PackedScore::score)).apply((Applicative)i, PackedScore::new));
    }
}

