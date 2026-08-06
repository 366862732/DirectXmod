/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.inventory;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface ContainerLevelAccess {
    public static final ContainerLevelAccess NULL = new ContainerLevelAccess(){

        @Override
        public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> action) {
            return Optional.empty();
        }
    };

    public static ContainerLevelAccess create(final Level level, final BlockPos pos) {
        return new ContainerLevelAccess(){

            @Override
            public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> action) {
                return Optional.of(action.apply(level, pos));
            }
        };
    }

    public <T> Optional<T> evaluate(BiFunction<Level, BlockPos, T> var1);

    default public <T> T evaluate(BiFunction<Level, BlockPos, T> action, T defaultValue) {
        return this.evaluate(action).orElse(defaultValue);
    }

    default public void execute(BiConsumer<Level, BlockPos> action) {
        this.evaluate((level, pos) -> {
            action.accept((Level)level, (BlockPos)pos);
            return Optional.empty();
        });
    }
}

