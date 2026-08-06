/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.state.predicate;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class BlockStatePredicate
implements Predicate<BlockState> {
    public static final Predicate<BlockState> ANY = input -> true;
    private final StateDefinition<Block, BlockState> definition;
    private final Map<Property<?>, Predicate<Object>> properties = Maps.newHashMap();

    private BlockStatePredicate(StateDefinition<Block, BlockState> definition) {
        this.definition = definition;
    }

    public static BlockStatePredicate forBlock(Block block) {
        return new BlockStatePredicate(block.getStateDefinition());
    }

    @Override
    public boolean test(@Nullable BlockState input) {
        if (input == null || !input.getBlock().equals(this.definition.getOwner())) {
            return false;
        }
        if (this.properties.isEmpty()) {
            return true;
        }
        for (Map.Entry<Property<?>, Predicate<Object>> entry : this.properties.entrySet()) {
            if (this.applies(input, entry.getKey(), entry.getValue())) continue;
            return false;
        }
        return true;
    }

    protected <T extends Comparable<T>> boolean applies(BlockState input, Property<T> key, Predicate<Object> predicate) {
        T value = input.getValue(key);
        return predicate.test(value);
    }

    public <V extends Comparable<V>> BlockStatePredicate where(Property<V> property, Predicate<Object> predicate) {
        if (!this.definition.getProperties().contains(property)) {
            throw new IllegalArgumentException(String.valueOf(this.definition) + " cannot support property " + String.valueOf(property));
        }
        this.properties.put(property, predicate);
        return this;
    }
}

