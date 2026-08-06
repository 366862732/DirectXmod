/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.google.common.collect.ImmutableSet
 *  com.google.common.collect.ImmutableSet$Builder
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record ShaderDefines(Map<String, String> values, Set<String> flags) {
    public static final ShaderDefines EMPTY = new ShaderDefines(Map.of(), Set.of());
    public static final Codec<ShaderDefines> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.unboundedMap((Codec)Codec.STRING, (Codec)Codec.STRING).optionalFieldOf("values", Map.of()).forGetter(ShaderDefines::values), (App)Codec.STRING.listOf().xmap(Set::copyOf, List::copyOf).optionalFieldOf("flags", Set.of()).forGetter(ShaderDefines::flags)).apply((Applicative)i, ShaderDefines::new));

    public static Builder builder() {
        return new Builder();
    }

    public ShaderDefines withOverrides(ShaderDefines defines) {
        if (this.isEmpty()) {
            return defines;
        }
        if (defines.isEmpty()) {
            return this;
        }
        ImmutableMap.Builder newValues = ImmutableMap.builderWithExpectedSize((int)(this.values.size() + defines.values.size()));
        newValues.putAll(this.values);
        newValues.putAll(defines.values);
        ImmutableSet.Builder newFlags = ImmutableSet.builderWithExpectedSize((int)(this.flags.size() + defines.flags.size()));
        newFlags.addAll(this.flags);
        newFlags.addAll(defines.flags);
        return new ShaderDefines((Map<String, String>)newValues.buildKeepingLast(), (Set<String>)newFlags.build());
    }

    public String asSourceDirectives() {
        StringBuilder directives = new StringBuilder();
        for (Map.Entry<String, String> entry : this.values.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            directives.append("#define ").append(key).append(" ").append(value).append('\n');
        }
        for (String flag : this.flags) {
            directives.append("#define ").append(flag).append('\n');
        }
        return directives.toString();
    }

    public boolean isEmpty() {
        return this.values.isEmpty() && this.flags.isEmpty();
    }

    @Environment(value=EnvType.CLIENT)
    public static class Builder {
        private final ImmutableMap.Builder<String, String> values = ImmutableMap.builder();
        private final ImmutableSet.Builder<String> flags = ImmutableSet.builder();

        private Builder() {
        }

        public Builder define(String key, String value) {
            if (value.isBlank()) {
                throw new IllegalArgumentException("Cannot define empty string");
            }
            this.values.put((Object)key, (Object)Builder.escapeNewLines(value));
            return this;
        }

        private static String escapeNewLines(String value) {
            return value.replaceAll("\n", "\\\\\n");
        }

        public Builder define(String key, float value) {
            this.values.put((Object)key, (Object)String.valueOf(value));
            return this;
        }

        public Builder define(String key, int value) {
            this.values.put((Object)key, (Object)String.valueOf(value));
            return this;
        }

        public Builder define(String key) {
            this.flags.add((Object)key);
            return this;
        }

        public ShaderDefines build() {
            return new ShaderDefines((Map<String, String>)this.values.build(), (Set<String>)this.flags.build());
        }
    }
}

