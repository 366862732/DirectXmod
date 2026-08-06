/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.Maps
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryMap;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemorySlot;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class Brain<E extends LivingEntity> {
    private static final int SCHEDULE_UPDATE_DELAY = 20;
    private final Map<MemoryModuleType<?>, MemorySlot<?>> memories = Maps.newHashMap();
    private final Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> sensors = Maps.newLinkedHashMap();
    private final Map<Integer, Map<Activity, Set<BehaviorControl<? super E>>>> availableBehaviorsByPriority = Maps.newTreeMap();
    private @Nullable EnvironmentAttribute<Activity> schedule;
    private final Map<Activity, Set<Pair<MemoryModuleType<?>, MemoryStatus>>> activityRequirements = Maps.newHashMap();
    private final Map<Activity, Set<MemoryModuleType<?>>> activityMemoriesToEraseWhenStopped = Maps.newHashMap();
    private Set<Activity> coreActivities = Sets.newHashSet();
    private final Set<Activity> activeActivities = Sets.newHashSet();
    private Activity defaultActivity = Activity.IDLE;
    private long lastScheduleUpdate = -9999L;

    public static <E extends LivingEntity> Provider<E> provider(Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes) {
        return new Provider<LivingEntity>((Collection<MemoryModuleType<?>>)ImmutableList.of(), (Collection<SensorType<Sensor<LivingEntity>>>)sensorTypes, livingEntity -> List.of());
    }

    public static <E extends LivingEntity> Provider<E> provider(Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes, ActivitySupplier<E> activities) {
        return new Provider<E>((Collection<MemoryModuleType<?>>)ImmutableList.of(), sensorTypes, activities);
    }

    @Deprecated
    public static <E extends LivingEntity> Provider<E> provider(Collection<? extends MemoryModuleType<?>> memoryTypes, Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes, ActivitySupplier<E> activities) {
        return new Provider<E>(memoryTypes, sensorTypes, activities);
    }

    @VisibleForTesting
    protected Brain(Collection<? extends MemoryModuleType<?>> memoryTypes, Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes, List<ActivityData<E>> activities, MemoryMap memories, RandomSource randomSource) {
        for (MemoryModuleType<?> memoryModuleType : memoryTypes) {
            this.registerMemory(memoryModuleType);
        }
        for (SensorType sensorType : sensorTypes) {
            Object newSensor = sensorType.create();
            ((Sensor)newSensor).randomlyDelayStart(randomSource);
            this.sensors.put(sensorType, (Sensor<E>)newSensor);
            for (MemoryModuleType<?> type : ((Sensor)newSensor).requires()) {
                this.registerMemory(type);
            }
        }
        for (ActivityData activityData : activities) {
            this.addActivity(activityData.activityType(), activityData.behaviorPriorityPairs(), activityData.conditions(), activityData.memoriesToEraseWhenStopped());
        }
        for (MemoryMap.Value value : memories) {
            this.setMemoryInternal(value);
        }
        this.setCoreActivities((Set<Activity>)ImmutableSet.of((Object)Activity.CORE));
        this.useDefaultActivity();
    }

    private void registerMemory(MemoryModuleType<?> memoryType) {
        this.memories.putIfAbsent(memoryType, MemorySlot.create());
    }

    public Brain() {
        this.setCoreActivities((Set<Activity>)ImmutableSet.of((Object)Activity.CORE));
        this.useDefaultActivity();
    }

    public Packed pack() {
        final MemoryMap.Builder builder = new MemoryMap.Builder();
        this.forEach(new Visitor(){
            {
                Objects.requireNonNull(this$0);
            }

            @Override
            public <U> void acceptEmpty(MemoryModuleType<U> type) {
            }

            @Override
            public <U> void accept(MemoryModuleType<U> type, U value, long timeToLive) {
                if (type.canSerialize()) {
                    builder.add(type, ExpirableValue.of(value, timeToLive));
                }
            }

            @Override
            public <U> void accept(MemoryModuleType<U> type, U value) {
                if (type.canSerialize()) {
                    builder.add(type, ExpirableValue.of(value));
                }
            }
        });
        return new Packed(builder.build());
    }

    private <T> @Nullable MemorySlot<T> getMemorySlotIfPresent(MemoryModuleType<T> memoryType) {
        return this.memories.get(memoryType);
    }

    private <T> MemorySlot<T> getMemorySlot(MemoryModuleType<T> memoryType) {
        MemorySlot<T> result = this.getMemorySlotIfPresent(memoryType);
        if (result == null) {
            throw new IllegalStateException("Unregistered memory fetched: " + String.valueOf(memoryType));
        }
        return result;
    }

    public boolean hasMemoryValue(MemoryModuleType<?> type) {
        return this.checkMemory(type, MemoryStatus.VALUE_PRESENT);
    }

    public void clearMemories() {
        this.memories.values().forEach(MemorySlot::clear);
    }

    public <U> void eraseMemory(MemoryModuleType<U> type) {
        MemorySlot<U> slot = this.getMemorySlotIfPresent(type);
        if (slot != null) {
            slot.clear();
        }
    }

    public <U> void setMemory(MemoryModuleType<U> type, @Nullable U value) {
        this.setMemoryInternal(type, value);
    }

    public <U> void setMemoryWithExpiry(MemoryModuleType<U> type, U value, long timeToLive) {
        this.setMemoryInternal(type, value, timeToLive);
    }

    public <U> void setMemory(MemoryModuleType<U> type, Optional<? extends U> optionalValue) {
        this.setMemoryInternal(type, optionalValue.orElse(null));
    }

    private <U> void setMemoryInternal(MemoryMap.Value<U> value) {
        ExpirableValue<U> expirableValue = value.value();
        if (expirableValue.timeToLive().isPresent()) {
            this.setMemoryInternal(value.type(), expirableValue.value(), expirableValue.timeToLive().get());
        } else {
            this.setMemoryInternal(value.type(), expirableValue.value());
        }
    }

    private <U> void setMemoryInternal(MemoryModuleType<U> type, U value, long tileToLive) {
        MemorySlot<U> slot = this.getMemorySlotIfPresent(type);
        if (slot != null) {
            if (Brain.isEmptyCollection(value)) {
                value = null;
            }
            if (value == null) {
                slot.clear();
            } else {
                slot.set(value, tileToLive);
            }
        }
    }

    private <U> void setMemoryInternal(MemoryModuleType<U> type, @Nullable U value) {
        MemorySlot<U> slot = this.getMemorySlotIfPresent(type);
        if (slot != null) {
            if (value != null && Brain.isEmptyCollection(value)) {
                value = null;
            }
            if (value == null) {
                slot.clear();
            } else {
                slot.set(value);
            }
        }
    }

    public <U> Optional<U> getMemory(MemoryModuleType<U> type) {
        return Optional.ofNullable(this.getMemorySlot(type).value());
    }

    public <U> @Nullable Optional<U> getMemoryInternal(MemoryModuleType<U> type) {
        MemorySlot<U> slot = this.getMemorySlotIfPresent(type);
        if (slot == null) {
            return null;
        }
        return Optional.ofNullable(slot.value());
    }

    public <U> long getTimeUntilExpiry(MemoryModuleType<U> type) {
        return this.getMemorySlot(type).timeToLive();
    }

    public void forEach(Visitor visitor) {
        this.memories.forEach((? super K memoryModuleType, ? super V slot) -> Brain.callVisitor(visitor, memoryModuleType, slot));
    }

    private static <U> void callVisitor(Visitor visitor, MemoryModuleType<U> memoryModuleType, MemorySlot<?> slot) {
        slot.visit(memoryModuleType, visitor);
    }

    public <U> boolean isMemoryValue(MemoryModuleType<U> memoryType, U value) {
        MemorySlot<U> slot = this.getMemorySlotIfPresent(memoryType);
        return slot != null && Objects.equals(value, slot.value());
    }

    public boolean checkMemory(MemoryModuleType<?> type, MemoryStatus status) {
        MemorySlot<?> slot = this.getMemorySlotIfPresent(type);
        if (slot == null) {
            return false;
        }
        return status == MemoryStatus.REGISTERED || status == MemoryStatus.VALUE_PRESENT && slot.hasValue() || status == MemoryStatus.VALUE_ABSENT && !slot.hasValue();
    }

    public void setSchedule(EnvironmentAttribute<Activity> schedule) {
        this.schedule = schedule;
    }

    public void setCoreActivities(Set<Activity> activities) {
        this.coreActivities = activities;
    }

    @Deprecated
    @VisibleForDebug
    public Set<Activity> getActiveActivities() {
        return this.activeActivities;
    }

    @Deprecated
    @VisibleForDebug
    public List<BehaviorControl<? super E>> getRunningBehaviors() {
        ObjectArrayList runningBehaviours = new ObjectArrayList();
        for (Map<Activity, Set<BehaviorControl<E>>> behavioursByActivities : this.availableBehaviorsByPriority.values()) {
            for (Set<BehaviorControl<E>> behaviors : behavioursByActivities.values()) {
                for (BehaviorControl<E> behavior : behaviors) {
                    if (behavior.getStatus() != Behavior.Status.RUNNING) continue;
                    runningBehaviours.add(behavior);
                }
            }
        }
        return runningBehaviours;
    }

    public void useDefaultActivity() {
        this.setActiveActivity(this.defaultActivity);
    }

    public Optional<Activity> getActiveNonCoreActivity() {
        for (Activity activity : this.activeActivities) {
            if (this.coreActivities.contains(activity)) continue;
            return Optional.of(activity);
        }
        return Optional.empty();
    }

    public void setActiveActivityIfPossible(Activity activity) {
        if (this.activityRequirementsAreMet(activity)) {
            this.setActiveActivity(activity);
        } else {
            this.useDefaultActivity();
        }
    }

    private void setActiveActivity(Activity activity) {
        if (this.isActive(activity)) {
            return;
        }
        this.eraseMemoriesForOtherActivitesThan(activity);
        this.activeActivities.clear();
        this.activeActivities.addAll(this.coreActivities);
        this.activeActivities.add(activity);
    }

    private void eraseMemoriesForOtherActivitesThan(Activity activity) {
        for (Activity oldActivity : this.activeActivities) {
            Set<MemoryModuleType<?>> memoryModuleTypes;
            if (oldActivity == activity || (memoryModuleTypes = this.activityMemoriesToEraseWhenStopped.get(oldActivity)) == null) continue;
            for (MemoryModuleType<?> memoryModuleType : memoryModuleTypes) {
                this.eraseMemory(memoryModuleType);
            }
        }
    }

    public void updateActivityFromSchedule(EnvironmentAttributeSystem environmentAttributes, long gameTime, Vec3 pos) {
        if (gameTime - this.lastScheduleUpdate > 20L) {
            Activity scheduledActivity;
            this.lastScheduleUpdate = gameTime;
            Activity activity = scheduledActivity = this.schedule != null ? environmentAttributes.getValue(this.schedule, pos) : Activity.IDLE;
            if (!this.activeActivities.contains(scheduledActivity)) {
                this.setActiveActivityIfPossible(scheduledActivity);
            }
        }
    }

    public void setActiveActivityToFirstValid(List<Activity> activities) {
        for (Activity activity : activities) {
            if (!this.activityRequirementsAreMet(activity)) continue;
            this.setActiveActivity(activity);
            break;
        }
    }

    public void setDefaultActivity(Activity activity) {
        this.defaultActivity = activity;
    }

    public void addActivity(Activity activity, ImmutableList<? extends Pair<Integer, ? extends BehaviorControl<? super E>>> behaviorPriorityPairs, Set<Pair<MemoryModuleType<?>, MemoryStatus>> conditions, Set<MemoryModuleType<?>> memoriesToEraseWhenStopped) {
        this.activityRequirements.put(activity, conditions);
        if (!memoriesToEraseWhenStopped.isEmpty()) {
            this.activityMemoriesToEraseWhenStopped.put(activity, memoriesToEraseWhenStopped);
        }
        for (Pair pair : behaviorPriorityPairs) {
            BehaviorControl behavior = (BehaviorControl)pair.getSecond();
            for (MemoryModuleType<?> requiredMemory : behavior.getRequiredMemories()) {
                this.registerMemory(requiredMemory);
            }
            this.availableBehaviorsByPriority.computeIfAbsent((Integer)pair.getFirst(), key -> Maps.newHashMap()).computeIfAbsent(activity, key -> Sets.newLinkedHashSet()).add(behavior);
        }
    }

    @VisibleForTesting
    public void removeAllBehaviors() {
        this.availableBehaviorsByPriority.clear();
    }

    public boolean isActive(Activity activity) {
        return this.activeActivities.contains(activity);
    }

    public void tick(ServerLevel level, E body) {
        this.forgetOutdatedMemories();
        this.tickSensors(level, body);
        this.startEachNonRunningBehavior(level, body);
        this.tickEachRunningBehavior(level, body);
    }

    private void tickSensors(ServerLevel level, E body) {
        for (Sensor<E> sensor : this.sensors.values()) {
            sensor.tick(level, body);
        }
    }

    private void forgetOutdatedMemories() {
        this.memories.values().forEach(MemorySlot::tick);
    }

    public void stopAll(ServerLevel level, E body) {
        long timestamp = ((Entity)body).level().getGameTime();
        for (BehaviorControl<E> behavior : this.getRunningBehaviors()) {
            behavior.doStop(level, body, timestamp);
        }
    }

    private void startEachNonRunningBehavior(ServerLevel level, E body) {
        long time = level.getGameTime();
        for (Map<Activity, Set<BehaviorControl<E>>> behavioursByActivities : this.availableBehaviorsByPriority.values()) {
            for (Map.Entry<Activity, Set<BehaviorControl<E>>> behavioursForActivity : behavioursByActivities.entrySet()) {
                Activity activity = behavioursForActivity.getKey();
                if (!this.activeActivities.contains(activity)) continue;
                Set<BehaviorControl<E>> behaviors = behavioursForActivity.getValue();
                for (BehaviorControl<E> behavior : behaviors) {
                    if (behavior.getStatus() != Behavior.Status.STOPPED) continue;
                    behavior.tryStart(level, body, time);
                }
            }
        }
    }

    private void tickEachRunningBehavior(ServerLevel level, E body) {
        long timestamp = level.getGameTime();
        for (BehaviorControl<E> behavior : this.getRunningBehaviors()) {
            behavior.tickOrStop(level, body, timestamp);
        }
    }

    private boolean activityRequirementsAreMet(Activity activity) {
        if (!this.activityRequirements.containsKey(activity)) {
            return false;
        }
        for (Pair<MemoryModuleType<?>, MemoryStatus> memoryRequirement : this.activityRequirements.get(activity)) {
            MemoryStatus memoryStatus;
            MemoryModuleType memoryType = (MemoryModuleType)memoryRequirement.getFirst();
            if (this.checkMemory(memoryType, memoryStatus = (MemoryStatus)((Object)memoryRequirement.getSecond()))) continue;
            return false;
        }
        return true;
    }

    private static boolean isEmptyCollection(Object object) {
        Collection collection;
        return object instanceof Collection && (collection = (Collection)object).isEmpty();
    }

    public boolean isBrainDead() {
        return this.memories.isEmpty() && this.sensors.isEmpty() && this.availableBehaviorsByPriority.isEmpty();
    }

    public static final class Provider<E extends LivingEntity> {
        private final Collection<? extends MemoryModuleType<?>> memoryTypes;
        private final Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes;
        private final ActivitySupplier<E> activities;

        private Provider(Collection<? extends MemoryModuleType<?>> memoryTypes, Collection<? extends SensorType<? extends Sensor<? super E>>> sensorTypes, ActivitySupplier<E> activities) {
            this.memoryTypes = memoryTypes;
            this.sensorTypes = sensorTypes;
            this.activities = activities;
        }

        public Brain<E> makeBrain(E body, Packed packed) {
            List<ActivityData<E>> activities = this.activities.createActivities(body);
            return new Brain<E>(this.memoryTypes, this.sensorTypes, activities, packed.memories, ((Entity)body).getRandom());
        }
    }

    @FunctionalInterface
    public static interface ActivitySupplier<E extends LivingEntity> {
        public List<ActivityData<E>> createActivities(E var1);
    }

    public static interface Visitor {
        public <U> void acceptEmpty(MemoryModuleType<U> var1);

        public <U> void accept(MemoryModuleType<U> var1, U var2);

        public <U> void accept(MemoryModuleType<U> var1, U var2, long var3);
    }

    public record Packed(MemoryMap memories) {
        public static final Packed EMPTY = new Packed(MemoryMap.EMPTY);
        public static final Codec<Packed> CODEC = RecordCodecBuilder.create(i -> i.group((App)MemoryMap.CODEC.fieldOf("memories").forGetter(Packed::memories)).apply((Applicative)i, Packed::new));
    }
}

