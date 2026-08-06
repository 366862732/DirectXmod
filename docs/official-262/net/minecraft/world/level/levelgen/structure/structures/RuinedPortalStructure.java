/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.RuinedPortalPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class RuinedPortalStructure
extends Structure {
    private static final String[] STRUCTURE_LOCATION_PORTALS = new String[]{"ruined_portal/portal_1", "ruined_portal/portal_2", "ruined_portal/portal_3", "ruined_portal/portal_4", "ruined_portal/portal_5", "ruined_portal/portal_6", "ruined_portal/portal_7", "ruined_portal/portal_8", "ruined_portal/portal_9", "ruined_portal/portal_10"};
    private static final String[] STRUCTURE_LOCATION_GIANT_PORTALS = new String[]{"ruined_portal/giant_portal_1", "ruined_portal/giant_portal_2", "ruined_portal/giant_portal_3"};
    private static final float PROBABILITY_OF_GIANT_PORTAL = 0.05f;
    private static final int MIN_Y_INDEX = 15;
    private final List<Setup> setups;
    public static final MapCodec<RuinedPortalStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(RuinedPortalStructure.settingsCodec(i), (App)ExtraCodecs.nonEmptyList(Setup.CODEC.listOf()).fieldOf("setups").forGetter(s -> s.setups)).apply((Applicative)i, RuinedPortalStructure::new));

    public RuinedPortalStructure(Structure.StructureSettings settings, List<Setup> setups) {
        super(settings);
        this.setups = setups;
    }

    public RuinedPortalStructure(Structure.StructureSettings settings, Setup setup) {
        this(settings, List.of(setup));
    }

    /*
     * WARNING - void declaration
     */
    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        void var6_13;
        WorldgenRandom random = context.random();
        Setup chosenSetup = null;
        if (this.setups.size() > 1) {
            float total = 0.0f;
            for (Setup setup : this.setups) {
                total += setup.weight();
            }
            float pick = random.nextFloat();
            for (Setup s : this.setups) {
                if (!((pick -= s.weight() / total) < 0.0f)) continue;
                chosenSetup = s;
                break;
            }
        } else {
            chosenSetup = this.setups.get(0);
        }
        if (chosenSetup == null) {
            throw new IllegalStateException();
        }
        Setup setup = chosenSetup;
        boolean airPocket = RuinedPortalStructure.sample(random, setup.airPocketProbability());
        if (random.nextFloat() < 0.05f) {
            Identifier identifier = Identifier.withDefaultNamespace(STRUCTURE_LOCATION_GIANT_PORTALS[random.nextInt(STRUCTURE_LOCATION_GIANT_PORTALS.length)]);
        } else {
            Identifier identifier = Identifier.withDefaultNamespace(STRUCTURE_LOCATION_PORTALS[random.nextInt(STRUCTURE_LOCATION_PORTALS.length)]);
        }
        StructureTemplate template = context.structureTemplateManager().getOrCreate((Identifier)var6_13);
        Rotation rotation = Util.getRandom(Rotation.values(), (RandomSource)random);
        Mirror mirror = random.nextFloat() < 0.5f ? Mirror.NONE : Mirror.FRONT_BACK;
        BlockPos pivot = new BlockPos(template.getSize().getX() / 2, 0, template.getSize().getZ() / 2);
        ChunkGenerator chunkGenerator = context.chunkGenerator();
        LevelHeightAccessor heightAccessor = context.heightAccessor();
        RandomState randomState = context.randomState();
        BlockPos basePosition = context.chunkPos().getWorldPosition();
        BoundingBox boundingBox = template.getBoundingBox(basePosition, rotation, pivot, mirror);
        BlockPos center = boundingBox.getCenter();
        int surfaceY = chunkGenerator.getBaseHeight(center.getX(), center.getZ(), RuinedPortalPiece.getHeightMapType(setup.placement()), heightAccessor, randomState) - 1;
        int projectedY = RuinedPortalStructure.findSuitableY(random, chunkGenerator, setup.placement(), airPocket, surfaceY, boundingBox.getYSpan(), boundingBox, heightAccessor, randomState);
        BlockPos origin = new BlockPos(basePosition.getX(), projectedY, basePosition.getZ());
        return Optional.of(new Structure.GenerationStub(origin, arg_0 -> RuinedPortalStructure.lambda$findGenerationPoint$0(setup, origin, context, randomState, chunkGenerator, airPocket, (Identifier)var6_13, template, rotation, mirror, pivot, arg_0)));
    }

    private static boolean sample(WorldgenRandom random, float limit) {
        if (limit == 0.0f) {
            return false;
        }
        if (limit == 1.0f) {
            return true;
        }
        return random.nextFloat() < limit;
    }

    private static boolean isCold(BlockPos pos, Holder<Biome> biome, int seaLevel) {
        return biome.value().coldEnoughToSnow(pos, seaLevel);
    }

    private static int findSuitableY(RandomSource random, ChunkGenerator generator, RuinedPortalPiece.VerticalPlacement verticalPlacement, boolean airPocket, int surfaceYAtCenter, int ySpan, BoundingBox boundingBox, LevelHeightAccessor heightAccessor, RandomState randomState) {
        int projectedY;
        int minY = heightAccessor.getMinY() + 15;
        if (verticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_NETHER) {
            newY = airPocket ? Mth.randomBetweenInclusive(random, 32, 100) : (random.nextFloat() < 0.5f ? Mth.randomBetweenInclusive(random, 27, 29) : Mth.randomBetweenInclusive(random, 29, 100));
        } else if (verticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_MOUNTAIN) {
            maxY = surfaceYAtCenter - ySpan;
            newY = RuinedPortalStructure.getRandomWithinInterval(random, 70, maxY);
        } else if (verticalPlacement == RuinedPortalPiece.VerticalPlacement.UNDERGROUND) {
            maxY = surfaceYAtCenter - ySpan;
            newY = RuinedPortalStructure.getRandomWithinInterval(random, minY, maxY);
        } else {
            newY = verticalPlacement == RuinedPortalPiece.VerticalPlacement.PARTLY_BURIED ? surfaceYAtCenter - ySpan + Mth.randomBetweenInclusive(random, 2, 8) : surfaceYAtCenter;
        }
        ImmutableList bottomCorners = ImmutableList.of((Object)new BlockPos(boundingBox.minX(), 0, boundingBox.minZ()), (Object)new BlockPos(boundingBox.maxX(), 0, boundingBox.minZ()), (Object)new BlockPos(boundingBox.minX(), 0, boundingBox.maxZ()), (Object)new BlockPos(boundingBox.maxX(), 0, boundingBox.maxZ()));
        List columns = bottomCorners.stream().map(p -> generator.getBaseColumn(p.getX(), p.getZ(), heightAccessor, randomState)).collect(Collectors.toList());
        Heightmap.Types heightmap = verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
        block0: for (projectedY = newY; projectedY > minY; --projectedY) {
            int cornersOnSolidGround = 0;
            for (NoiseColumn column : columns) {
                BlockState blockState = column.getBlock(projectedY);
                if (!heightmap.isOpaque().test(blockState) || ++cornersOnSolidGround != 3) continue;
                break block0;
            }
        }
        return projectedY;
    }

    private static int getRandomWithinInterval(RandomSource random, int minPreferred, int max) {
        if (minPreferred < max) {
            return Mth.randomBetweenInclusive(random, minPreferred, max);
        }
        return max;
    }

    @Override
    public StructureType<?> type() {
        return StructureType.RUINED_PORTAL;
    }

    public record Setup(RuinedPortalPiece.VerticalPlacement placement, float airPocketProbability, float mossiness, boolean overgrown, boolean vines, boolean canBeCold, boolean replaceWithBlackstone, float weight) {
        public static final Codec<Setup> CODEC = RecordCodecBuilder.create(i -> i.group((App)RuinedPortalPiece.VerticalPlacement.CODEC.fieldOf("placement").forGetter(Setup::placement), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("air_pocket_probability").forGetter(Setup::airPocketProbability), (App)Codec.floatRange((float)0.0f, (float)1.0f).fieldOf("mossiness").forGetter(Setup::mossiness), (App)Codec.BOOL.fieldOf("overgrown").forGetter(Setup::overgrown), (App)Codec.BOOL.fieldOf("vines").forGetter(Setup::vines), (App)Codec.BOOL.fieldOf("can_be_cold").forGetter(Setup::canBeCold), (App)Codec.BOOL.fieldOf("replace_with_blackstone").forGetter(Setup::replaceWithBlackstone), (App)ExtraCodecs.POSITIVE_FLOAT.fieldOf("weight").forGetter(Setup::weight)).apply((Applicative)i, Setup::new));
    }
}

