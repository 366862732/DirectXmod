/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.state.properties.ChestType;

@Environment(value=EnvType.CLIENT)
public record MultiblockChestResources<T>(T single, T left, T right) {
    public T select(ChestType chestType) {
        return switch (chestType) {
            default -> throw new MatchException(null, null);
            case ChestType.SINGLE -> this.single;
            case ChestType.LEFT -> this.left;
            case ChestType.RIGHT -> this.right;
        };
    }

    public <S> MultiblockChestResources<S> map(Function<T, S> mapper) {
        return new MultiblockChestResources<S>(mapper.apply(this.single), mapper.apply(this.left), mapper.apply(this.right));
    }
}

