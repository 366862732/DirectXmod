/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.block.state.properties;

import java.util.List;
import java.util.Optional;
import net.minecraft.world.level.block.state.properties.Property;

public final class BooleanProperty
extends Property<Boolean> {
    private static final List<Boolean> VALUES = List.of(Boolean.valueOf(true), Boolean.valueOf(false));
    private static final int TRUE_INDEX = 0;
    private static final int FALSE_INDEX = 1;

    private BooleanProperty(String name) {
        super(name, Boolean.class);
    }

    @Override
    public List<Boolean> getPossibleValues() {
        return VALUES;
    }

    public static BooleanProperty create(String name) {
        return new BooleanProperty(name);
    }

    @Override
    public Optional<Boolean> getValue(String name) {
        return switch (name) {
            case "true" -> Optional.of(true);
            case "false" -> Optional.of(false);
            default -> Optional.empty();
        };
    }

    @Override
    public String getName(Boolean value) {
        return value.toString();
    }

    @Override
    public int getInternalIndex(Boolean value) {
        return value != false ? 0 : 1;
    }
}

