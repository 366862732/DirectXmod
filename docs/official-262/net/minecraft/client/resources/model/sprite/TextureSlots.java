/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMap$Entry
 *  it.unimi.dsi.fastutil.objects.Object2ObjectMaps
 *  it.unimi.dsi.fastutil.objects.ObjectIterator
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.resources.model.sprite;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class TextureSlots {
    public static final TextureSlots EMPTY = new TextureSlots(Map.of());
    private static final char REFERENCE_CHAR = '#';
    private final Map<String, Material> resolvedValues;

    private TextureSlots(Map<String, Material> resolvedValues) {
        this.resolvedValues = resolvedValues;
    }

    public @Nullable Material getMaterial(String reference) {
        if (TextureSlots.isTextureReference(reference)) {
            reference = reference.substring(1);
        }
        return this.resolvedValues.get(reference);
    }

    private static boolean isTextureReference(String texture) {
        return texture.charAt(0) == '#';
    }

    public static Data parseTextureMap(JsonObject texturesObject) {
        Data.Builder builder = new Data.Builder();
        for (Map.Entry entry : texturesObject.entrySet()) {
            TextureSlots.parseEntry((String)entry.getKey(), (JsonElement)entry.getValue(), builder);
        }
        return builder.build();
    }

    private static void parseEntry(String slot, JsonElement value, Data.Builder output) {
        if (GsonHelper.isStringValue(value) && TextureSlots.isTextureReference(value.getAsString())) {
            output.addReference(slot, value.getAsString().substring(1));
        } else {
            output.addTexture(slot, (Material)Material.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)value).getOrThrow(JsonParseException::new));
        }
    }

    @Environment(value=EnvType.CLIENT)
    public record Data(Map<String, SlotContents> values) {
        public static final Data EMPTY = new Data(Map.of());

        @Environment(value=EnvType.CLIENT)
        public static class Builder {
            private final Map<String, SlotContents> textureMap = new HashMap<String, SlotContents>();

            public Builder addReference(String slot, String reference) {
                this.textureMap.put(slot, new Reference(reference));
                return this;
            }

            public Builder addTexture(String slot, Material material) {
                this.textureMap.put(slot, new Value(material));
                return this;
            }

            public Data build() {
                if (this.textureMap.isEmpty()) {
                    return EMPTY;
                }
                return new Data(Map.copyOf(this.textureMap));
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class Resolver {
        private static final Logger LOGGER = LogUtils.getLogger();
        private final List<Data> entries = new ArrayList<Data>();

        public Resolver addLast(Data data) {
            this.entries.addLast(data);
            return this;
        }

        public Resolver addFirst(Data data) {
            this.entries.addFirst(data);
            return this;
        }

        public TextureSlots resolve(ModelDebugName debugNameProvider) {
            if (this.entries.isEmpty()) {
                return EMPTY;
            }
            Object2ObjectArrayMap resolved = new Object2ObjectArrayMap();
            Object2ObjectArrayMap unresolved = new Object2ObjectArrayMap();
            for (Data data : Lists.reverse(this.entries)) {
                data.values.forEach((arg_0, arg_1) -> Resolver.lambda$resolve$0((Object2ObjectMap)unresolved, (Object2ObjectMap)resolved, arg_0, arg_1));
            }
            if (unresolved.isEmpty()) {
                return new TextureSlots((Map<String, Material>)resolved);
            }
            boolean hasChanges = true;
            while (hasChanges) {
                hasChanges = false;
                ObjectIterator iterator = Object2ObjectMaps.fastIterator((Object2ObjectMap)unresolved);
                while (iterator.hasNext()) {
                    Object2ObjectMap.Entry entry = (Object2ObjectMap.Entry)iterator.next();
                    Material maybeResolved = (Material)resolved.get((Object)((Reference)entry.getValue()).target);
                    if (maybeResolved == null) continue;
                    resolved.put((Object)((String)entry.getKey()), (Object)maybeResolved);
                    iterator.remove();
                    hasChanges = true;
                }
            }
            if (!unresolved.isEmpty()) {
                LOGGER.warn("Unresolved texture references in {}:\n{}", (Object)debugNameProvider.debugName(), (Object)unresolved.entrySet().stream().map(e -> "\t#" + (String)e.getKey() + "-> #" + ((Reference)e.getValue()).target + "\n").collect(Collectors.joining()));
            }
            return new TextureSlots((Map<String, Material>)resolved);
        }

        private static /* synthetic */ void lambda$resolve$0(Object2ObjectMap unresolved, Object2ObjectMap resolved, String slot, SlotContents contents) {
            SlotContents slotContents = contents;
            Objects.requireNonNull(slotContents);
            SlotContents selector0$temp = slotContents;
            int index$1 = 0;
            switch (SwitchBootstraps.typeSwitch("typeSwitch", new Object[]{Value.class, Reference.class}, (SlotContents)selector0$temp, index$1)) {
                default: {
                    throw new MatchException(null, null);
                }
                case 0: {
                    Value value = (Value)selector0$temp;
                    unresolved.remove((Object)slot);
                    resolved.put((Object)slot, (Object)value.material());
                    break;
                }
                case 1: {
                    Reference reference = (Reference)selector0$temp;
                    resolved.remove((Object)slot);
                    unresolved.put((Object)slot, (Object)reference);
                }
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    private record Reference(String target) implements SlotContents
    {
    }

    @Environment(value=EnvType.CLIENT)
    private record Value(Material material) implements SlotContents
    {
    }

    @Environment(value=EnvType.CLIENT)
    public static sealed interface SlotContents
    permits Value, Reference {
    }
}

