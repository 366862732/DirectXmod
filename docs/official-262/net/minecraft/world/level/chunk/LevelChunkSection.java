/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.chunk;

import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.material.FluidState;

public class LevelChunkSection {
    public static final int BIOME_CONTAINER_BITS = 2;
    private short nonEmptyBlockCount;
    private short fluidCount;
    private short tickingBlockCount;
    private short tickingFluidCount;
    private final PalettedContainer<BlockState> states;
    private PalettedContainerRO<Holder<Biome>> biomes;

    private LevelChunkSection(LevelChunkSection source) {
        this.nonEmptyBlockCount = source.nonEmptyBlockCount;
        this.fluidCount = source.fluidCount;
        this.tickingBlockCount = source.tickingBlockCount;
        this.tickingFluidCount = source.tickingFluidCount;
        this.states = source.states.copy();
        this.biomes = source.biomes.copy();
    }

    public LevelChunkSection(PalettedContainer<BlockState> states, PalettedContainerRO<Holder<Biome>> biomes) {
        this.states = states;
        this.biomes = biomes;
        this.recalcBlockCounts();
    }

    public LevelChunkSection(PalettedContainerFactory containerFactory) {
        this.states = containerFactory.createForBlockStates();
        this.biomes = containerFactory.createForBiomes();
    }

    public BlockState getBlockState(int sectionX, int sectionY, int sectionZ) {
        return this.states.get(sectionX, sectionY, sectionZ);
    }

    public FluidState getFluidState(int sectionX, int sectionY, int sectionZ) {
        return this.states.get(sectionX, sectionY, sectionZ).getFluidState();
    }

    public void acquire() {
        this.states.acquire();
    }

    public void release() {
        this.states.release();
    }

    public BlockState setBlockState(int sectionX, int sectionY, int sectionZ, BlockState state) {
        return this.setBlockState(sectionX, sectionY, sectionZ, state, true);
    }

    public BlockState setBlockState(int sectionX, int sectionY, int sectionZ, BlockState state, boolean checkThreading) {
        BlockState previous = checkThreading ? this.states.getAndSet(sectionX, sectionY, sectionZ, state) : this.states.getAndSetUnchecked(sectionX, sectionY, sectionZ, state);
        if (!previous.isAir()) {
            FluidState previousFluid;
            this.nonEmptyBlockCount = (short)(this.nonEmptyBlockCount - 1);
            if (previous.isRandomlyTicking()) {
                this.tickingBlockCount = (short)(this.tickingBlockCount - 1);
            }
            if (!(previousFluid = previous.getFluidState()).isEmpty()) {
                this.fluidCount = (short)(this.fluidCount - 1);
                if (previousFluid.isRandomlyTicking()) {
                    this.tickingFluidCount = (short)(this.tickingFluidCount - 1);
                }
            }
        }
        if (!state.isAir()) {
            FluidState fluid;
            this.nonEmptyBlockCount = (short)(this.nonEmptyBlockCount + 1);
            if (state.isRandomlyTicking()) {
                this.tickingBlockCount = (short)(this.tickingBlockCount + 1);
            }
            if (!(fluid = state.getFluidState()).isEmpty()) {
                this.fluidCount = (short)(this.fluidCount + 1);
                if (fluid.isRandomlyTicking()) {
                    this.tickingFluidCount = (short)(this.tickingFluidCount + 1);
                }
            }
        }
        return previous;
    }

    public boolean hasOnlyAir() {
        return this.nonEmptyBlockCount == 0;
    }

    public boolean hasFluid() {
        return this.fluidCount > 0;
    }

    public boolean isRandomlyTicking() {
        return this.isRandomlyTickingBlocks() || this.isRandomlyTickingFluids();
    }

    public boolean isRandomlyTickingBlocks() {
        return this.tickingBlockCount > 0;
    }

    public boolean isRandomlyTickingFluids() {
        return this.tickingFluidCount > 0;
    }

    public void recalcBlockCounts() {
        class BlockCounter
        implements PalettedContainer.CountConsumer<BlockState> {
            public int nonEmptyBlockCount;
            public int fluidCount;
            public int tickingBlockCount;
            public int tickingFluidCount;

            BlockCounter(LevelChunkSection this$0) {
                Objects.requireNonNull(this$0);
            }

            @Override
            public void accept(BlockState state, int count) {
                FluidState fluid;
                if (state.isAir()) {
                    return;
                }
                this.nonEmptyBlockCount += count;
                if (state.isRandomlyTicking()) {
                    this.tickingBlockCount += count;
                }
                if (!(fluid = state.getFluidState()).isEmpty()) {
                    this.fluidCount += count;
                    if (fluid.isRandomlyTicking()) {
                        this.tickingFluidCount += count;
                    }
                }
            }
        }
        BlockCounter blockCounter = new BlockCounter(this);
        this.states.count(blockCounter);
        this.nonEmptyBlockCount = (short)blockCounter.nonEmptyBlockCount;
        this.fluidCount = (short)blockCounter.fluidCount;
        this.tickingBlockCount = (short)blockCounter.tickingBlockCount;
        this.tickingFluidCount = (short)blockCounter.tickingFluidCount;
    }

    public PalettedContainer<BlockState> getStates() {
        return this.states;
    }

    public PalettedContainerRO<Holder<Biome>> getBiomes() {
        return this.biomes;
    }

    public void read(FriendlyByteBuf buffer) {
        this.nonEmptyBlockCount = buffer.readShort();
        this.fluidCount = buffer.readShort();
        this.states.read(buffer);
        PalettedContainer<Holder<Biome>> biomes = this.biomes.recreate();
        biomes.read(buffer);
        this.biomes = biomes;
    }

    public void readBiomes(FriendlyByteBuf buffer) {
        PalettedContainer<Holder<Biome>> biomes = this.biomes.recreate();
        biomes.read(buffer);
        this.biomes = biomes;
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeShort(this.nonEmptyBlockCount);
        buffer.writeShort(this.fluidCount);
        this.states.write(buffer);
        this.biomes.write(buffer);
    }

    public int getSerializedSize() {
        return 4 + this.states.getSerializedSize() + this.biomes.getSerializedSize();
    }

    public boolean maybeHas(Predicate<BlockState> predicate) {
        return this.states.maybeHas(predicate);
    }

    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        return this.biomes.get(quartX, quartY, quartZ);
    }

    public void fillBiomesFromNoise(BiomeResolver biomeResolver, Climate.Sampler sampler, int quartMinX, int quartMinY, int quartMinZ) {
        PalettedContainer<Holder<Biome>> newBiomes = this.biomes.recreate();
        int size = 4;
        for (int x = 0; x < 4; ++x) {
            for (int y = 0; y < 4; ++y) {
                for (int z = 0; z < 4; ++z) {
                    newBiomes.getAndSetUnchecked(x, y, z, biomeResolver.getNoiseBiome(quartMinX + x, quartMinY + y, quartMinZ + z, sampler));
                }
            }
        }
        this.biomes = newBiomes;
    }

    public LevelChunkSection copy() {
        return new LevelChunkSection(this);
    }
}

