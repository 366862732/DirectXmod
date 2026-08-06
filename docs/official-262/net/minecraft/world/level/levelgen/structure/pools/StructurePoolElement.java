/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.FeaturePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.LegacySinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.ListPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jspecify.annotations.Nullable;

public abstract class StructurePoolElement {
    public static final Codec<StructurePoolElement> CODEC = BuiltInRegistries.STRUCTURE_POOL_ELEMENT.byNameCodec().dispatch("element_type", StructurePoolElement::getType, StructurePoolElementType::codec);
    private static final Holder<StructureProcessorList> EMPTY = Holder.direct(new StructureProcessorList(List.of()));
    private volatile  @Nullable StructureTemplatePool.Projection projection;

    protected static <E extends StructurePoolElement> RecordCodecBuilder<E, StructureTemplatePool.Projection> projectionCodec() {
        return StructureTemplatePool.Projection.CODEC.fieldOf("projection").forGetter(StructurePoolElement::getProjection);
    }

    protected StructurePoolElement(StructureTemplatePool.Projection projection) {
        this.projection = projection;
    }

    public abstract Vec3i getSize(StructureTemplateManager var1, Rotation var2);

    public abstract List<StructureTemplate.JigsawBlockInfo> getShuffledJigsawBlocks(StructureTemplateManager var1, BlockPos var2, Rotation var3, RandomSource var4);

    public abstract BoundingBox getBoundingBox(StructureTemplateManager var1, BlockPos var2, Rotation var3);

    public abstract boolean place(StructureTemplateManager var1, WorldGenLevel var2, StructureManager var3, ChunkGenerator var4, BlockPos var5, BlockPos var6, Rotation var7, BoundingBox var8, RandomSource var9, LiquidSettings var10, boolean var11);

    public abstract StructurePoolElementType<?> getType();

    public void handleDataMarker(LevelAccessor level, StructureTemplate.StructureBlockInfo dataMarker, BlockPos position, Rotation rotation, RandomSource random, BoundingBox chunkBB) {
    }

    public StructurePoolElement setProjection(StructureTemplatePool.Projection projection) {
        this.projection = projection;
        return this;
    }

    public StructureTemplatePool.Projection getProjection() {
        StructureTemplatePool.Projection projection = this.projection;
        if (projection == null) {
            throw new IllegalStateException();
        }
        return projection;
    }

    public int getGroundLevelDelta() {
        return 1;
    }

    public static Function<StructureTemplatePool.Projection, EmptyPoolElement> empty() {
        return p -> EmptyPoolElement.INSTANCE;
    }

    public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String location) {
        return p -> new LegacySinglePoolElement((Either<Identifier, StructureTemplate>)Either.left((Object)Identifier.parse(location)), EMPTY, (StructureTemplatePool.Projection)p, Optional.empty());
    }

    public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String location, Holder<StructureProcessorList> processors) {
        return p -> new LegacySinglePoolElement((Either<Identifier, StructureTemplate>)Either.left((Object)Identifier.parse(location)), processors, (StructureTemplatePool.Projection)p, Optional.empty());
    }

    public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String location) {
        return p -> new SinglePoolElement((Either<Identifier, StructureTemplate>)Either.left((Object)Identifier.parse(location)), EMPTY, (StructureTemplatePool.Projection)p, Optional.empty());
    }

    public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String location, Holder<StructureProcessorList> processors) {
        return p -> new SinglePoolElement((Either<Identifier, StructureTemplate>)Either.left((Object)Identifier.parse(location)), processors, (StructureTemplatePool.Projection)p, Optional.empty());
    }

    public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String location, LiquidSettings overrideLiquidSettings) {
        return p -> new SinglePoolElement((Either<Identifier, StructureTemplate>)Either.left((Object)Identifier.parse(location)), EMPTY, (StructureTemplatePool.Projection)p, Optional.of(overrideLiquidSettings));
    }

    public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String location, Holder<StructureProcessorList> processors, LiquidSettings overrideLiquidSettings) {
        return p -> new SinglePoolElement((Either<Identifier, StructureTemplate>)Either.left((Object)Identifier.parse(location)), processors, (StructureTemplatePool.Projection)p, Optional.of(overrideLiquidSettings));
    }

    public static Function<StructureTemplatePool.Projection, FeaturePoolElement> feature(Holder<PlacedFeature> feature) {
        return p -> new FeaturePoolElement(feature, (StructureTemplatePool.Projection)p);
    }

    public static Function<StructureTemplatePool.Projection, ListPoolElement> list(List<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>> elements) {
        return p -> new ListPoolElement(elements.stream().map(e -> (StructurePoolElement)e.apply(p)).collect(Collectors.toList()), (StructureTemplatePool.Projection)p);
    }
}

