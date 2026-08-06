/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Iterables
 *  com.google.common.collect.Lists
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.apache.commons.lang3.mutable.MutableObject
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Validatable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.nbt.ContextNbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProviders;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

public class CopyCustomDataFunction
extends LootItemConditionalFunction {
    public static final MapCodec<CopyCustomDataFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> CopyCustomDataFunction.commonFields(i).and(i.group((App)NbtProviders.CODEC.fieldOf("source").forGetter(f -> f.source), (App)CopyOperation.CODEC.listOf().fieldOf("ops").forGetter(f -> f.operations))).apply((Applicative)i, CopyCustomDataFunction::new));
    private final NbtProvider source;
    private final List<CopyOperation> operations;

    private CopyCustomDataFunction(List<LootItemCondition> predicates, NbtProvider source, List<CopyOperation> operations) {
        super(predicates);
        this.source = source;
        this.operations = List.copyOf(operations);
    }

    public MapCodec<CopyCustomDataFunction> codec() {
        return MAP_CODEC;
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);
        Validatable.validate(context, "source", this.source);
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    @Override
    public ItemStack run(ItemStack itemStack, LootContext context) {
        Tag sourceTag = this.source.get(context);
        if (sourceTag == null) {
            return itemStack;
        }
        @Nullable MutableObject result = new MutableObject();
        Supplier<Tag> lazyTargetCopy = () -> {
            if (result.get() == null) {
                result.setValue((Object)itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
            }
            return (Tag)result.get();
        };
        this.operations.forEach(op -> op.apply(lazyTargetCopy, sourceTag));
        CompoundTag resultTag = (CompoundTag)result.get();
        if (resultTag != null) {
            CustomData.set(DataComponents.CUSTOM_DATA, itemStack, resultTag);
        }
        return itemStack;
    }

    @Deprecated
    public static Builder copyData(NbtProvider source) {
        return new Builder(source);
    }

    public static Builder copyData(LootContext.EntityTarget source) {
        return new Builder(ContextNbtProvider.forContextEntity(source));
    }

    public static class Builder
    extends LootItemConditionalFunction.Builder<Builder> {
        private final NbtProvider source;
        private final List<CopyOperation> ops = Lists.newArrayList();

        private Builder(NbtProvider source) {
            this.source = source;
        }

        public Builder copy(String sourcePath, String targetPath, MergeStrategy mergeStrategy) {
            try {
                this.ops.add(new CopyOperation(NbtPathArgument.NbtPath.of(sourcePath), NbtPathArgument.NbtPath.of(targetPath), mergeStrategy));
            }
            catch (CommandSyntaxException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }

        public Builder copy(String sourcePath, String targetPath) {
            return this.copy(sourcePath, targetPath, MergeStrategy.REPLACE);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new CopyCustomDataFunction(this.getConditions(), this.source, this.ops);
        }
    }

    private record CopyOperation(NbtPathArgument.NbtPath sourcePath, NbtPathArgument.NbtPath targetPath, MergeStrategy op) {
        public static final Codec<CopyOperation> CODEC = RecordCodecBuilder.create(i -> i.group((App)NbtPathArgument.NbtPath.CODEC.fieldOf("source").forGetter(CopyOperation::sourcePath), (App)NbtPathArgument.NbtPath.CODEC.fieldOf("target").forGetter(CopyOperation::targetPath), (App)MergeStrategy.CODEC.fieldOf("op").forGetter(CopyOperation::op)).apply((Applicative)i, CopyOperation::new));

        public void apply(Supplier<Tag> target, Tag source) {
            try {
                List<Tag> sourceTags = this.sourcePath.get(source);
                if (!sourceTags.isEmpty()) {
                    this.op.merge(target.get(), this.targetPath, sourceTags);
                }
            }
            catch (CommandSyntaxException commandSyntaxException) {
                // empty catch block
            }
        }
    }

    public static enum MergeStrategy implements StringRepresentable
    {
        REPLACE("replace"){

            @Override
            public void merge(Tag target, NbtPathArgument.NbtPath path, List<Tag> sources) throws CommandSyntaxException {
                path.set(target, (Tag)Iterables.getLast(sources));
            }
        }
        ,
        APPEND("append"){

            @Override
            public void merge(Tag target, NbtPathArgument.NbtPath path, List<Tag> sources) throws CommandSyntaxException {
                List<Tag> targets = path.getOrCreate(target, ListTag::new);
                targets.forEach(tag -> {
                    if (tag instanceof ListTag) {
                        ListTag listTag = (ListTag)tag;
                        sources.forEach(source -> listTag.add(source.copy()));
                    }
                });
            }
        }
        ,
        MERGE("merge"){

            @Override
            public void merge(Tag target, NbtPathArgument.NbtPath path, List<Tag> sources) throws CommandSyntaxException {
                List<Tag> targets = path.getOrCreate(target, CompoundTag::new);
                targets.forEach(tag -> {
                    if (tag instanceof CompoundTag) {
                        CompoundTag compoundTag = (CompoundTag)tag;
                        sources.forEach(source -> {
                            if (source instanceof CompoundTag) {
                                CompoundTag sourceTag = (CompoundTag)source;
                                compoundTag.merge(sourceTag);
                            }
                        });
                    }
                });
            }
        };

        public static final Codec<MergeStrategy> CODEC;
        private final String name;

        public abstract void merge(Tag var1, NbtPathArgument.NbtPath var2, List<Tag> var3) throws CommandSyntaxException;

        private MergeStrategy(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(MergeStrategy::values);
        }
    }
}

