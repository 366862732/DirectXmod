/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.entity.variant;

import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class VariantUtils {
    public static final String TAG_VARIANT = "variant";

    public static <T> Holder<T> getDefaultOrAny(RegistryAccess registryAccess, ResourceKey<T> id) {
        HolderLookup.RegistryLookup registry = registryAccess.lookupOrThrow(id.registryKey());
        return (Holder)registry.get(id).or(((Registry)registry)::getAny).orElseThrow();
    }

    public static <T> Holder<T> getAny(RegistryAccess registryAccess, ResourceKey<? extends Registry<T>> registryId) {
        return registryAccess.lookupOrThrow(registryId).getAny().orElseThrow();
    }

    public static <T> void writeVariant(ValueOutput output, Holder<T> holder) {
        holder.unwrapKey().ifPresent(k -> output.store(TAG_VARIANT, Identifier.CODEC, k.identifier()));
    }

    public static <T> Optional<Holder<T>> readVariant(ValueInput input, ResourceKey<? extends Registry<T>> registryId) {
        return input.read(TAG_VARIANT, Identifier.CODEC).map(id -> ResourceKey.create(registryId, id)).flatMap(input.lookup()::get);
    }

    public static <T extends PriorityProvider<SpawnContext, ?>> Optional<Holder.Reference<T>> selectVariantToSpawn(SpawnContext context, ResourceKey<Registry<T>> variantRegistry) {
        ServerLevelAccessor level = context.level();
        Stream entries = level.registryAccess().lookupOrThrow(variantRegistry).listElements();
        return PriorityProvider.pick(entries, Holder::value, level.getRandom(), context);
    }
}

