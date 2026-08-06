/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 */
package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class GateBehavior<E extends LivingEntity>
implements BehaviorControl<E> {
    private final Map<MemoryModuleType<?>, MemoryStatus> entryCondition;
    private final Set<MemoryModuleType<?>> exitErasedMemories;
    private final OrderPolicy orderPolicy;
    private final RunningPolicy runningPolicy;
    private final ShufflingList<BehaviorControl<? super E>> behaviors = new ShufflingList();
    private Behavior.Status status = Behavior.Status.STOPPED;

    public GateBehavior(Map<MemoryModuleType<?>, MemoryStatus> entryCondition, Set<MemoryModuleType<?>> exitErasedMemories, OrderPolicy orderPolicy, RunningPolicy runningPolicy, List<Pair<? extends BehaviorControl<? super E>, Integer>> behaviors) {
        this.entryCondition = entryCondition;
        this.exitErasedMemories = exitErasedMemories;
        this.orderPolicy = orderPolicy;
        this.runningPolicy = runningPolicy;
        behaviors.forEach(entry -> this.behaviors.add((BehaviorControl)entry.getFirst(), (Integer)entry.getSecond()));
    }

    @Override
    public Behavior.Status getStatus() {
        return this.status;
    }

    @Override
    public Set<MemoryModuleType<?>> getRequiredMemories() {
        HashSet memories = new HashSet(this.entryCondition.keySet());
        for (BehaviorControl<E> behavior : this.behaviors) {
            memories.addAll(behavior.getRequiredMemories());
        }
        return memories;
    }

    private boolean hasRequiredMemories(E body) {
        for (Map.Entry<MemoryModuleType<?>, MemoryStatus> entry : this.entryCondition.entrySet()) {
            MemoryModuleType<?> memoryType = entry.getKey();
            MemoryStatus requiredStatus = entry.getValue();
            if (((LivingEntity)body).getBrain().checkMemory(memoryType, requiredStatus)) continue;
            return false;
        }
        return true;
    }

    @Override
    public final boolean tryStart(ServerLevel level, E body, long timestamp) {
        if (this.hasRequiredMemories(body)) {
            this.status = Behavior.Status.RUNNING;
            this.orderPolicy.apply(this.behaviors);
            this.runningPolicy.apply(this.behaviors.stream(), level, body, timestamp);
            return true;
        }
        return false;
    }

    @Override
    public final void tickOrStop(ServerLevel level, E body, long timestamp) {
        this.behaviors.stream().filter(goal -> goal.getStatus() == Behavior.Status.RUNNING).forEach(goal -> goal.tickOrStop(level, body, timestamp));
        if (this.behaviors.stream().noneMatch(g -> g.getStatus() == Behavior.Status.RUNNING)) {
            this.doStop(level, body, timestamp);
        }
    }

    @Override
    public final void doStop(ServerLevel level, E body, long timestamp) {
        this.status = Behavior.Status.STOPPED;
        this.behaviors.stream().filter(goal -> goal.getStatus() == Behavior.Status.RUNNING).forEach(goal -> goal.doStop(level, body, timestamp));
        this.exitErasedMemories.forEach(((LivingEntity)body).getBrain()::eraseMemory);
    }

    @Override
    public String debugString() {
        Set runningBehaviours = this.behaviors.stream().filter(goal -> goal.getStatus() == Behavior.Status.RUNNING).map(b -> b.getClass().getSimpleName()).collect(Collectors.toSet());
        return this.getClass().getSimpleName() + ": " + String.valueOf(runningBehaviours);
    }

    public static enum OrderPolicy {
        ORDERED(t -> {}),
        SHUFFLED(ShufflingList::shuffle);

        private final Consumer<ShufflingList<?>> consumer;

        private OrderPolicy(Consumer<ShufflingList<?>> consumer) {
            this.consumer = consumer;
        }

        public void apply(ShufflingList<?> list) {
            this.consumer.accept(list);
        }
    }

    public static enum RunningPolicy {
        RUN_ONE{

            @Override
            public <E extends LivingEntity> void apply(Stream<BehaviorControl<? super E>> behaviors, ServerLevel level, E body, long timestamp) {
                behaviors.filter(goal -> goal.getStatus() == Behavior.Status.STOPPED).filter(goal -> goal.tryStart(level, body, timestamp)).findFirst();
            }
        }
        ,
        TRY_ALL{

            @Override
            public <E extends LivingEntity> void apply(Stream<BehaviorControl<? super E>> behaviors, ServerLevel level, E body, long timestamp) {
                behaviors.filter(goal -> goal.getStatus() == Behavior.Status.STOPPED).forEach(goal -> goal.tryStart(level, body, timestamp));
            }
        };


        public abstract <E extends LivingEntity> void apply(Stream<BehaviorControl<? super E>> var1, ServerLevel var2, E var3, long var4);
    }
}

