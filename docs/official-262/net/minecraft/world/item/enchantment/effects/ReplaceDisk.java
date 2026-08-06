/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.item.enchantment.effects;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.phys.Vec3;

public record ReplaceDisk(LevelBasedValue radius, LevelBasedValue height, Vec3i offset, Optional<BlockPredicate> predicate, BlockStateProvider blockState, Optional<Holder<GameEvent>> triggerGameEvent) implements EnchantmentEntityEffect
{
    public static final MapCodec<ReplaceDisk> CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)LevelBasedValue.CODEC.fieldOf("radius").forGetter(ReplaceDisk::radius), (App)LevelBasedValue.CODEC.fieldOf("height").forGetter(ReplaceDisk::height), (App)Vec3i.CODEC.optionalFieldOf("offset", (Object)Vec3i.ZERO).forGetter(ReplaceDisk::offset), (App)BlockPredicate.CODEC.optionalFieldOf("predicate").forGetter(ReplaceDisk::predicate), (App)BlockStateProvider.CODEC.fieldOf("block_state").forGetter(ReplaceDisk::blockState), (App)GameEvent.CODEC.optionalFieldOf("trigger_game_event").forGetter(ReplaceDisk::triggerGameEvent)).apply((Applicative)i, ReplaceDisk::new));

    @Override
    public void apply(ServerLevel serverLevel, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 position) {
        BlockPos centerBlock = BlockPos.containing(position).offset(this.offset);
        RandomSource random = entity.getRandom();
        int dist = (int)this.radius.calculate(enchantmentLevel);
        int height = (int)this.height.calculate(enchantmentLevel);
        for (BlockPos pos : BlockPos.betweenClosed(centerBlock.offset(-dist, 0, -dist), centerBlock.offset(dist, Math.min(height - 1, 0), dist))) {
            if (!(pos.distToCenterSqr(position.x(), (double)pos.getY() + 0.5, position.z()) < (double)Mth.square(dist)) || !this.predicate.map(p -> p.test(serverLevel, pos)).orElse(true).booleanValue() || !serverLevel.setBlockAndUpdate(pos, this.blockState.getState(serverLevel, random, pos))) continue;
            this.triggerGameEvent.ifPresent(event -> serverLevel.gameEvent(entity, (Holder<GameEvent>)event, pos));
        }
    }

    public MapCodec<ReplaceDisk> codec() {
        return CODEC;
    }
}

