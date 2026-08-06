/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.jspecify.annotations.Nullable;

public class CommandStorage {
    private static final String COMMAND_STORAGE = "command_storage";
    private final Map<String, Container> namespaces = new HashMap<String, Container>();
    private final SavedDataStorage savedDataStorage;

    public CommandStorage(SavedDataStorage savedDataStorage) {
        this.savedDataStorage = savedDataStorage;
    }

    public CompoundTag get(Identifier id) {
        Container container = this.getContainer(id.getNamespace());
        if (container != null) {
            return container.get(id.getPath());
        }
        return new CompoundTag();
    }

    private @Nullable Container getContainer(String namespace) {
        Container container = this.namespaces.get(namespace);
        if (container != null) {
            return container;
        }
        Container newContainer = this.savedDataStorage.get(Container.type(namespace));
        if (newContainer != null) {
            this.namespaces.put(namespace, newContainer);
        }
        return newContainer;
    }

    private Container getOrCreateContainer(String namespace) {
        Container container = this.namespaces.get(namespace);
        if (container != null) {
            return container;
        }
        Container newContainer = this.savedDataStorage.computeIfAbsent(Container.type(namespace));
        this.namespaces.put(namespace, newContainer);
        return newContainer;
    }

    public void set(Identifier id, CompoundTag contents) {
        this.getOrCreateContainer(id.getNamespace()).put(id.getPath(), contents);
    }

    public Stream<Identifier> keys() {
        return this.namespaces.entrySet().stream().flatMap(e -> ((Container)e.getValue()).getKeys((String)e.getKey()));
    }

    private static class Container
    extends SavedData {
        public static final Codec<Container> CODEC = RecordCodecBuilder.create(i -> i.group((App)Codec.unboundedMap(ExtraCodecs.RESOURCE_PATH_CODEC, CompoundTag.CODEC).fieldOf("contents").forGetter(container -> container.storage)).apply((Applicative)i, Container::new));
        private final Map<String, CompoundTag> storage;

        private Container(Map<String, CompoundTag> storage) {
            this.storage = new HashMap<String, CompoundTag>(storage);
        }

        private Container() {
            this(new HashMap<String, CompoundTag>());
        }

        public static SavedDataType<Container> type(String namespace) {
            return new SavedDataType<Container>(Identifier.fromNamespaceAndPath(namespace, CommandStorage.COMMAND_STORAGE), Container::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
        }

        public CompoundTag get(String id) {
            CompoundTag result = this.storage.get(id);
            return result != null ? result : new CompoundTag();
        }

        public void put(String id, CompoundTag contents) {
            if (contents.isEmpty()) {
                this.storage.remove(id);
            } else {
                this.storage.put(id, contents);
            }
            this.setDirty();
        }

        public Stream<Identifier> getKeys(String namespace) {
            return this.storage.keySet().stream().map(p -> Identifier.fromNamespaceAndPath(namespace, p));
        }
    }
}

