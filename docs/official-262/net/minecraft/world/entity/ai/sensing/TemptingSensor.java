/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableSet
 */
package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class TemptingSensor
extends Sensor<PathfinderMob> {
    private static final TargetingConditions TEMPT_TARGETING = TargetingConditions.forNonCombat().ignoreLineOfSight();
    private final BiPredicate<PathfinderMob, ItemStack> temptations;

    public TemptingSensor(Predicate<ItemStack> tt) {
        this((PathfinderMob m, ItemStack i) -> tt.test((ItemStack)i));
    }

    public static TemptingSensor forAnimal() {
        return new TemptingSensor((m, i) -> {
            if (m instanceof Animal) {
                Animal animal = (Animal)m;
                return animal.isFood((ItemStack)i);
            }
            return false;
        });
    }

    private TemptingSensor(BiPredicate<PathfinderMob, ItemStack> temptations) {
        this.temptations = temptations;
    }

    @Override
    protected void doTick(ServerLevel level, PathfinderMob body) {
        Brain<? extends LivingEntity> brain = body.getBrain();
        TargetingConditions targeting = TEMPT_TARGETING.copy().range((float)body.getAttributeValue(Attributes.TEMPT_RANGE));
        List players = level.players().stream().filter(EntitySelector.NO_SPECTATORS).filter(player -> targeting.test(level, body, (LivingEntity)player)).filter(p -> this.playerHoldingTemptation(body, (Player)p)).filter(player -> !body.hasPassenger((Entity)player)).sorted(Comparator.comparingDouble(body::distanceToSqr)).collect(Collectors.toList());
        if (!players.isEmpty()) {
            Player player2 = (Player)players.get(0);
            brain.setMemory(MemoryModuleType.TEMPTING_PLAYER, player2);
        } else {
            brain.eraseMemory(MemoryModuleType.TEMPTING_PLAYER);
        }
    }

    private boolean playerHoldingTemptation(PathfinderMob mob, Player player) {
        return this.isTemptation(mob, player.getMainHandItem()) || this.isTemptation(mob, player.getOffhandItem());
    }

    private boolean isTemptation(PathfinderMob mob, ItemStack itemStack) {
        return this.temptations.test(mob, itemStack);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.TEMPTING_PLAYER);
    }
}

