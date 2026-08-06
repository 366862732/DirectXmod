/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  io.netty.handler.codec.DecoderException
 *  io.netty.handler.codec.EncoderException
 *  org.apache.commons.lang3.ObjectUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.network.syncher;

import com.mojang.logging.LogUtils;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.util.ClassTreeIdRegistry;
import org.apache.commons.lang3.ObjectUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SynchedEntityData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_ID_VALUE = 254;
    private static final ClassTreeIdRegistry ID_REGISTRY = new ClassTreeIdRegistry();
    private final SyncedDataHolder entity;
    private final DataItem<?>[] itemsById;
    private boolean isDirty;

    private SynchedEntityData(SyncedDataHolder entity, DataItem<?>[] itemsById) {
        this.entity = entity;
        this.itemsById = itemsById;
    }

    public static <T> EntityDataAccessor<T> defineId(Class<? extends SyncedDataHolder> clazz, EntityDataSerializer<T> type) {
        int id;
        if (LOGGER.isDebugEnabled()) {
            try {
                Class<?> aClass = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
                if (!aClass.equals(clazz)) {
                    LOGGER.debug("defineId called for: {} from {}", new Object[]{clazz, aClass, new RuntimeException()});
                }
            }
            catch (ClassNotFoundException aClass) {
                // empty catch block
            }
        }
        if ((id = ID_REGISTRY.define(clazz)) > 254) {
            throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is 254)");
        }
        return type.createAccessor(id);
    }

    private <T> DataItem<T> getItem(EntityDataAccessor<T> accessor) {
        return this.itemsById[accessor.id()];
    }

    public <T> T get(EntityDataAccessor<T> accessor) {
        return this.getItem(accessor).getValue();
    }

    public <T> void set(EntityDataAccessor<T> accessor, T value) {
        this.set(accessor, value, false);
    }

    public <T> void set(EntityDataAccessor<T> accessor, T value, boolean forceDirty) {
        DataItem<T> dataItem = this.getItem(accessor);
        if (forceDirty || ObjectUtils.notEqual(value, dataItem.getValue())) {
            dataItem.setValue(value);
            this.entity.onSyncedDataUpdated(accessor);
            dataItem.setDirty(true);
            this.isDirty = true;
        }
    }

    public boolean isDirty() {
        return this.isDirty;
    }

    public @Nullable List<DataValue<?>> packDirty() {
        if (!this.isDirty) {
            return null;
        }
        this.isDirty = false;
        ArrayList result = new ArrayList();
        for (DataItem<?> dataItem : this.itemsById) {
            if (!dataItem.isDirty()) continue;
            dataItem.setDirty(false);
            result.add(dataItem.value());
        }
        return result;
    }

    public @Nullable List<DataValue<?>> getNonDefaultValues() {
        ArrayList result = null;
        for (DataItem<?> dataItem : this.itemsById) {
            if (dataItem.isSetToDefault()) continue;
            if (result == null) {
                result = new ArrayList();
            }
            result.add(dataItem.value());
        }
        return result;
    }

    public void assignValues(List<DataValue<?>> items) {
        for (DataValue<?> item : items) {
            DataItem<?> dataItem = this.itemsById[item.id];
            this.assignValue(dataItem, item);
            this.entity.onSyncedDataUpdated(dataItem.getAccessor());
        }
        this.entity.onSyncedDataUpdated(items);
    }

    private <T> void assignValue(DataItem<T> dataItem, DataValue<?> item) {
        if (!Objects.equals(item.serializer(), dataItem.accessor.serializer())) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)", dataItem.accessor.id(), this.entity, dataItem.value, dataItem.value.getClass(), item.value, item.value.getClass()));
        }
        dataItem.setValue(item.value);
    }

    public static class DataItem<T> {
        private final EntityDataAccessor<T> accessor;
        private T value;
        private final T initialValue;
        private boolean dirty;

        public DataItem(EntityDataAccessor<T> accessor, T initialValue) {
            this.accessor = accessor;
            this.initialValue = initialValue;
            this.value = initialValue;
        }

        public EntityDataAccessor<T> getAccessor() {
            return this.accessor;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public T getValue() {
            return this.value;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public void setDirty(boolean dirty) {
            this.dirty = dirty;
        }

        public boolean isSetToDefault() {
            return this.initialValue.equals(this.value);
        }

        public DataValue<T> value() {
            return DataValue.create(this.accessor, this.value);
        }
    }

    public record DataValue<T>(int id, EntityDataSerializer<T> serializer, T value) {
        public static <T> DataValue<T> create(EntityDataAccessor<T> accessor, T value) {
            EntityDataSerializer<T> serializer = accessor.serializer();
            return new DataValue<T>(accessor.id(), serializer, serializer.copy(value));
        }

        public void write(RegistryFriendlyByteBuf output) {
            int serializerId = EntityDataSerializers.getSerializedId(this.serializer);
            if (serializerId < 0) {
                throw new EncoderException("Unknown serializer type " + String.valueOf(this.serializer));
            }
            output.writeByte(this.id);
            output.writeVarInt(serializerId);
            this.serializer.codec().encode(output, this.value);
        }

        public static DataValue<?> read(RegistryFriendlyByteBuf input, int id) {
            int type = input.readVarInt();
            EntityDataSerializer<?> serializer = EntityDataSerializers.getSerializer(type);
            if (serializer == null) {
                throw new DecoderException("Unknown serializer type " + type);
            }
            return DataValue.read(input, id, serializer);
        }

        private static <T> DataValue<T> read(RegistryFriendlyByteBuf input, int id, EntityDataSerializer<T> serializer) {
            return new DataValue<T>(id, serializer, serializer.codec().decode(input));
        }
    }

    public static class Builder {
        private final SyncedDataHolder entity;
        private final @Nullable DataItem<?>[] itemsById;

        public Builder(SyncedDataHolder entity) {
            this.entity = entity;
            this.itemsById = new DataItem[ID_REGISTRY.getCount(entity.getClass())];
        }

        public <T> Builder define(EntityDataAccessor<T> accessor, T value) {
            int id = accessor.id();
            if (id > this.itemsById.length) {
                throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is " + this.itemsById.length + ")");
            }
            if (this.itemsById[id] != null) {
                throw new IllegalArgumentException("Duplicate id value for " + id + "!");
            }
            if (EntityDataSerializers.getSerializedId(accessor.serializer()) < 0) {
                throw new IllegalArgumentException("Unregistered serializer " + String.valueOf(accessor.serializer()) + " for " + id + "!");
            }
            this.itemsById[accessor.id()] = new DataItem<T>(accessor, value);
            return this;
        }

        public SynchedEntityData build() {
            for (int i = 0; i < this.itemsById.length; ++i) {
                if (this.itemsById[i] != null) continue;
                throw new IllegalStateException("Entity " + String.valueOf(this.entity.getClass()) + " has not defined synched data value " + i);
            }
            return new SynchedEntityData(this.entity, this.itemsById);
        }
    }
}

