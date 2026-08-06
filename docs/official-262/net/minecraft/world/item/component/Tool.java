/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public record Tool(List<Rule> rules, float defaultMiningSpeed, int damagePerBlock, boolean canDestroyBlocksInCreative) {
    public static final Codec<Tool> CODEC = RecordCodecBuilder.create(i -> i.group((App)Rule.CODEC.listOf().fieldOf("rules").forGetter(Tool::rules), (App)Codec.FLOAT.optionalFieldOf("default_mining_speed", (Object)Float.valueOf(1.0f)).forGetter(Tool::defaultMiningSpeed), (App)ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("damage_per_block", (Object)1).forGetter(Tool::damagePerBlock), (App)Codec.BOOL.optionalFieldOf("can_destroy_blocks_in_creative", (Object)true).forGetter(Tool::canDestroyBlocksInCreative)).apply((Applicative)i, Tool::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Tool> STREAM_CODEC = StreamCodec.composite(Rule.STREAM_CODEC.apply(ByteBufCodecs.list()), Tool::rules, ByteBufCodecs.FLOAT, Tool::defaultMiningSpeed, ByteBufCodecs.VAR_INT, Tool::damagePerBlock, ByteBufCodecs.BOOL, Tool::canDestroyBlocksInCreative, Tool::new);

    public float getMiningSpeed(BlockState state) {
        for (Rule rule : this.rules) {
            if (!rule.speed.isPresent() || !state.is(rule.blocks)) continue;
            return rule.speed.get().floatValue();
        }
        return this.defaultMiningSpeed;
    }

    public boolean isCorrectForDrops(BlockState state) {
        for (Rule rule : this.rules) {
            if (!rule.correctForDrops.isPresent() || !state.is(rule.blocks)) continue;
            return rule.correctForDrops.get();
        }
        return false;
    }

    public record Rule(HolderSet<Block> blocks, Optional<Float> speed, Optional<Boolean> correctForDrops) {
        public static final Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group((App)RegistryCodecs.homogeneousList(Registries.BLOCK).fieldOf("blocks").forGetter(Rule::blocks), (App)ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("speed").forGetter(Rule::speed), (App)Codec.BOOL.optionalFieldOf("correct_for_drops").forGetter(Rule::correctForDrops)).apply((Applicative)i, Rule::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Rule> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.holderSet(Registries.BLOCK), Rule::blocks, ByteBufCodecs.FLOAT.apply(ByteBufCodecs::optional), Rule::speed, ByteBufCodecs.BOOL.apply(ByteBufCodecs::optional), Rule::correctForDrops, Rule::new);

        public static Rule minesAndDrops(HolderSet<Block> blocks, float speed) {
            return new Rule(blocks, Optional.of(Float.valueOf(speed)), Optional.of(true));
        }

        public static Rule deniesDrops(HolderSet<Block> blocks) {
            return new Rule(blocks, Optional.empty(), Optional.of(false));
        }

        public static Rule overrideSpeed(HolderSet<Block> blocks, float speed) {
            return new Rule(blocks, Optional.of(Float.valueOf(speed)), Optional.empty());
        }
    }
}

