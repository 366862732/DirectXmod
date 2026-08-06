/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.scores;

import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.TeamColor;
import org.jspecify.annotations.Nullable;

public class PlayerTeam
extends Team {
    private static final byte BIT_FRIENDLY_FIRE = 1;
    private static final byte BIT_SEE_INVISIBLES = 2;
    private final Scoreboard scoreboard;
    private final String name;
    private final Set<String> players = Sets.newHashSet();
    private Component displayName;
    private Component playerPrefix = CommonComponents.EMPTY;
    private Component playerSuffix = CommonComponents.EMPTY;
    private boolean allowFriendlyFire = true;
    private boolean seeFriendlyInvisibles = true;
    private Team.Visibility nameTagVisibility = Team.Visibility.ALWAYS;
    private Team.Visibility deathMessageVisibility = Team.Visibility.ALWAYS;
    private Optional<TeamColor> color = Optional.empty();
    private Team.CollisionRule collisionRule = Team.CollisionRule.ALWAYS;
    private final Style displayNameStyle;

    public PlayerTeam(Scoreboard scoreboard, String name) {
        this.scoreboard = scoreboard;
        this.name = name;
        this.displayName = Component.literal(name);
        this.displayNameStyle = Style.EMPTY.withInsertion(name).withHoverEvent(new HoverEvent.ShowText(Component.literal(name)));
    }

    public Packed pack() {
        return new Packed(this.name, Optional.of(this.displayName), this.color, this.allowFriendlyFire, this.seeFriendlyInvisibles, this.playerPrefix, this.playerSuffix, this.nameTagVisibility, this.deathMessageVisibility, this.collisionRule, List.copyOf(this.players));
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    private MutableComponent applyColor(MutableComponent result) {
        this.color.ifPresent(teamColor -> result.withColor(teamColor.textColor()));
        return result;
    }

    public MutableComponent getFormattedDisplayName() {
        return this.applyColor(ComponentUtils.wrapInSquareBrackets(this.displayName.copy().withStyle(this.displayNameStyle)));
    }

    public void setDisplayName(Component displayName) {
        if (displayName == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        this.displayName = displayName;
        this.scoreboard.onTeamChanged(this);
    }

    public void setPlayerPrefix(@Nullable Component playerPrefix) {
        this.playerPrefix = playerPrefix == null ? CommonComponents.EMPTY : playerPrefix;
        this.scoreboard.onTeamChanged(this);
    }

    public Component getPlayerPrefix() {
        return this.playerPrefix;
    }

    public void setPlayerSuffix(@Nullable Component playerSuffix) {
        this.playerSuffix = playerSuffix == null ? CommonComponents.EMPTY : playerSuffix;
        this.scoreboard.onTeamChanged(this);
    }

    public Component getPlayerSuffix() {
        return this.playerSuffix;
    }

    @Override
    public Collection<String> getPlayers() {
        return this.players;
    }

    @Override
    public MutableComponent getFormattedName(Component teamMemberName) {
        return this.applyColor(Component.empty().append(this.playerPrefix).append(teamMemberName).append(this.playerSuffix));
    }

    public static MutableComponent formatNameForTeam(@Nullable Team team, Component name) {
        if (team == null) {
            return name.copy();
        }
        return team.getFormattedName(name);
    }

    @Override
    public boolean isAllowFriendlyFire() {
        return this.allowFriendlyFire;
    }

    public void setAllowFriendlyFire(boolean allowFriendlyFire) {
        this.allowFriendlyFire = allowFriendlyFire;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public boolean canSeeFriendlyInvisibles() {
        return this.seeFriendlyInvisibles;
    }

    public void setSeeFriendlyInvisibles(boolean seeFriendlyInvisibles) {
        this.seeFriendlyInvisibles = seeFriendlyInvisibles;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Team.Visibility getNameTagVisibility() {
        return this.nameTagVisibility;
    }

    @Override
    public Team.Visibility getDeathMessageVisibility() {
        return this.deathMessageVisibility;
    }

    public void setNameTagVisibility(Team.Visibility visibility) {
        this.nameTagVisibility = visibility;
        this.scoreboard.onTeamChanged(this);
    }

    public void setDeathMessageVisibility(Team.Visibility visibility) {
        this.deathMessageVisibility = visibility;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Team.CollisionRule getCollisionRule() {
        return this.collisionRule;
    }

    public void setCollisionRule(Team.CollisionRule collisionRule) {
        this.collisionRule = collisionRule;
        this.scoreboard.onTeamChanged(this);
    }

    public @OptionFlags byte packOptions() {
        byte result = 0;
        if (this.isAllowFriendlyFire()) {
            result = (byte)(result | 1);
        }
        if (this.canSeeFriendlyInvisibles()) {
            result = (byte)(result | 2);
        }
        return result;
    }

    public void unpackOptions(@OptionFlags byte options) {
        this.setAllowFriendlyFire((options & 1) != 0);
        this.setSeeFriendlyInvisibles((options & 2) != 0);
    }

    public void setColor(Optional<TeamColor> color) {
        this.color = color;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Optional<TeamColor> getColor() {
        return this.color;
    }

    public record Packed(String name, Optional<Component> displayName, Optional<TeamColor> color, boolean allowFriendlyFire, boolean seeFriendlyInvisibles, Component memberNamePrefix, Component memberNameSuffix, Team.Visibility nameTagVisibility, Team.Visibility deathMessageVisibility, Team.CollisionRule collisionRule, List<String> players) {
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.STRING.fieldOf("Name").forGetter(Packed::name), (App)ComponentSerialization.CODEC.optionalFieldOf("DisplayName").forGetter(Packed::displayName), (App)TeamColor.CODEC.optionalFieldOf("TeamColor").forGetter(Packed::color), (App)Codec.BOOL.optionalFieldOf("AllowFriendlyFire", (Object)true).forGetter(Packed::allowFriendlyFire), (App)Codec.BOOL.optionalFieldOf("SeeFriendlyInvisibles", (Object)true).forGetter(Packed::seeFriendlyInvisibles), (App)ComponentSerialization.CODEC.optionalFieldOf("MemberNamePrefix", (Object)CommonComponents.EMPTY).forGetter(Packed::memberNamePrefix), (App)ComponentSerialization.CODEC.optionalFieldOf("MemberNameSuffix", (Object)CommonComponents.EMPTY).forGetter(Packed::memberNameSuffix), (App)Team.Visibility.CODEC.optionalFieldOf("NameTagVisibility", (Object)Team.Visibility.ALWAYS).forGetter(Packed::nameTagVisibility), (App)Team.Visibility.CODEC.optionalFieldOf("DeathMessageVisibility", (Object)Team.Visibility.ALWAYS).forGetter(Packed::deathMessageVisibility), (App)Team.CollisionRule.CODEC.optionalFieldOf("CollisionRule", (Object)Team.CollisionRule.ALWAYS).forGetter(Packed::collisionRule), (App)Codec.STRING.listOf().optionalFieldOf("Players", List.of()).forGetter(Packed::players)).apply((Applicative)i, Packed::new));
    }

    @Retention(value=RetentionPolicy.CLASS)
    @Target(value={ElementType.TYPE_USE})
    public static @interface OptionFlags {
    }
}

