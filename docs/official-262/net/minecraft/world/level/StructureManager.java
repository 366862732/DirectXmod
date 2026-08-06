/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  it.unimi.dsi.fastutil.longs.LongIterator
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.jspecify.annotations.Nullable;

public class StructureManager {
    private final LevelAccessor level;
    private final WorldOptions worldOptions;
    private final StructureCheck structureCheck;

    public StructureManager(LevelAccessor level, WorldOptions worldOptions, StructureCheck structureCheck) {
        this.level = level;
        this.worldOptions = worldOptions;
        this.structureCheck = structureCheck;
    }

    public StructureManager forWorldGenRegion(WorldGenRegion region) {
        if (region.getLevel() != this.level) {
            throw new IllegalStateException("Using invalid structure manager (source level: " + String.valueOf(region.getLevel()) + ", region: " + String.valueOf(region));
        }
        return new StructureManager(region, this.worldOptions, this.structureCheck);
    }

    public List<StructureStart> startsForStructure(ChunkPos pos, Predicate<Structure> matcher) {
        Map<Structure, LongSet> allReferences = this.level.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
        ImmutableList.Builder result = ImmutableList.builder();
        for (Map.Entry<Structure, LongSet> entry : allReferences.entrySet()) {
            Structure structure = entry.getKey();
            if (!matcher.test(structure)) continue;
            this.fillStartsForStructure(structure, entry.getValue(), arg_0 -> ((ImmutableList.Builder)result).add(arg_0));
        }
        return result.build();
    }

    public List<StructureStart> startsForStructure(SectionPos pos, Structure structure) {
        LongSet referencesForStructure = this.level.getChunk(pos.x(), pos.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForStructure(structure);
        ImmutableList.Builder result = ImmutableList.builder();
        this.fillStartsForStructure(structure, referencesForStructure, arg_0 -> ((ImmutableList.Builder)result).add(arg_0));
        return result.build();
    }

    public void fillStartsForStructure(Structure structure, LongSet referencesForStructure, Consumer<StructureStart> consumer) {
        LongIterator longIterator = referencesForStructure.iterator();
        while (longIterator.hasNext()) {
            long key = (Long)longIterator.next();
            SectionPos sectionPos = SectionPos.of(ChunkPos.unpack(key), this.level.getMinSectionY());
            StructureStart start = this.getStartForStructure(sectionPos, structure, this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_STARTS));
            if (start == null || !start.isValid()) continue;
            consumer.accept(start);
        }
    }

    public @Nullable StructureStart getStartForStructure(SectionPos pos, Structure structure, StructureAccess chunk) {
        return chunk.getStartForStructure(structure);
    }

    public void setStartForStructure(SectionPos pos, Structure structure, StructureStart start, StructureAccess chunk) {
        chunk.setStartForStructure(structure, start);
    }

    public void addReferenceForStructure(SectionPos pos, Structure structure, long reference, StructureAccess chunk) {
        chunk.addReferenceForStructure(structure, reference);
    }

    public boolean shouldGenerateStructures() {
        return this.worldOptions.generateStructures();
    }

    public StructureStart getStructureAt(BlockPos blockPos, Structure structure) {
        for (StructureStart structureStart : this.startsForStructure(SectionPos.of(blockPos), structure)) {
            if (!structureStart.getBoundingBox().isInside(blockPos)) continue;
            return structureStart;
        }
        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureWithPieceAt(BlockPos blockPos, TagKey<Structure> structureTag) {
        return this.getStructureWithPieceAt(blockPos, (Holder<Structure> structure) -> structure.is(structureTag));
    }

    public StructureStart getStructureWithPieceAt(BlockPos blockPos, HolderSet<Structure> structures) {
        return this.getStructureWithPieceAt(blockPos, structures::contains);
    }

    public StructureStart getStructureWithPieceAt(BlockPos blockPos, Predicate<Holder<Structure>> predicate) {
        HolderLookup.RegistryLookup structures = this.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (StructureStart structureStart : this.startsForStructure(ChunkPos.containing(blockPos), arg_0 -> StructureManager.lambda$getStructureWithPieceAt$1((Registry)structures, predicate, arg_0))) {
            if (!this.structureHasPieceAt(blockPos, structureStart)) continue;
            return structureStart;
        }
        return StructureStart.INVALID_START;
    }

    public StructureStart getStructureWithPieceAt(BlockPos blockPos, Structure structure) {
        for (StructureStart structureStart : this.startsForStructure(SectionPos.of(blockPos), structure)) {
            if (!this.structureHasPieceAt(blockPos, structureStart)) continue;
            return structureStart;
        }
        return StructureStart.INVALID_START;
    }

    public boolean structureHasPieceAt(BlockPos blockPos, StructureStart structureStart) {
        for (StructurePiece piece : structureStart.getPieces()) {
            if (!piece.getBoundingBox().isInside(blockPos)) continue;
            return true;
        }
        return false;
    }

    public boolean hasAnyStructureAt(BlockPos pos) {
        SectionPos sectionPos = SectionPos.of(pos);
        return this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
    }

    public Map<Structure, LongSet> getAllStructuresAt(BlockPos pos) {
        SectionPos sectionPos = SectionPos.of(pos);
        return this.level.getChunk(sectionPos.x(), sectionPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getAllReferences();
    }

    public StructureCheckResult checkStructurePresence(ChunkPos pos, Structure structure, StructurePlacement placement, boolean createReference) {
        return this.structureCheck.checkStart(pos, structure, placement, createReference);
    }

    public void addReference(StructureStart start) {
        start.addReference();
        this.structureCheck.incrementReference(start.getChunkPos(), start.getStructure());
    }

    public RegistryAccess registryAccess() {
        return this.level.registryAccess();
    }

    private static /* synthetic */ boolean lambda$getStructureWithPieceAt$1(Registry structures, Predicate predicate, Structure s) {
        return structures.get(structures.getId(s)).map(predicate::test).orElse(false);
    }
}

