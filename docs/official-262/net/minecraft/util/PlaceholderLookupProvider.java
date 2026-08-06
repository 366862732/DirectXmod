/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JavaOps
 *  com.mojang.serialization.Lifecycle
 */
package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.Lifecycle;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RegistryContextSwapper;

public class PlaceholderLookupProvider
implements HolderGetter.Provider {
    private final HolderLookup.Provider context;
    private final UniversalLookup lookup = new UniversalLookup(this);
    private final Map<ResourceKey<Object>, Holder.Reference<Object>> holders = new HashMap<ResourceKey<Object>, Holder.Reference<Object>>();
    private final Map<TagKey<Object>, HolderSet.Named<Object>> holderSets = new HashMap<TagKey<Object>, HolderSet.Named<Object>>();

    public PlaceholderLookupProvider(HolderLookup.Provider context) {
        this.context = context;
    }

    @Override
    public <T> Optional<? extends HolderGetter<T>> lookup(ResourceKey<? extends Registry<? extends T>> key) {
        return Optional.of(this.lookup.castAsLookup());
    }

    public <V> RegistryOps<V> createSerializationContext(DynamicOps<V> parent) {
        return RegistryOps.create(parent, new RegistryOps.RegistryInfoLookup(this){
            final /* synthetic */ PlaceholderLookupProvider this$0;
            {
                PlaceholderLookupProvider placeholderLookupProvider = this$0;
                Objects.requireNonNull(placeholderLookupProvider);
                this.this$0 = placeholderLookupProvider;
            }

            @Override
            public <T> Optional<RegistryOps.RegistryInfo<T>> lookup(ResourceKey<? extends Registry<? extends T>> registryKey) {
                return this.this$0.context.lookup(registryKey).map(RegistryOps.RegistryInfo::fromRegistryLookup).or(() -> Optional.of(new RegistryOps.RegistryInfo(this.this$0.lookup.castAsOwner(), this.this$0.lookup.castAsLookup(), Lifecycle.experimental())));
            }
        });
    }

    public RegistryContextSwapper createSwapper() {
        return new RegistryContextSwapper(this){
            final /* synthetic */ PlaceholderLookupProvider this$0;
            {
                PlaceholderLookupProvider placeholderLookupProvider = this$0;
                Objects.requireNonNull(placeholderLookupProvider);
                this.this$0 = placeholderLookupProvider;
            }

            @Override
            public <T> DataResult<T> swapTo(Codec<T> codec, T value, HolderLookup.Provider newContext) {
                return codec.encodeStart(this.this$0.createSerializationContext(JavaOps.INSTANCE), value).flatMap(v -> codec.parse(newContext.createSerializationContext(JavaOps.INSTANCE), v));
            }
        };
    }

    public boolean hasRegisteredPlaceholders() {
        return !this.holders.isEmpty() || !this.holderSets.isEmpty();
    }

    private class UniversalLookup
    implements HolderGetter<Object>,
    HolderOwner<Object> {
        final /* synthetic */ PlaceholderLookupProvider this$0;

        private UniversalLookup(PlaceholderLookupProvider placeholderLookupProvider) {
            PlaceholderLookupProvider placeholderLookupProvider2 = placeholderLookupProvider;
            Objects.requireNonNull(placeholderLookupProvider2);
            this.this$0 = placeholderLookupProvider2;
        }

        @Override
        public Optional<Holder.Reference<Object>> get(ResourceKey<Object> id) {
            return Optional.of(this.getOrCreate(id));
        }

        @Override
        public Holder.Reference<Object> getOrThrow(ResourceKey<Object> id) {
            return this.getOrCreate(id);
        }

        private Holder.Reference<Object> getOrCreate(ResourceKey<Object> id) {
            return this.this$0.holders.computeIfAbsent(id, k -> Holder.Reference.createStandAlone(this, k));
        }

        @Override
        public Optional<HolderSet.Named<Object>> get(TagKey<Object> id) {
            return Optional.of(this.getOrCreate(id));
        }

        @Override
        public HolderSet.Named<Object> getOrThrow(TagKey<Object> id) {
            return this.getOrCreate(id);
        }

        private HolderSet.Named<Object> getOrCreate(TagKey<Object> id) {
            return this.this$0.holderSets.computeIfAbsent(id, k -> HolderSet.emptyNamed(this, k));
        }

        public <T> HolderGetter<T> castAsLookup() {
            return this;
        }

        public <T> HolderOwner<T> castAsOwner() {
            return this;
        }
    }
}

