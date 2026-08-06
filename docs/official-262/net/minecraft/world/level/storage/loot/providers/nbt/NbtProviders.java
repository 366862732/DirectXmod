/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 */
package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.StorageNbtProvider;

public class NbtProviders {
    private static final Codec<NbtProvider> TYPED_CODEC = BuiltInRegistries.LOOT_NBT_PROVIDER_TYPE.byNameCodec().dispatch(NbtProvider::codec, c -> c);
    public static final Codec<NbtProvider> CODEC = Codec.lazyInitialized(() -> Codec.either(ContextNbtProvider.INLINE_CODEC, TYPED_CODEC).xmap(Either::unwrap, provider -> {
        Either either;
        if (provider instanceof ContextNbtProvider) {
            ContextNbtProvider context = (ContextNbtProvider)provider;
            either = Either.left((Object)context);
        } else {
            either = Either.right((Object)provider);
        }
        return either;
    }));

    public static MapCodec<? extends NbtProvider> bootstrap(Registry<MapCodec<? extends NbtProvider>> registry) {
        Registry.register(registry, "storage", StorageNbtProvider.MAP_CODEC);
        return Registry.register(registry, "context", ContextNbtProvider.MAP_CODEC);
    }
}

