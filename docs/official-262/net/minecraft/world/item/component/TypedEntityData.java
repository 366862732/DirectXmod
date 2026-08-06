/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DynamicOps
 *  io.netty.buffer.ByteBuf
 *  org.slf4j.Logger
 */
package net.minecraft.world.item.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public final class TypedEntityData<IdType>
implements TooltipProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TYPE_TAG = "id";
    private final IdType type;
    private final CompoundTag tag;

    public static <T> Codec<TypedEntityData<T>> codec(final Codec<T> typeCodec) {
        return new Codec<TypedEntityData<T>>(){

            public <V> DataResult<Pair<TypedEntityData<T>, V>> decode(DynamicOps<V> ops, V input) {
                return CustomData.COMPOUND_TAG_CODEC.decode(ops, input).flatMap(pair -> {
                    CompoundTag tagWithoutType = ((CompoundTag)pair.getFirst()).copy();
                    Tag typeTag = tagWithoutType.remove(TypedEntityData.TYPE_TAG);
                    if (typeTag == null) {
                        return DataResult.error(() -> "Expected 'id' field in " + String.valueOf(input));
                    }
                    return typeCodec.parse(1.asNbtOps(ops), (Object)typeTag).map(type -> Pair.of(new TypedEntityData<Object>(type, tagWithoutType), (Object)pair.getSecond()));
                });
            }

            public <V> DataResult<V> encode(TypedEntityData<T> input, DynamicOps<V> ops, V prefix) {
                return typeCodec.encodeStart(1.asNbtOps(ops), input.type).flatMap(typeTag -> {
                    CompoundTag tag = input.tag.copy();
                    tag.put(TypedEntityData.TYPE_TAG, (Tag)typeTag);
                    return CustomData.COMPOUND_TAG_CODEC.encode((Object)tag, ops, prefix);
                });
            }

            private static <T> DynamicOps<Tag> asNbtOps(DynamicOps<T> ops) {
                if (ops instanceof RegistryOps) {
                    RegistryOps registryOps = (RegistryOps)ops;
                    return registryOps.withParent(NbtOps.INSTANCE);
                }
                return NbtOps.INSTANCE;
            }
        };
    }

    public static <B extends ByteBuf, T> StreamCodec<B, TypedEntityData<T>> streamCodec(StreamCodec<B, T> typeCodec) {
        return StreamCodec.composite(typeCodec, TypedEntityData::type, ByteBufCodecs.COMPOUND_TAG, TypedEntityData::tag, TypedEntityData::new);
    }

    private TypedEntityData(IdType type, CompoundTag data) {
        this.type = type;
        this.tag = TypedEntityData.stripId(data);
    }

    public static <T> TypedEntityData<T> of(T type, CompoundTag data) {
        return new TypedEntityData<T>(type, data);
    }

    private static CompoundTag stripId(CompoundTag tag) {
        if (tag.contains(TYPE_TAG)) {
            CompoundTag copy = tag.copy();
            copy.remove(TYPE_TAG);
            return copy;
        }
        return tag;
    }

    public IdType type() {
        return this.type;
    }

    public boolean contains(String name) {
        return this.tag.contains(name);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof TypedEntityData) {
            TypedEntityData customData = (TypedEntityData)obj;
            return this.type == customData.type && this.tag.equals(customData.tag);
        }
        return false;
    }

    public int hashCode() {
        return 31 * this.type.hashCode() + this.tag.hashCode();
    }

    public String toString() {
        return String.valueOf(this.type) + " " + String.valueOf(this.tag);
    }

    public void loadInto(Entity entity) {
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER);){
            TagValueOutput output = TagValueOutput.createWithContext(reporter, entity.registryAccess());
            entity.saveWithoutId(output);
            CompoundTag entityData = output.buildResult();
            UUID uuid = entity.getUUID();
            entityData.merge(this.getUnsafe());
            entity.load(TagValueInput.create((ProblemReporter)reporter, (HolderLookup.Provider)entity.registryAccess(), entityData));
            entity.setUUID(uuid);
        }
    }

    /*
     * Exception decompiling
     */
    public boolean loadInto(BlockEntity blockEntity, HolderLookup.Provider registries) {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Tried to end blocks [5[CATCHBLOCK]], but top level block is 2[TRYBLOCK]
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.processEndingBlocks(Op04StructuredStatement.java:435)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:484)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }

    private CompoundTag tag() {
        return this.tag;
    }

    @Deprecated
    public CompoundTag getUnsafe() {
        return this.tag;
    }

    public CompoundTag copyTagWithoutId() {
        return this.tag.copy();
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> consumer, TooltipFlag flag, DataComponentGetter components) {
        if (this.type.getClass() == EntityType.class) {
            EntityType type = (EntityType)this.type;
            if (context.isPeaceful() && !type.isAllowedInPeaceful()) {
                consumer.accept(Component.translatable("item.spawn_egg.peaceful").withStyle(ChatFormatting.RED));
            }
        }
    }

    private static /* synthetic */ String lambda$loadInto$0() {
        return "(rollback)";
    }
}

