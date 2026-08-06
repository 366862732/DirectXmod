/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.minecart.MinecartChest;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.structures.MineshaftStructure;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public class MineshaftPieces {
    private static final int DEFAULT_SHAFT_WIDTH = 3;
    private static final int DEFAULT_SHAFT_HEIGHT = 3;
    private static final int DEFAULT_SHAFT_LENGTH = 5;
    private static final int MAX_PILLAR_HEIGHT = 20;
    private static final int MAX_CHAIN_HEIGHT = 50;
    private static final int MAX_DEPTH = 8;
    public static final int MAGIC_START_Y = 50;

    private static @Nullable MineShaftPiece createRandomShaftPiece(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int genDepth, MineshaftStructure.Type type) {
        int randomSelection = random.nextInt(100);
        if (randomSelection >= 80) {
            BoundingBox crossingBox = MineShaftCrossing.findCrossing(structurePieceAccessor, random, footX, footY, footZ, direction);
            if (crossingBox != null) {
                return new MineShaftCrossing(genDepth, crossingBox, direction, type);
            }
        } else if (randomSelection >= 70) {
            BoundingBox stairsBox = MineShaftStairs.findStairs(structurePieceAccessor, random, footX, footY, footZ, direction);
            if (stairsBox != null) {
                return new MineShaftStairs(genDepth, stairsBox, direction, type);
            }
        } else {
            BoundingBox corridorBox = MineShaftCorridor.findCorridorSize(structurePieceAccessor, random, footX, footY, footZ, direction);
            if (corridorBox != null) {
                return new MineShaftCorridor(genDepth, random, corridorBox, direction, type);
            }
        }
        return null;
    }

    private static @Nullable MineShaftPiece generateAndAddPiece(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int depth) {
        if (depth > 8) {
            return null;
        }
        if (Math.abs(footX - startPiece.getBoundingBox().minX()) > 80 || Math.abs(footZ - startPiece.getBoundingBox().minZ()) > 80) {
            return null;
        }
        MineshaftStructure.Type type = ((MineShaftPiece)startPiece).type;
        MineShaftPiece newPiece = MineshaftPieces.createRandomShaftPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth + 1, type);
        if (newPiece != null) {
            structurePieceAccessor.addPiece(newPiece);
            newPiece.addChildren(startPiece, structurePieceAccessor, random);
        }
        return newPiece;
    }

    public static class MineShaftCrossing
    extends MineShaftPiece {
        private final Direction direction;
        private final boolean isTwoFloored;

        public MineShaftCrossing(CompoundTag tag) {
            super(StructurePieceType.MINE_SHAFT_CROSSING, tag);
            this.isTwoFloored = tag.getBooleanOr("tf", false);
            this.direction = tag.read("D", Direction.LEGACY_ID_CODEC_2D).orElse(Direction.SOUTH);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putBoolean("tf", this.isTwoFloored);
            tag.store("D", Direction.LEGACY_ID_CODEC_2D, this.direction);
        }

        public MineShaftCrossing(int genDepth, BoundingBox boundingBox, @Nullable Direction direction, MineshaftStructure.Type type) {
            super(StructurePieceType.MINE_SHAFT_CROSSING, genDepth, type, boundingBox);
            this.direction = direction;
            this.isTwoFloored = boundingBox.getYSpan() > 3;
        }

        public static @Nullable BoundingBox findCrossing(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction) {
            int y1 = random.nextInt(4) == 0 ? 6 : 2;
            BoundingBox box = switch (direction) {
                default -> new BoundingBox(-1, 0, -4, 3, y1, 0);
                case Direction.SOUTH -> new BoundingBox(-1, 0, 0, 3, y1, 4);
                case Direction.WEST -> new BoundingBox(-4, 0, -1, 0, y1, 3);
                case Direction.EAST -> new BoundingBox(0, 0, -1, 4, y1, 3);
            };
            box.move(footX, footY, footZ);
            if (structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return box;
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            int depth = this.getGenDepth();
            switch (this.direction) {
                default: {
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, depth);
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, depth);
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, depth);
                    break;
                }
                case SOUTH: {
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, depth);
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, depth);
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, depth);
                    break;
                }
                case WEST: {
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, depth);
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, depth);
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.WEST, depth);
                    break;
                }
                case EAST: {
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, depth);
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, depth);
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, Direction.EAST, depth);
                }
            }
            if (this.isTwoFloored) {
                if (random.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() - 1, Direction.NORTH, depth);
                }
                if (random.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, Direction.WEST, depth);
                }
                if (random.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.minZ() + 1, Direction.EAST, depth);
                }
                if (random.nextBoolean()) {
                    MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3 + 1, this.boundingBox.maxZ() + 1, Direction.SOUTH, depth);
                }
            }
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            if (this.isInInvalidLocation(level, chunkBB)) {
                return;
            }
            BlockState planks = this.type.getPlanksState();
            if (this.isTwoFloored) {
                this.generateBox(level, chunkBB, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.minY() + 3 - 1, this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                this.generateBox(level, chunkBB, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.minY() + 3 - 1, this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                this.generateBox(level, chunkBB, this.boundingBox.minX() + 1, this.boundingBox.maxY() - 2, this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                this.generateBox(level, chunkBB, this.boundingBox.minX(), this.boundingBox.maxY() - 2, this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
                this.generateBox(level, chunkBB, this.boundingBox.minX() + 1, this.boundingBox.minY() + 3, this.boundingBox.minZ() + 1, this.boundingBox.maxX() - 1, this.boundingBox.minY() + 3, this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
            } else {
                this.generateBox(level, chunkBB, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), this.boundingBox.maxX() - 1, this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
                this.generateBox(level, chunkBB, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ() - 1, CAVE_AIR, CAVE_AIR, false);
            }
            this.placeSupportPillar(level, chunkBB, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
            this.placeSupportPillar(level, chunkBB, this.boundingBox.minX() + 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
            this.placeSupportPillar(level, chunkBB, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.minZ() + 1, this.boundingBox.maxY());
            this.placeSupportPillar(level, chunkBB, this.boundingBox.maxX() - 1, this.boundingBox.minY(), this.boundingBox.maxZ() - 1, this.boundingBox.maxY());
            int y = this.boundingBox.minY() - 1;
            for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); ++x) {
                for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); ++z) {
                    this.setPlanksBlock(level, chunkBB, planks, x, y, z);
                }
            }
        }

        private void placeSupportPillar(WorldGenLevel level, BoundingBox chunkBB, int x, int y0, int z, int y1) {
            if (!this.getBlock(level, x, y1 + 1, z, chunkBB).isAir()) {
                this.generateBox(level, chunkBB, x, y0, z, x, y1, z, this.type.getPlanksState(), CAVE_AIR, false);
            }
        }
    }

    public static class MineShaftStairs
    extends MineShaftPiece {
        public MineShaftStairs(int genDepth, BoundingBox boundingBox, Direction direction, MineshaftStructure.Type type) {
            super(StructurePieceType.MINE_SHAFT_STAIRS, genDepth, type, boundingBox);
            this.setOrientation(direction);
        }

        public MineShaftStairs(CompoundTag tag) {
            super(StructurePieceType.MINE_SHAFT_STAIRS, tag);
        }

        public static @Nullable BoundingBox findStairs(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction) {
            BoundingBox box = switch (direction) {
                default -> new BoundingBox(0, -5, -8, 2, 2, 0);
                case Direction.SOUTH -> new BoundingBox(0, -5, 0, 2, 2, 8);
                case Direction.WEST -> new BoundingBox(-8, -5, 0, 0, 2, 2);
                case Direction.EAST -> new BoundingBox(0, -5, 0, 8, 2, 2);
            };
            box.move(footX, footY, footZ);
            if (structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return box;
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            int depth = this.getGenDepth();
            Direction orientation = this.getOrientation();
            if (orientation != null) {
                switch (orientation) {
                    default: {
                        MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, depth);
                        break;
                    }
                    case SOUTH: {
                        MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, depth);
                        break;
                    }
                    case WEST: {
                        MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.WEST, depth);
                        break;
                    }
                    case EAST: {
                        MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), this.boundingBox.minZ(), Direction.EAST, depth);
                    }
                }
            }
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            if (this.isInInvalidLocation(level, chunkBB)) {
                return;
            }
            this.generateBox(level, chunkBB, 0, 5, 0, 2, 7, 1, CAVE_AIR, CAVE_AIR, false);
            this.generateBox(level, chunkBB, 0, 0, 7, 2, 2, 8, CAVE_AIR, CAVE_AIR, false);
            for (int i = 0; i < 5; ++i) {
                this.generateBox(level, chunkBB, 0, 5 - i - (i < 4 ? 1 : 0), 2 + i, 2, 7 - i, 2 + i, CAVE_AIR, CAVE_AIR, false);
            }
        }
    }

    public static class MineShaftCorridor
    extends MineShaftPiece {
        private final boolean hasRails;
        private final boolean spiderCorridor;
        private boolean hasPlacedSpider;
        private final int numSections;

        public MineShaftCorridor(CompoundTag tag) {
            super(StructurePieceType.MINE_SHAFT_CORRIDOR, tag);
            this.hasRails = tag.getBooleanOr("hr", false);
            this.spiderCorridor = tag.getBooleanOr("sc", false);
            this.hasPlacedSpider = tag.getBooleanOr("hps", false);
            this.numSections = tag.getIntOr("Num", 0);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putBoolean("hr", this.hasRails);
            tag.putBoolean("sc", this.spiderCorridor);
            tag.putBoolean("hps", this.hasPlacedSpider);
            tag.putInt("Num", this.numSections);
        }

        public MineShaftCorridor(int genDepth, RandomSource random, BoundingBox boundingBox, Direction direction, MineshaftStructure.Type type) {
            super(StructurePieceType.MINE_SHAFT_CORRIDOR, genDepth, type, boundingBox);
            this.setOrientation(direction);
            this.hasRails = random.nextInt(3) == 0;
            this.spiderCorridor = !this.hasRails && random.nextInt(23) == 0;
            this.numSections = this.getOrientation().getAxis() == Direction.Axis.Z ? boundingBox.getZSpan() / 5 : boundingBox.getXSpan() / 5;
        }

        /*
         * Enabled aggressive block sorting
         */
        public static @Nullable BoundingBox findCorridorSize(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction) {
            int corridorLength = random.nextInt(3) + 2;
            while (corridorLength > 0) {
                int blockLength = corridorLength * 5;
                BoundingBox box = switch (direction) {
                    default -> new BoundingBox(0, 0, -(blockLength - 1), 2, 2, 0);
                    case Direction.SOUTH -> new BoundingBox(0, 0, 0, 2, 2, blockLength - 1);
                    case Direction.WEST -> new BoundingBox(-(blockLength - 1), 0, 0, 0, 2, 2);
                    case Direction.EAST -> new BoundingBox(0, 0, 0, blockLength - 1, 2, 2);
                };
                box.move(footX, footY, footZ);
                if (structurePieceAccessor.findCollisionPiece(box) == null) {
                    return box;
                }
                --corridorLength;
            }
            return null;
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            block24: {
                int depth = this.getGenDepth();
                int endSelection = random.nextInt(4);
                Direction orientation = this.getOrientation();
                if (orientation != null) {
                    switch (orientation) {
                        default: {
                            if (endSelection <= 1) {
                                MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, orientation, depth);
                                break;
                            }
                            if (endSelection == 2) {
                                MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), Direction.WEST, depth);
                                break;
                            }
                            MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), Direction.EAST, depth);
                            break;
                        }
                        case SOUTH: {
                            if (endSelection <= 1) {
                                MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, orientation, depth);
                                break;
                            }
                            if (endSelection == 2) {
                                MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() - 3, Direction.WEST, depth);
                                break;
                            }
                            MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() - 3, Direction.EAST, depth);
                            break;
                        }
                        case WEST: {
                            if (endSelection <= 1) {
                                MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), orientation, depth);
                                break;
                            }
                            if (endSelection == 2) {
                                MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, Direction.NORTH, depth);
                                break;
                            }
                            MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX(), this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, Direction.SOUTH, depth);
                            break;
                        }
                        case EAST: {
                            if (endSelection <= 1) {
                                MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ(), orientation, depth);
                                break;
                            }
                            if (endSelection == 2) {
                                MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() - 3, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.minZ() - 1, Direction.NORTH, depth);
                                break;
                            }
                            MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() - 3, this.boundingBox.minY() - 1 + random.nextInt(3), this.boundingBox.maxZ() + 1, Direction.SOUTH, depth);
                        }
                    }
                }
                if (depth >= 8) break block24;
                if (orientation == Direction.NORTH || orientation == Direction.SOUTH) {
                    int z = this.boundingBox.minZ() + 3;
                    while (z + 3 <= this.boundingBox.maxZ()) {
                        int selection = random.nextInt(5);
                        if (selection == 0) {
                            MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY(), z, Direction.WEST, depth + 1);
                        } else if (selection == 1) {
                            MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY(), z, Direction.EAST, depth + 1);
                        }
                        z += 5;
                    }
                } else {
                    int x = this.boundingBox.minX() + 3;
                    while (x + 3 <= this.boundingBox.maxX()) {
                        int selection = random.nextInt(5);
                        if (selection == 0) {
                            MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, x, this.boundingBox.minY(), this.boundingBox.minZ() - 1, Direction.NORTH, depth + 1);
                        } else if (selection == 1) {
                            MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, x, this.boundingBox.minY(), this.boundingBox.maxZ() + 1, Direction.SOUTH, depth + 1);
                        }
                        x += 5;
                    }
                }
            }
        }

        @Override
        protected boolean createChest(WorldGenLevel level, BoundingBox chunkBB, RandomSource random, int x, int y, int z, ResourceKey<LootTable> lootTable) {
            BlockPos.MutableBlockPos pos = this.getWorldPos(x, y, z);
            if (chunkBB.isInside(pos) && level.getBlockState(pos).isAir() && !level.getBlockState(((BlockPos)pos).below()).isAir()) {
                BlockState state = (BlockState)Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, random.nextBoolean() ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
                this.placeBlock(level, state, x, y, z, chunkBB);
                MinecartChest chest = EntityTypes.CHEST_MINECART.create((Level)level.getLevel(), EntitySpawnReason.CHUNK_GENERATION);
                if (chest != null) {
                    chest.setInitialPos((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
                    chest.setLootTable(lootTable, random.nextLong());
                    level.addFreshEntity(chest);
                }
                return true;
            }
            return false;
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            int z;
            if (this.isInInvalidLocation(level, chunkBB)) {
                return;
            }
            boolean x0 = false;
            int x1 = 2;
            boolean y0 = false;
            int y1 = 2;
            int length = this.numSections * 5 - 1;
            BlockState planks = this.type.getPlanksState();
            this.generateBox(level, chunkBB, 0, 0, 0, 2, 1, length, CAVE_AIR, CAVE_AIR, false);
            this.generateMaybeBox(level, chunkBB, random, 0.8f, 0, 2, 0, 2, 2, length, CAVE_AIR, CAVE_AIR, false, false);
            if (this.spiderCorridor) {
                this.generateMaybeBox(level, chunkBB, random, 0.6f, 0, 0, 0, 2, 1, length, Blocks.COBWEB.defaultBlockState(), CAVE_AIR, false, true);
            }
            for (int section = 0; section < this.numSections; ++section) {
                z = 2 + section * 5;
                this.placeSupport(level, chunkBB, 0, 0, z, 2, 2, random);
                this.maybePlaceCobWeb(level, chunkBB, random, 0.1f, 0, 2, z - 1);
                this.maybePlaceCobWeb(level, chunkBB, random, 0.1f, 2, 2, z - 1);
                this.maybePlaceCobWeb(level, chunkBB, random, 0.1f, 0, 2, z + 1);
                this.maybePlaceCobWeb(level, chunkBB, random, 0.1f, 2, 2, z + 1);
                this.maybePlaceCobWeb(level, chunkBB, random, 0.05f, 0, 2, z - 2);
                this.maybePlaceCobWeb(level, chunkBB, random, 0.05f, 2, 2, z - 2);
                this.maybePlaceCobWeb(level, chunkBB, random, 0.05f, 0, 2, z + 2);
                this.maybePlaceCobWeb(level, chunkBB, random, 0.05f, 2, 2, z + 2);
                if (random.nextInt(100) == 0) {
                    this.createChest(level, chunkBB, random, 2, 0, z - 1, BuiltInLootTables.ABANDONED_MINESHAFT);
                }
                if (random.nextInt(100) == 0) {
                    this.createChest(level, chunkBB, random, 0, 0, z + 1, BuiltInLootTables.ABANDONED_MINESHAFT);
                }
                if (!this.spiderCorridor || this.hasPlacedSpider) continue;
                boolean newX = true;
                int newZ = z - 1 + random.nextInt(3);
                BlockPos.MutableBlockPos pos = this.getWorldPos(1, 0, newZ);
                if (!chunkBB.isInside(pos) || !this.isInterior(level, 1, 0, newZ, chunkBB)) continue;
                this.hasPlacedSpider = true;
                level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (!(blockEntity instanceof SpawnerBlockEntity)) continue;
                SpawnerBlockEntity spawner = (SpawnerBlockEntity)blockEntity;
                spawner.setEntityId(EntityTypes.CAVE_SPIDER, random);
            }
            for (int x = 0; x <= 2; ++x) {
                for (z = 0; z <= length; ++z) {
                    this.setPlanksBlock(level, chunkBB, planks, x, -1, z);
                }
            }
            int supportPillarIndent = 2;
            this.placeDoubleLowerOrUpperSupport(level, chunkBB, 0, -1, 2);
            if (this.numSections > 1) {
                int lastSupportPillar = length - 2;
                this.placeDoubleLowerOrUpperSupport(level, chunkBB, 0, -1, lastSupportPillar);
            }
            if (this.hasRails) {
                BlockState state = (BlockState)Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.NORTH_SOUTH);
                for (int z2 = 0; z2 <= length; ++z2) {
                    BlockState floor = this.getBlock(level, 1, -1, z2, chunkBB);
                    if (floor.isAir() || !floor.isSolidRender()) continue;
                    float probability = this.isInterior(level, 1, 0, z2, chunkBB) ? 0.7f : 0.9f;
                    this.maybeGenerateBlock(level, chunkBB, random, probability, 1, 0, z2, state);
                }
            }
        }

        private void placeDoubleLowerOrUpperSupport(WorldGenLevel level, BoundingBox chunkBB, int x, int y, int z) {
            BlockState woodBlock = this.type.getWoodState();
            BlockState plankBlock = this.type.getPlanksState();
            if (this.getBlock(level, x, y, z, chunkBB).is(plankBlock.getBlock())) {
                this.fillPillarDownOrChainUp(level, woodBlock, x, y, z, chunkBB);
            }
            if (this.getBlock(level, x + 2, y, z, chunkBB).is(plankBlock.getBlock())) {
                this.fillPillarDownOrChainUp(level, woodBlock, x + 2, y, z, chunkBB);
            }
        }

        @Override
        protected void fillColumnDown(WorldGenLevel level, BlockState columnState, int x, int startY, int z, BoundingBox chunkBB) {
            BlockPos.MutableBlockPos pos = this.getWorldPos(x, startY, z);
            if (!chunkBB.isInside(pos)) {
                return;
            }
            int worldY = pos.getY();
            while (this.isReplaceableByStructures(level.getBlockState(pos)) && pos.getY() > level.getMinY() + 1) {
                pos.move(Direction.DOWN);
            }
            if (!this.canPlaceColumnOnTopOf(level, pos, level.getBlockState(pos))) {
                return;
            }
            while (pos.getY() < worldY) {
                pos.move(Direction.UP);
                level.setBlock(pos, columnState, 2);
            }
        }

        protected void fillPillarDownOrChainUp(WorldGenLevel level, BlockState pillarState, int x, int y, int z, BoundingBox chunkBB) {
            BlockPos.MutableBlockPos pos = this.getWorldPos(x, y, z);
            if (!chunkBB.isInside(pos)) {
                return;
            }
            int worldY = pos.getY();
            int distanceFromWorldY = 1;
            boolean checkBelow = true;
            boolean checkAbove = true;
            while (checkBelow || checkAbove) {
                if (checkBelow) {
                    boolean emptyBelow;
                    pos.setY(worldY - distanceFromWorldY);
                    BlockState belowState = level.getBlockState(pos);
                    boolean bl = emptyBelow = this.isReplaceableByStructures(belowState) && !belowState.is(Blocks.LAVA);
                    if (!emptyBelow && this.canPlaceColumnOnTopOf(level, pos, belowState)) {
                        MineShaftCorridor.fillColumnBetween(level, pillarState, pos, worldY - distanceFromWorldY + 1, worldY);
                        return;
                    }
                    boolean bl2 = checkBelow = distanceFromWorldY <= 20 && emptyBelow && pos.getY() > level.getMinY() + 1;
                }
                if (checkAbove) {
                    pos.setY(worldY + distanceFromWorldY);
                    BlockState aboveState = level.getBlockState(pos);
                    boolean emptyAbove = this.isReplaceableByStructures(aboveState);
                    if (!emptyAbove && this.canHangChainBelow(level, pos, aboveState)) {
                        level.setBlock(pos.setY(worldY + 1), this.type.getFenceState(), 2);
                        MineShaftCorridor.fillColumnBetween(level, Blocks.IRON_CHAIN.defaultBlockState(), pos, worldY + 2, worldY + distanceFromWorldY);
                        return;
                    }
                    checkAbove = distanceFromWorldY <= 50 && emptyAbove && pos.getY() < level.getMaxY();
                }
                ++distanceFromWorldY;
            }
        }

        private static void fillColumnBetween(WorldGenLevel level, BlockState pillarState, BlockPos.MutableBlockPos pos, int bottomInclusive, int topExclusive) {
            for (int pillarY = bottomInclusive; pillarY < topExclusive; ++pillarY) {
                level.setBlock(pos.setY(pillarY), pillarState, 2);
            }
        }

        private boolean canPlaceColumnOnTopOf(LevelReader level, BlockPos posBelow, BlockState stateBelow) {
            return stateBelow.isFaceSturdy(level, posBelow, Direction.UP);
        }

        private boolean canHangChainBelow(LevelReader level, BlockPos posAbove, BlockState stateAbove) {
            return Block.canSupportCenter(level, posAbove, Direction.DOWN) && !(stateAbove.getBlock() instanceof FallingBlock);
        }

        private void placeSupport(WorldGenLevel level, BoundingBox chunkBB, int x0, int y0, int z, int y1, int x1, RandomSource random) {
            if (!this.isSupportingBox(level, chunkBB, x0, x1, y1, z)) {
                return;
            }
            BlockState planksBlock = this.type.getPlanksState();
            BlockState fenceBlock = this.type.getFenceState();
            this.generateBox(level, chunkBB, x0, y0, z, x0, y1 - 1, z, (BlockState)fenceBlock.setValue(FenceBlock.WEST, true), CAVE_AIR, false);
            this.generateBox(level, chunkBB, x1, y0, z, x1, y1 - 1, z, (BlockState)fenceBlock.setValue(FenceBlock.EAST, true), CAVE_AIR, false);
            if (random.nextInt(4) == 0) {
                this.generateBox(level, chunkBB, x0, y1, z, x0, y1, z, planksBlock, CAVE_AIR, false);
                this.generateBox(level, chunkBB, x1, y1, z, x1, y1, z, planksBlock, CAVE_AIR, false);
            } else {
                this.generateBox(level, chunkBB, x0, y1, z, x1, y1, z, planksBlock, CAVE_AIR, false);
                this.maybeGenerateBlock(level, chunkBB, random, 0.05f, x0 + 1, y1, z - 1, (BlockState)Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
                this.maybeGenerateBlock(level, chunkBB, random, 0.05f, x0 + 1, y1, z + 1, (BlockState)Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH));
            }
        }

        private void maybePlaceCobWeb(WorldGenLevel level, BoundingBox chunkBB, RandomSource random, float probability, int x, int y, int z) {
            if (this.isInterior(level, x, y, z, chunkBB) && random.nextFloat() < probability && this.hasSturdyNeighbours(level, chunkBB, x, y, z, 2)) {
                this.placeBlock(level, Blocks.COBWEB.defaultBlockState(), x, y, z, chunkBB);
            }
        }

        private boolean hasSturdyNeighbours(WorldGenLevel level, BoundingBox chunkBB, int x, int y, int z, int count) {
            BlockPos.MutableBlockPos worldPos = this.getWorldPos(x, y, z);
            int sturdyNeighbours = 0;
            for (Direction direction : Direction.values()) {
                worldPos.move(direction);
                if (chunkBB.isInside(worldPos) && level.getBlockState(worldPos).isFaceSturdy(level, worldPos, direction.getOpposite()) && ++sturdyNeighbours >= count) {
                    return true;
                }
                worldPos.move(direction.getOpposite());
            }
            return false;
        }
    }

    private static abstract class MineShaftPiece
    extends StructurePiece {
        protected MineshaftStructure.Type type;

        public MineShaftPiece(StructurePieceType pieceType, int genDepth, MineshaftStructure.Type type, BoundingBox boundingBox) {
            super(pieceType, genDepth, boundingBox);
            this.type = type;
        }

        public MineShaftPiece(StructurePieceType type, CompoundTag tag) {
            super(type, tag);
            this.type = MineshaftStructure.Type.byId(tag.getIntOr("MST", 0));
        }

        @Override
        protected boolean canBeReplaced(LevelReader level, int x, int y, int z, BoundingBox chunkBB) {
            BlockState state = this.getBlock(level, x, y, z, chunkBB);
            return !state.is(this.type.getPlanksState().getBlock()) && !state.is(this.type.getWoodState().getBlock()) && !state.is(this.type.getFenceState().getBlock()) && !state.is(Blocks.IRON_CHAIN);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("MST", this.type.ordinal());
        }

        protected boolean isSupportingBox(BlockGetter level, BoundingBox chunkBB, int x0, int x1, int y1, int z0) {
            for (int x = x0; x <= x1; ++x) {
                if (!this.getBlock(level, x, y1 + 1, z0, chunkBB).isAir()) continue;
                return false;
            }
            return true;
        }

        protected boolean isInInvalidLocation(LevelAccessor level, BoundingBox chunkBB) {
            int y;
            int x;
            int z1;
            int y1;
            int x0 = Math.max(this.boundingBox.minX() - 1, chunkBB.minX());
            int y0 = Math.max(this.boundingBox.minY() - 1, chunkBB.minY());
            int z0 = Math.max(this.boundingBox.minZ() - 1, chunkBB.minZ());
            int x1 = Math.min(this.boundingBox.maxX() + 1, chunkBB.maxX());
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos((x0 + x1) / 2, (y0 + (y1 = Math.min(this.boundingBox.maxY() + 1, chunkBB.maxY()))) / 2, (z0 + (z1 = Math.min(this.boundingBox.maxZ() + 1, chunkBB.maxZ()))) / 2);
            if (level.getBiome(blockPos).is(BiomeTags.MINESHAFT_BLOCKING)) {
                return true;
            }
            for (x = x0; x <= x1; ++x) {
                for (int z = z0; z <= z1; ++z) {
                    if (level.getBlockState(blockPos.set(x, y0, z)).liquid()) {
                        return true;
                    }
                    if (!level.getBlockState(blockPos.set(x, y1, z)).liquid()) continue;
                    return true;
                }
            }
            for (x = x0; x <= x1; ++x) {
                for (y = y0; y <= y1; ++y) {
                    if (level.getBlockState(blockPos.set(x, y, z0)).liquid()) {
                        return true;
                    }
                    if (!level.getBlockState(blockPos.set(x, y, z1)).liquid()) continue;
                    return true;
                }
            }
            for (int z = z0; z <= z1; ++z) {
                for (y = y0; y <= y1; ++y) {
                    if (level.getBlockState(blockPos.set(x0, y, z)).liquid()) {
                        return true;
                    }
                    if (!level.getBlockState(blockPos.set(x1, y, z)).liquid()) continue;
                    return true;
                }
            }
            return false;
        }

        protected void setPlanksBlock(WorldGenLevel level, BoundingBox chunkBB, BlockState planksBlock, int x, int y, int z) {
            if (!this.isInterior(level, x, y, z, chunkBB)) {
                return;
            }
            BlockPos.MutableBlockPos pos = this.getWorldPos(x, y, z);
            BlockState existingState = level.getBlockState(pos);
            if (!existingState.isFaceSturdy(level, pos, Direction.UP)) {
                level.setBlock(pos, planksBlock, 2);
            }
        }
    }

    public static class MineShaftRoom
    extends MineShaftPiece {
        private final List<BoundingBox> childEntranceBoxes = Lists.newLinkedList();

        public MineShaftRoom(int genDepth, RandomSource random, int west, int north, MineshaftStructure.Type type) {
            super(StructurePieceType.MINE_SHAFT_ROOM, genDepth, type, new BoundingBox(west, 50, north, west + 7 + random.nextInt(6), 54 + random.nextInt(6), north + 7 + random.nextInt(6)));
            this.type = type;
        }

        public MineShaftRoom(CompoundTag tag) {
            super(StructurePieceType.MINE_SHAFT_ROOM, tag);
            this.childEntranceBoxes.addAll(tag.read("Entrances", BoundingBox.CODEC.listOf()).orElse(List.of()));
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            BoundingBox childBox;
            MineShaftPiece child;
            int pos;
            int depth = this.getGenDepth();
            int heightSpace = this.boundingBox.getYSpan() - 3 - 1;
            if (heightSpace <= 0) {
                heightSpace = 1;
            }
            for (pos = 0; pos < this.boundingBox.getXSpan() && (pos += random.nextInt(this.boundingBox.getXSpan())) + 3 <= this.boundingBox.getXSpan(); pos += 4) {
                child = MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + pos, this.boundingBox.minY() + random.nextInt(heightSpace) + 1, this.boundingBox.minZ() - 1, Direction.NORTH, depth);
                if (child == null) continue;
                childBox = child.getBoundingBox();
                this.childEntranceBoxes.add(new BoundingBox(childBox.minX(), childBox.minY(), this.boundingBox.minZ(), childBox.maxX(), childBox.maxY(), this.boundingBox.minZ() + 1));
            }
            for (pos = 0; pos < this.boundingBox.getXSpan() && (pos += random.nextInt(this.boundingBox.getXSpan())) + 3 <= this.boundingBox.getXSpan(); pos += 4) {
                child = MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + pos, this.boundingBox.minY() + random.nextInt(heightSpace) + 1, this.boundingBox.maxZ() + 1, Direction.SOUTH, depth);
                if (child == null) continue;
                childBox = child.getBoundingBox();
                this.childEntranceBoxes.add(new BoundingBox(childBox.minX(), childBox.minY(), this.boundingBox.maxZ() - 1, childBox.maxX(), childBox.maxY(), this.boundingBox.maxZ()));
            }
            for (pos = 0; pos < this.boundingBox.getZSpan() && (pos += random.nextInt(this.boundingBox.getZSpan())) + 3 <= this.boundingBox.getZSpan(); pos += 4) {
                child = MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + random.nextInt(heightSpace) + 1, this.boundingBox.minZ() + pos, Direction.WEST, depth);
                if (child == null) continue;
                childBox = child.getBoundingBox();
                this.childEntranceBoxes.add(new BoundingBox(this.boundingBox.minX(), childBox.minY(), childBox.minZ(), this.boundingBox.minX() + 1, childBox.maxY(), childBox.maxZ()));
            }
            for (pos = 0; pos < this.boundingBox.getZSpan() && (pos += random.nextInt(this.boundingBox.getZSpan())) + 3 <= this.boundingBox.getZSpan(); pos += 4) {
                child = MineshaftPieces.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + random.nextInt(heightSpace) + 1, this.boundingBox.minZ() + pos, Direction.EAST, depth);
                if (child == null) continue;
                childBox = child.getBoundingBox();
                this.childEntranceBoxes.add(new BoundingBox(this.boundingBox.maxX() - 1, childBox.minY(), childBox.minZ(), this.boundingBox.maxX(), childBox.maxY(), childBox.maxZ()));
            }
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            if (this.isInInvalidLocation(level, chunkBB)) {
                return;
            }
            this.generateBox(level, chunkBB, this.boundingBox.minX(), this.boundingBox.minY() + 1, this.boundingBox.minZ(), this.boundingBox.maxX(), Math.min(this.boundingBox.minY() + 3, this.boundingBox.maxY()), this.boundingBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
            for (BoundingBox entranceBox : this.childEntranceBoxes) {
                this.generateBox(level, chunkBB, entranceBox.minX(), entranceBox.maxY() - 2, entranceBox.minZ(), entranceBox.maxX(), entranceBox.maxY(), entranceBox.maxZ(), CAVE_AIR, CAVE_AIR, false);
            }
            this.generateUpperHalfSphere(level, chunkBB, this.boundingBox.minX(), this.boundingBox.minY() + 4, this.boundingBox.minZ(), this.boundingBox.maxX(), this.boundingBox.maxY(), this.boundingBox.maxZ(), CAVE_AIR, false);
        }

        @Override
        public void move(int dx, int dy, int dz) {
            super.move(dx, dy, dz);
            for (BoundingBox bb : this.childEntranceBoxes) {
                bb.move(dx, dy, dz);
            }
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.store("Entrances", BoundingBox.CODEC.listOf(), this.childEntranceBoxes);
        }
    }
}

