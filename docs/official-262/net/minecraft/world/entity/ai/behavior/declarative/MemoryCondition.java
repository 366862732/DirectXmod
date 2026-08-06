/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.Const
 *  com.mojang.datafixers.kinds.Const$Mu
 *  com.mojang.datafixers.kinds.IdF
 *  com.mojang.datafixers.kinds.IdF$Mu
 *  com.mojang.datafixers.kinds.K1
 *  com.mojang.datafixers.kinds.OptionalBox
 *  com.mojang.datafixers.kinds.OptionalBox$Mu
 *  com.mojang.datafixers.util.Unit
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Unit;
import java.util.Optional;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jspecify.annotations.Nullable;

public interface MemoryCondition<F extends K1, Value> {
    public MemoryModuleType<Value> memory();

    public MemoryStatus condition();

    public @Nullable MemoryAccessor<F, Value> createAccessor(Brain<?> var1, Optional<Value> var2);

    public record Absent<Value>(MemoryModuleType<Value> memory) implements MemoryCondition<Const.Mu<Unit>, Value>
    {
        @Override
        public MemoryStatus condition() {
            return MemoryStatus.VALUE_ABSENT;
        }

        @Override
        public MemoryAccessor<Const.Mu<Unit>, Value> createAccessor(Brain<?> brain, Optional<Value> value) {
            if (value.isPresent()) {
                return null;
            }
            return new MemoryAccessor(brain, this.memory, Const.create((Object)Unit.INSTANCE));
        }
    }

    public record Present<Value>(MemoryModuleType<Value> memory) implements MemoryCondition<IdF.Mu, Value>
    {
        @Override
        public MemoryStatus condition() {
            return MemoryStatus.VALUE_PRESENT;
        }

        @Override
        public MemoryAccessor<IdF.Mu, Value> createAccessor(Brain<?> brain, Optional<Value> value) {
            if (value.isEmpty()) {
                return null;
            }
            return new MemoryAccessor(brain, this.memory, IdF.create(value.get()));
        }
    }

    public record Registered<Value>(MemoryModuleType<Value> memory) implements MemoryCondition<OptionalBox.Mu, Value>
    {
        @Override
        public MemoryStatus condition() {
            return MemoryStatus.REGISTERED;
        }

        @Override
        public MemoryAccessor<OptionalBox.Mu, Value> createAccessor(Brain<?> brain, Optional<Value> value) {
            return new MemoryAccessor(brain, this.memory, OptionalBox.create(value));
        }
    }
}

