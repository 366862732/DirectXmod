/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.kinds.Applicative$Mu
 *  com.mojang.datafixers.kinds.Const$Mu
 *  com.mojang.datafixers.kinds.IdF
 *  com.mojang.datafixers.kinds.IdF$Mu
 *  com.mojang.datafixers.kinds.K1
 *  com.mojang.datafixers.kinds.OptionalBox
 *  com.mojang.datafixers.kinds.OptionalBox$Mu
 *  com.mojang.datafixers.util.Function3
 *  com.mojang.datafixers.util.Function4
 *  com.mojang.datafixers.util.Unit
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Unit;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryCondition;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jspecify.annotations.Nullable;

public class BehaviorBuilder<E extends LivingEntity, M>
implements App<Mu<E>, M> {
    private final TriggerWithResult<E, M> trigger;

    public static <E extends LivingEntity, M> BehaviorBuilder<E, M> unbox(App<Mu<E>, M> box) {
        return (BehaviorBuilder)box;
    }

    public static <E extends LivingEntity> Instance<E> instance() {
        return new Instance();
    }

    public static <E extends LivingEntity> OneShot<E> create(Function<Instance<E>, ? extends App<Mu<E>, Trigger<E>>> builder) {
        final TriggerWithResult<E, Trigger<E>> resolvedBuilder = BehaviorBuilder.get(builder.apply(BehaviorBuilder.instance()));
        return new OneShot<E>(){

            @Override
            public boolean trigger(ServerLevel level, E body, long timestamp) {
                Trigger trigger = (Trigger)resolvedBuilder.tryTrigger(level, body, timestamp);
                if (trigger == null) {
                    return false;
                }
                return trigger.trigger(level, body, timestamp);
            }

            @Override
            public Set<MemoryModuleType<?>> getRequiredMemories() {
                return resolvedBuilder.memories();
            }

            @Override
            public String debugString() {
                return "OneShot[" + resolvedBuilder.debugString() + "]";
            }

            public String toString() {
                return this.debugString();
            }
        };
    }

    public static <E extends LivingEntity> OneShot<E> sequence(Trigger<? super E> first, final OneShot<? super E> second) {
        final OneShot<E> wrapped = BehaviorBuilder.create((Instance<E> i) -> i.group(i.ifTriggered(first)).apply((Applicative)i, unit -> second::trigger));
        return new OneShot<E>(){

            @Override
            public boolean trigger(ServerLevel level, E body, long timestamp) {
                return wrapped.trigger(level, body, timestamp);
            }

            @Override
            public Set<MemoryModuleType<?>> getRequiredMemories() {
                HashSet memories = new HashSet();
                memories.addAll(wrapped.getRequiredMemories());
                memories.addAll(second.getRequiredMemories());
                return memories;
            }

            @Override
            public String debugString() {
                return "OneShot[stuff]";
            }

            public String toString() {
                return this.debugString();
            }
        };
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> predicate, OneShot<? super E> behavior) {
        return BehaviorBuilder.sequence(BehaviorBuilder.triggerIf(predicate), behavior);
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> predicate) {
        return BehaviorBuilder.create((Instance<E> i) -> i.point((level, body, timestamp) -> predicate.test(body)));
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(BiPredicate<ServerLevel, E> predicate) {
        return BehaviorBuilder.create((Instance<E> i) -> i.point((level, body, timestamp) -> predicate.test(level, body)));
    }

    private static <E extends LivingEntity, M> TriggerWithResult<E, M> get(App<Mu<E>, M> box) {
        return BehaviorBuilder.unbox(box).trigger;
    }

    private BehaviorBuilder(TriggerWithResult<E, M> trigger) {
        this.trigger = trigger;
    }

    private static <E extends LivingEntity, M> BehaviorBuilder<E, M> create(TriggerWithResult<E, M> instanceFactory) {
        return new BehaviorBuilder<E, M>(instanceFactory);
    }

    public static final class Instance<E extends LivingEntity>
    implements Applicative<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, Mu<E>> {
        public <Value> Optional<Value> tryGet(MemoryAccessor<OptionalBox.Mu, Value> box) {
            return OptionalBox.unbox(box.value());
        }

        public <Value> Value get(MemoryAccessor<IdF.Mu, Value> box) {
            return (Value)IdF.get(box.value());
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<OptionalBox.Mu, Value>> registered(MemoryModuleType<Value> memory) {
            return new PureMemory(new MemoryCondition.Registered<Value>(memory));
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<IdF.Mu, Value>> present(MemoryModuleType<Value> memory) {
            return new PureMemory(new MemoryCondition.Present<Value>(memory));
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<Const.Mu<Unit>, Value>> absent(MemoryModuleType<Value> memory) {
            return new PureMemory(new MemoryCondition.Absent<Value>(memory));
        }

        public BehaviorBuilder<E, Unit> ifTriggered(Trigger<? super E> dependentTrigger) {
            return new TriggerWrapper<E>(dependentTrigger);
        }

        public <A> BehaviorBuilder<E, A> point(A a) {
            return new Constant(a);
        }

        public <A> BehaviorBuilder<E, A> point(Supplier<String> debugString, A a) {
            return new Constant(a, debugString);
        }

        public <A, R> Function<App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, A>, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, R>> lift1(App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, Function<A, R>> function) {
            return a -> {
                final TriggerWithResult aTrigger = BehaviorBuilder.get(a);
                final TriggerWithResult fTrigger = BehaviorBuilder.get(function);
                return BehaviorBuilder.create(new TriggerWithResult<E, R>(this){
                    {
                        Objects.requireNonNull(this$0);
                    }

                    @Override
                    public R tryTrigger(ServerLevel level, E body, long timestamp) {
                        Object ra = aTrigger.tryTrigger(level, body, timestamp);
                        if (ra == null) {
                            return null;
                        }
                        Function rf = (Function)fTrigger.tryTrigger(level, body, timestamp);
                        if (rf == null) {
                            return null;
                        }
                        return rf.apply(ra);
                    }

                    @Override
                    public Set<MemoryModuleType<?>> memories() {
                        HashSet memories = new HashSet();
                        memories.addAll(aTrigger.memories());
                        memories.addAll(fTrigger.memories());
                        return memories;
                    }

                    @Override
                    public String debugString() {
                        return fTrigger.debugString() + " * " + aTrigger.debugString();
                    }

                    public String toString() {
                        return this.debugString();
                    }
                });
            };
        }

        public <T, R> BehaviorBuilder<E, R> map(final Function<? super T, ? extends R> func, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, T> ts) {
            final TriggerWithResult<E, T> tTrigger = BehaviorBuilder.get(ts);
            return BehaviorBuilder.create(new TriggerWithResult<E, R>(this){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public R tryTrigger(ServerLevel level, E body, long timestamp) {
                    Object t = tTrigger.tryTrigger(level, body, timestamp);
                    if (t == null) {
                        return null;
                    }
                    return func.apply(t);
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    return tTrigger.memories();
                }

                @Override
                public String debugString() {
                    return tTrigger.debugString() + ".map[" + String.valueOf(func) + "]";
                }

                public String toString() {
                    return this.debugString();
                }
            });
        }

        public <A, B, R> BehaviorBuilder<E, R> ap2(App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, BiFunction<A, B, R>> func, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, A> a, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, B> b) {
            final TriggerWithResult<E, A> aTrigger = BehaviorBuilder.get(a);
            final TriggerWithResult<E, B> bTrigger = BehaviorBuilder.get(b);
            final TriggerWithResult<E, BiFunction<A, B, R>> fTrigger = BehaviorBuilder.get(func);
            return BehaviorBuilder.create(new TriggerWithResult<E, R>(this){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public R tryTrigger(ServerLevel level, E body, long timestamp) {
                    Object ra = aTrigger.tryTrigger(level, body, timestamp);
                    if (ra == null) {
                        return null;
                    }
                    Object rb = bTrigger.tryTrigger(level, body, timestamp);
                    if (rb == null) {
                        return null;
                    }
                    BiFunction fr = (BiFunction)fTrigger.tryTrigger(level, body, timestamp);
                    if (fr == null) {
                        return null;
                    }
                    return fr.apply(ra, rb);
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    HashSet memories = new HashSet();
                    memories.addAll(aTrigger.memories());
                    memories.addAll(bTrigger.memories());
                    memories.addAll(fTrigger.memories());
                    return memories;
                }

                @Override
                public String debugString() {
                    return fTrigger.debugString() + " * " + aTrigger.debugString() + " * " + bTrigger.debugString();
                }

                public String toString() {
                    return this.debugString();
                }
            });
        }

        public <T1, T2, T3, R> BehaviorBuilder<E, R> ap3(App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, Function3<T1, T2, T3, R>> func, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, T1> t1, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, T2> t2, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, T3> t3) {
            final TriggerWithResult<E, T1> t1Trigger = BehaviorBuilder.get(t1);
            final TriggerWithResult<E, T2> t2Trigger = BehaviorBuilder.get(t2);
            final TriggerWithResult<E, T3> t3Trigger = BehaviorBuilder.get(t3);
            final TriggerWithResult<E, Function3<T1, T2, T3, R>> fTrigger = BehaviorBuilder.get(func);
            return BehaviorBuilder.create(new TriggerWithResult<E, R>(this){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public R tryTrigger(ServerLevel level, E body, long timestamp) {
                    Object r1 = t1Trigger.tryTrigger(level, body, timestamp);
                    if (r1 == null) {
                        return null;
                    }
                    Object r2 = t2Trigger.tryTrigger(level, body, timestamp);
                    if (r2 == null) {
                        return null;
                    }
                    Object r3 = t3Trigger.tryTrigger(level, body, timestamp);
                    if (r3 == null) {
                        return null;
                    }
                    Function3 rf = (Function3)fTrigger.tryTrigger(level, body, timestamp);
                    if (rf == null) {
                        return null;
                    }
                    return rf.apply(r1, r2, r3);
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    HashSet memories = new HashSet();
                    memories.addAll(t1Trigger.memories());
                    memories.addAll(t2Trigger.memories());
                    memories.addAll(t3Trigger.memories());
                    memories.addAll(fTrigger.memories());
                    return memories;
                }

                @Override
                public String debugString() {
                    return fTrigger.debugString() + " * " + t1Trigger.debugString() + " * " + t2Trigger.debugString() + " * " + t3Trigger.debugString();
                }

                public String toString() {
                    return this.debugString();
                }
            });
        }

        public <T1, T2, T3, T4, R> BehaviorBuilder<E, R> ap4(App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, Function4<T1, T2, T3, T4, R>> func, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, T1> t1, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, T2> t2, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, T3> t3, App<net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder$Mu<E>, T4> t4) {
            final TriggerWithResult<E, T1> t1Trigger = BehaviorBuilder.get(t1);
            final TriggerWithResult<E, T2> t2Trigger = BehaviorBuilder.get(t2);
            final TriggerWithResult<E, T3> t3Trigger = BehaviorBuilder.get(t3);
            final TriggerWithResult<E, T4> t4Trigger = BehaviorBuilder.get(t4);
            final TriggerWithResult<E, Function4<T1, T2, T3, T4, R>> fTrigger = BehaviorBuilder.get(func);
            return BehaviorBuilder.create(new TriggerWithResult<E, R>(this){
                {
                    Objects.requireNonNull(this$0);
                }

                @Override
                public R tryTrigger(ServerLevel level, E body, long timestamp) {
                    Object r1 = t1Trigger.tryTrigger(level, body, timestamp);
                    if (r1 == null) {
                        return null;
                    }
                    Object r2 = t2Trigger.tryTrigger(level, body, timestamp);
                    if (r2 == null) {
                        return null;
                    }
                    Object r3 = t3Trigger.tryTrigger(level, body, timestamp);
                    if (r3 == null) {
                        return null;
                    }
                    Object r4 = t4Trigger.tryTrigger(level, body, timestamp);
                    if (r4 == null) {
                        return null;
                    }
                    Function4 rf = (Function4)fTrigger.tryTrigger(level, body, timestamp);
                    if (rf == null) {
                        return null;
                    }
                    return rf.apply(r1, r2, r3, r4);
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    HashSet memories = new HashSet();
                    memories.addAll(t1Trigger.memories());
                    memories.addAll(t2Trigger.memories());
                    memories.addAll(t3Trigger.memories());
                    memories.addAll(t4Trigger.memories());
                    memories.addAll(fTrigger.memories());
                    return memories;
                }

                @Override
                public String debugString() {
                    return fTrigger.debugString() + " * " + t1Trigger.debugString() + " * " + t2Trigger.debugString() + " * " + t3Trigger.debugString() + " * " + t4Trigger.debugString();
                }

                public String toString() {
                    return this.debugString();
                }
            });
        }

        private static final class Mu<E extends LivingEntity>
        implements Applicative.Mu {
            private Mu() {
            }
        }
    }

    private static interface TriggerWithResult<E extends LivingEntity, R> {
        public @Nullable R tryTrigger(ServerLevel var1, E var2, long var3);

        public Set<MemoryModuleType<?>> memories();

        public String debugString();
    }

    private static final class TriggerWrapper<E extends LivingEntity>
    extends BehaviorBuilder<E, Unit> {
        private TriggerWrapper(final Trigger<? super E> dependentTrigger) {
            super(new TriggerWithResult<E, Unit>(){

                @Override
                public @Nullable Unit tryTrigger(ServerLevel level, E body, long timestamp) {
                    return dependentTrigger.trigger(level, body, timestamp) ? Unit.INSTANCE : null;
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    return Set.of();
                }

                @Override
                public String debugString() {
                    return "T[" + String.valueOf(dependentTrigger) + "]";
                }
            });
        }
    }

    private static final class Constant<E extends LivingEntity, A>
    extends BehaviorBuilder<E, A> {
        private Constant(A a) {
            this(a, () -> "C[" + String.valueOf(a) + "]");
        }

        private Constant(final A a, final Supplier<String> debugString) {
            super(new TriggerWithResult<E, A>(){

                @Override
                public A tryTrigger(ServerLevel level, E body, long timestamp) {
                    return a;
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    return Set.of();
                }

                @Override
                public String debugString() {
                    return (String)debugString.get();
                }

                public String toString() {
                    return this.debugString();
                }
            });
        }
    }

    private static final class PureMemory<E extends LivingEntity, F extends K1, Value>
    extends BehaviorBuilder<E, MemoryAccessor<F, Value>> {
        private PureMemory(final MemoryCondition<F, Value> condition) {
            super(new TriggerWithResult<E, MemoryAccessor<F, Value>>(){

                @Override
                public @Nullable MemoryAccessor<F, Value> tryTrigger(ServerLevel level, E body, long timestamp) {
                    Brain<? extends LivingEntity> brain = ((LivingEntity)body).getBrain();
                    Optional value = brain.getMemoryInternal(condition.memory());
                    if (value == null) {
                        return null;
                    }
                    return condition.createAccessor(brain, value);
                }

                @Override
                public Set<MemoryModuleType<?>> memories() {
                    return Set.of(condition.memory());
                }

                @Override
                public String debugString() {
                    return "M[" + String.valueOf(condition) + "]";
                }

                public String toString() {
                    return this.debugString();
                }
            });
        }
    }

    public static final class Mu<E extends LivingEntity>
    implements K1 {
    }
}

