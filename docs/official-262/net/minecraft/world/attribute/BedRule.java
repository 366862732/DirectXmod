/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.attribute;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public record BedRule(Rule canSleep, Rule canSetSpawn, boolean explodes, Optional<Component> errorMessage) {
    public static final BedRule CAN_SLEEP_WHEN_DARK = new BedRule(Rule.WHEN_DARK, Rule.ALWAYS, false, Optional.of(Component.translatable("block.minecraft.bed.no_sleep")));
    public static final BedRule EXPLODES = new BedRule(Rule.NEVER, Rule.NEVER, true, Optional.empty());
    public static final Codec<BedRule> CODEC = RecordCodecBuilder.create(i -> i.group((App)Rule.CODEC.fieldOf("can_sleep").forGetter(BedRule::canSleep), (App)Rule.CODEC.fieldOf("can_set_spawn").forGetter(BedRule::canSetSpawn), (App)Codec.BOOL.optionalFieldOf("explodes", (Object)false).forGetter(BedRule::explodes), (App)ComponentSerialization.CODEC.optionalFieldOf("error_message").forGetter(BedRule::errorMessage)).apply((Applicative)i, BedRule::new));

    public boolean canSleep(Level level) {
        return this.canSleep.test(level);
    }

    public boolean canSetSpawn(Level level) {
        return this.canSetSpawn.test(level);
    }

    public Player.BedSleepingProblem asProblem() {
        return new Player.BedSleepingProblem(this.errorMessage.orElse(null));
    }

    public static enum Rule implements StringRepresentable
    {
        ALWAYS("always"),
        WHEN_DARK("when_dark"),
        NEVER("never");

        public static final Codec<Rule> CODEC;
        private final String name;

        private Rule(String name) {
            this.name = name;
        }

        public boolean test(Level level) {
            return switch (this.ordinal()) {
                default -> throw new MatchException(null, null);
                case 0 -> true;
                case 1 -> level.isDarkOutside();
                case 2 -> false;
            };
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Rule::values);
        }
    }
}

