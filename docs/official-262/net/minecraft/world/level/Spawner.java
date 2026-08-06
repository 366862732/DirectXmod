/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

public interface Spawner {
    public void setEntityId(EntityType<?> var1, RandomSource var2);

    public static void appendHoverText(@Nullable TypedEntityData<BlockEntityType<?>> data, Consumer<Component> consumer, String nextSpawnDataTagKey) {
        Component displayName = Spawner.getSpawnEntityDisplayName(data, nextSpawnDataTagKey);
        if (displayName != null) {
            consumer.accept(displayName);
        } else {
            consumer.accept(CommonComponents.EMPTY);
            consumer.accept(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
            consumer.accept(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
        }
    }

    public static @Nullable Component getSpawnEntityDisplayName(@Nullable TypedEntityData<BlockEntityType<?>> data, String nextSpawnDataTagKey) {
        if (data == null) {
            return null;
        }
        return data.getUnsafe().getCompound(nextSpawnDataTagKey).flatMap(nextSpawnData -> nextSpawnData.getCompound("entity")).flatMap(entityTag -> entityTag.read("id", EntityType.CODEC)).map(entityType -> Component.translatable(entityType.getDescriptionId()).withStyle(ChatFormatting.GRAY)).orElse(null);
    }
}

