/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.item;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.predicates.BlockPredicate;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class AdventureModePredicate {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<AdventureModePredicate> CODEC = ExtraCodecs.compactListCodec(BlockPredicate.CODEC, ExtraCodecs.nonEmptyList(BlockPredicate.CODEC.listOf())).xmap(AdventureModePredicate::new, p -> p.predicates);
    public static final StreamCodec<RegistryFriendlyByteBuf, AdventureModePredicate> STREAM_CODEC = StreamCodec.composite(BlockPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()), predicate -> predicate.predicates, AdventureModePredicate::new);
    public static final Component CAN_BREAK_HEADER = Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY);
    public static final Component CAN_PLACE_HEADER = Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY);
    private static final Component UNKNOWN_USE = Component.translatable("item.canUse.unknown").withStyle(ChatFormatting.GRAY);
    private final List<BlockPredicate> predicates;
    private @Nullable List<Component> cachedTooltip;
    private @Nullable BlockInWorld lastCheckedBlock;
    private boolean lastResult;
    private boolean checksBlockEntity;

    public AdventureModePredicate(List<BlockPredicate> predicates) {
        this.predicates = predicates;
    }

    private static boolean areSameBlocks(BlockInWorld blockInWorld, @Nullable BlockInWorld cachedBlock, boolean checkBlockEntity) {
        if (cachedBlock == null || blockInWorld.getState() != cachedBlock.getState()) {
            return false;
        }
        if (!checkBlockEntity) {
            return true;
        }
        if (blockInWorld.getEntity() == null && cachedBlock.getEntity() == null) {
            return true;
        }
        if (blockInWorld.getEntity() == null || cachedBlock.getEntity() == null) {
            return false;
        }
        try (ProblemReporter.ScopedCollector reporter = new ProblemReporter.ScopedCollector(LOGGER);){
            RegistryAccess registryAccess = blockInWorld.getLevel().registryAccess();
            CompoundTag inWorldTag = AdventureModePredicate.saveBlockEntity(blockInWorld.getEntity(), registryAccess, reporter);
            CompoundTag cachedTag = AdventureModePredicate.saveBlockEntity(cachedBlock.getEntity(), registryAccess, reporter);
            boolean bl = Objects.equals(inWorldTag, cachedTag);
            return bl;
        }
    }

    private static CompoundTag saveBlockEntity(BlockEntity blockEntity, RegistryAccess registryAccess, ProblemReporter reporter) {
        TagValueOutput inWorldOutput = TagValueOutput.createWithContext(reporter.forChild(blockEntity.problemPath()), registryAccess);
        blockEntity.saveWithId(inWorldOutput);
        return inWorldOutput.buildResult();
    }

    public boolean test(BlockInWorld blockInWorld) {
        if (AdventureModePredicate.areSameBlocks(blockInWorld, this.lastCheckedBlock, this.checksBlockEntity)) {
            return this.lastResult;
        }
        this.lastCheckedBlock = blockInWorld;
        this.checksBlockEntity = false;
        for (BlockPredicate predicate : this.predicates) {
            if (!predicate.matches(blockInWorld)) continue;
            this.checksBlockEntity |= predicate.requiresNbt();
            this.lastResult = true;
            return true;
        }
        this.lastResult = false;
        return false;
    }

    private List<Component> tooltip() {
        if (this.cachedTooltip == null) {
            this.cachedTooltip = AdventureModePredicate.computeTooltip(this.predicates);
        }
        return this.cachedTooltip;
    }

    public void addToTooltip(Consumer<Component> consumer) {
        this.tooltip().forEach(consumer);
    }

    private static List<Component> computeTooltip(List<BlockPredicate> predicates) {
        for (BlockPredicate predicate2 : predicates) {
            if (!predicate2.blocks().isEmpty()) continue;
            return List.of(UNKNOWN_USE);
        }
        return predicates.stream().flatMap(predicate -> predicate.blocks().orElseThrow().stream()).distinct().map(block -> ((Block)block.value()).getName().withStyle(ChatFormatting.DARK_GRAY)).toList();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof AdventureModePredicate) {
            AdventureModePredicate predicate = (AdventureModePredicate)obj;
            return this.predicates.equals(predicate.predicates);
        }
        return false;
    }

    public int hashCode() {
        return this.predicates.hashCode();
    }

    public String toString() {
        return "AdventureModePredicate{predicates=" + String.valueOf(this.predicates) + "}";
    }
}

