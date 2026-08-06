/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Lists
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class NetherFortressPieces {
    private static final int MAX_DEPTH = 30;
    private static final int LOWEST_Y_POSITION = 10;
    public static final int MAGIC_START_Y = 64;
    private static final PieceWeight[] BRIDGE_PIECE_WEIGHTS = new PieceWeight[]{new PieceWeight(BridgeStraight.class, 30, 0, true), new PieceWeight(BridgeCrossing.class, 10, 4), new PieceWeight(RoomCrossing.class, 10, 4), new PieceWeight(StairsRoom.class, 10, 3), new PieceWeight(MonsterThrone.class, 5, 2), new PieceWeight(CastleEntrance.class, 5, 1)};
    private static final PieceWeight[] CASTLE_PIECE_WEIGHTS = new PieceWeight[]{new PieceWeight(CastleSmallCorridorPiece.class, 25, 0, true), new PieceWeight(CastleSmallCorridorCrossingPiece.class, 15, 5), new PieceWeight(CastleSmallCorridorRightTurnPiece.class, 5, 10), new PieceWeight(CastleSmallCorridorLeftTurnPiece.class, 5, 10), new PieceWeight(CastleCorridorStairsPiece.class, 10, 3, true), new PieceWeight(CastleCorridorTBalconyPiece.class, 7, 2), new PieceWeight(CastleStalkRoom.class, 5, 2)};

    private static @Nullable NetherBridgePiece findAndCreateBridgePieceFactory(PieceWeight piece, StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int depth) {
        Class<? extends NetherBridgePiece> pieceClass = piece.pieceClass;
        NetherBridgePiece structurePiece = null;
        if (pieceClass == BridgeStraight.class) {
            structurePiece = BridgeStraight.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
        } else if (pieceClass == BridgeCrossing.class) {
            structurePiece = BridgeCrossing.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
        } else if (pieceClass == RoomCrossing.class) {
            structurePiece = RoomCrossing.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
        } else if (pieceClass == StairsRoom.class) {
            structurePiece = StairsRoom.createPiece(structurePieceAccessor, footX, footY, footZ, depth, direction);
        } else if (pieceClass == MonsterThrone.class) {
            structurePiece = MonsterThrone.createPiece(structurePieceAccessor, footX, footY, footZ, depth, direction);
        } else if (pieceClass == CastleEntrance.class) {
            structurePiece = CastleEntrance.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
        } else if (pieceClass == CastleSmallCorridorPiece.class) {
            structurePiece = CastleSmallCorridorPiece.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
        } else if (pieceClass == CastleSmallCorridorRightTurnPiece.class) {
            structurePiece = CastleSmallCorridorRightTurnPiece.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
        } else if (pieceClass == CastleSmallCorridorLeftTurnPiece.class) {
            structurePiece = CastleSmallCorridorLeftTurnPiece.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
        } else if (pieceClass == CastleCorridorStairsPiece.class) {
            structurePiece = CastleCorridorStairsPiece.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
        } else if (pieceClass == CastleCorridorTBalconyPiece.class) {
            structurePiece = CastleCorridorTBalconyPiece.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
        } else if (pieceClass == CastleSmallCorridorCrossingPiece.class) {
            structurePiece = CastleSmallCorridorCrossingPiece.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
        } else if (pieceClass == CastleStalkRoom.class) {
            structurePiece = CastleStalkRoom.createPiece(structurePieceAccessor, footX, footY, footZ, direction, depth);
        }
        return structurePiece;
    }

    private static class PieceWeight {
        public final Class<? extends NetherBridgePiece> pieceClass;
        public final int weight;
        public int placeCount;
        public final int maxPlaceCount;
        public final boolean allowInRow;

        public PieceWeight(Class<? extends NetherBridgePiece> pieceClass, int weight, int maxPlaceCount, boolean allowInRow) {
            this.pieceClass = pieceClass;
            this.weight = weight;
            this.maxPlaceCount = maxPlaceCount;
            this.allowInRow = allowInRow;
        }

        public PieceWeight(Class<? extends NetherBridgePiece> pieceClass, int weight, int maxPlaceCount) {
            this(pieceClass, weight, maxPlaceCount, false);
        }

        public boolean doPlace(int depth) {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }

        public boolean isValid() {
            return this.maxPlaceCount == 0 || this.placeCount < this.maxPlaceCount;
        }
    }

    public static class BridgeStraight
    extends NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 19;

        public BridgeStraight(int genDepth, RandomSource random, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public BridgeStraight(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_STRAIGHT, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 1, 3, false);
        }

        public static @Nullable BridgeStraight createPiece(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -3, 0, 5, 10, 19, direction);
            if (!BridgeStraight.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new BridgeStraight(genDepth, random, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            this.generateBox(level, chunkBB, 0, 3, 0, 4, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 5, 0, 3, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 0, 0, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 5, 0, 4, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 4, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 13, 4, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 0, 15, 4, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (int x = 0; x <= 4; ++x) {
                for (int z = 0; z <= 2; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, 18 - z, chunkBB);
                }
            }
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            BlockState nseFence = (BlockState)nsFence.setValue(FenceBlock.EAST, true);
            BlockState nswFence = (BlockState)nsFence.setValue(FenceBlock.WEST, true);
            this.generateBox(level, chunkBB, 0, 1, 1, 0, 4, 1, nseFence, nseFence, false);
            this.generateBox(level, chunkBB, 0, 3, 4, 0, 4, 4, nseFence, nseFence, false);
            this.generateBox(level, chunkBB, 0, 3, 14, 0, 4, 14, nseFence, nseFence, false);
            this.generateBox(level, chunkBB, 0, 1, 17, 0, 4, 17, nseFence, nseFence, false);
            this.generateBox(level, chunkBB, 4, 1, 1, 4, 4, 1, nswFence, nswFence, false);
            this.generateBox(level, chunkBB, 4, 3, 4, 4, 4, 4, nswFence, nswFence, false);
            this.generateBox(level, chunkBB, 4, 3, 14, 4, 4, 14, nswFence, nswFence, false);
            this.generateBox(level, chunkBB, 4, 1, 17, 4, 4, 17, nswFence, nswFence, false);
        }
    }

    public static class BridgeCrossing
    extends NetherBridgePiece {
        private static final int WIDTH = 19;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 19;

        public BridgeCrossing(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        protected BridgeCrossing(int west, int north, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, 0, StructurePiece.makeBoundingBox(west, 64, north, direction, 19, 10, 19));
            this.setOrientation(direction);
        }

        protected BridgeCrossing(StructurePieceType type, CompoundTag tag) {
            super(type, tag);
        }

        public BridgeCrossing(CompoundTag tag) {
            this(StructurePieceType.NETHER_FORTRESS_BRIDGE_CROSSING, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 8, 3, false);
            this.generateChildLeft((StartPiece)startPiece, structurePieceAccessor, random, 3, 8, false);
            this.generateChildRight((StartPiece)startPiece, structurePieceAccessor, random, 3, 8, false);
        }

        public static @Nullable BridgeCrossing createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -8, -3, 0, 19, 10, 19, direction);
            if (!BridgeCrossing.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new BridgeCrossing(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            int z;
            int x;
            this.generateBox(level, chunkBB, 7, 3, 0, 11, 4, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 3, 7, 18, 4, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 8, 5, 0, 10, 7, 18, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 8, 18, 7, 10, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 7, 5, 0, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 7, 5, 11, 7, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 11, 5, 0, 11, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 11, 5, 11, 11, 5, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 7, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 11, 5, 7, 18, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 11, 7, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 11, 5, 11, 18, 5, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 7, 2, 0, 11, 2, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 7, 2, 13, 11, 2, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 7, 0, 0, 11, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 7, 0, 15, 11, 1, 18, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (x = 7; x <= 11; ++x) {
                for (z = 0; z <= 2; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, 18 - z, chunkBB);
                }
            }
            this.generateBox(level, chunkBB, 0, 2, 7, 5, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 13, 2, 7, 18, 2, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 0, 7, 3, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 15, 0, 7, 18, 1, 11, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (x = 0; x <= 2; ++x) {
                for (z = 7; z <= 11; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), 18 - x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class RoomCrossing
    extends NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 9;
        private static final int DEPTH = 7;

        public RoomCrossing(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public RoomCrossing(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_ROOM_CROSSING, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 2, 0, false);
            this.generateChildLeft((StartPiece)startPiece, structurePieceAccessor, random, 0, 2, false);
            this.generateChildRight((StartPiece)startPiece, structurePieceAccessor, random, 0, 2, false);
        }

        public static @Nullable RoomCrossing createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -2, 0, 0, 7, 9, 7, direction);
            if (!RoomCrossing.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new RoomCrossing(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            this.generateBox(level, chunkBB, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 6, 7, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 1, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 6, 1, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 2, 0, 6, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 2, 6, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 0, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 5, 0, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 2, 0, 6, 6, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 2, 5, 6, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState weFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.EAST, true);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            this.generateBox(level, chunkBB, 2, 6, 0, 4, 6, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 0, 4, 5, 0, weFence, weFence, false);
            this.generateBox(level, chunkBB, 2, 6, 6, 4, 6, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 6, 4, 5, 6, weFence, weFence, false);
            this.generateBox(level, chunkBB, 0, 6, 2, 0, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 2, 0, 5, 4, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 6, 6, 2, 6, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 5, 2, 6, 5, 4, nsFence, nsFence, false);
            for (int x = 0; x <= 6; ++x) {
                for (int z = 0; z <= 6; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class StairsRoom
    extends NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 11;
        private static final int DEPTH = 7;

        public StairsRoom(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public StairsRoom(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_STAIRS_ROOM, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildRight((StartPiece)startPiece, structurePieceAccessor, random, 6, 2, false);
        }

        public static @Nullable StairsRoom createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, int genDepth, Direction direction) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -2, 0, 0, 7, 11, 7, direction);
            if (!StairsRoom.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new StairsRoom(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            this.generateBox(level, chunkBB, 0, 0, 0, 6, 1, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 6, 10, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 1, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 2, 0, 6, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 1, 0, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 2, 1, 6, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 2, 6, 5, 8, 6, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState weFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.EAST, true);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            this.generateBox(level, chunkBB, 0, 3, 2, 0, 5, 4, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 6, 3, 2, 6, 5, 2, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 6, 3, 4, 6, 5, 4, nsFence, nsFence, false);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 5, 2, 5, chunkBB);
            this.generateBox(level, chunkBB, 4, 2, 5, 4, 3, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 3, 2, 5, 3, 4, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 2, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 2, 5, 1, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 7, 1, 5, 7, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 8, 2, 6, 8, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 6, 0, 4, 8, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 0, 4, 5, 0, weFence, weFence, false);
            for (int x = 0; x <= 6; ++x) {
                for (int z = 0; z <= 6; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class MonsterThrone
    extends NetherBridgePiece {
        private static final int WIDTH = 7;
        private static final int HEIGHT = 8;
        private static final int DEPTH = 9;
        private boolean hasPlacedSpawner;

        public MonsterThrone(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public MonsterThrone(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_MONSTER_THRONE, tag);
            this.hasPlacedSpawner = tag.getBooleanOr("Mob", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putBoolean("Mob", this.hasPlacedSpawner);
        }

        public static @Nullable MonsterThrone createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, int genDepth, Direction direction) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -2, 0, 0, 7, 8, 9, direction);
            if (!MonsterThrone.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new MonsterThrone(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            BlockPos.MutableBlockPos pos;
            this.generateBox(level, chunkBB, 0, 2, 0, 6, 7, 7, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 0, 0, 5, 1, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 2, 1, 5, 2, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 3, 2, 5, 3, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 4, 3, 5, 4, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 2, 0, 1, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 2, 0, 5, 4, 2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 5, 2, 1, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 5, 2, 5, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 3, 0, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 5, 3, 6, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 5, 8, 5, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState weFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.EAST, true);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            this.placeBlock(level, (BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 1, 6, 3, chunkBB);
            this.placeBlock(level, (BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 5, 6, 3, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true)).setValue(FenceBlock.NORTH, true), 0, 6, 3, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.NORTH, true), 6, 6, 3, chunkBB);
            this.generateBox(level, chunkBB, 0, 6, 4, 0, 6, 7, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 6, 6, 4, 6, 6, 7, nsFence, nsFence, false);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true)).setValue(FenceBlock.SOUTH, true), 0, 6, 8, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.SOUTH, true), 6, 6, 8, chunkBB);
            this.generateBox(level, chunkBB, 1, 6, 8, 5, 6, 8, weFence, weFence, false);
            this.placeBlock(level, (BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 1, 7, 8, chunkBB);
            this.generateBox(level, chunkBB, 2, 7, 8, 4, 7, 8, weFence, weFence, false);
            this.placeBlock(level, (BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 5, 7, 8, chunkBB);
            this.placeBlock(level, (BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true), 2, 8, 8, chunkBB);
            this.placeBlock(level, weFence, 3, 8, 8, chunkBB);
            this.placeBlock(level, (BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true), 4, 8, 8, chunkBB);
            if (!this.hasPlacedSpawner && chunkBB.isInside(pos = this.getWorldPos(3, 5, 5))) {
                this.hasPlacedSpawner = true;
                level.setBlock(pos, Blocks.SPAWNER.defaultBlockState(), 2);
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof SpawnerBlockEntity) {
                    SpawnerBlockEntity spawner = (SpawnerBlockEntity)blockEntity;
                    spawner.setEntityId(EntityTypes.BLAZE, random);
                }
            }
            for (int x = 0; x <= 6; ++x) {
                for (int z = 0; z <= 6; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class CastleEntrance
    extends NetherBridgePiece {
        private static final int WIDTH = 13;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 13;

        public CastleEntrance(int genDepth, RandomSource random, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public CastleEntrance(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_ENTRANCE, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 5, 3, true);
        }

        public static @Nullable CastleEntrance createPiece(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -5, -3, 0, 13, 14, 13, direction);
            if (!CastleEntrance.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new CastleEntrance(genDepth, random, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            int z;
            int x;
            this.generateBox(level, chunkBB, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 8, 0, 7, 8, 0, Blocks.NETHER_BRICK_FENCE.defaultBlockState(), Blocks.NETHER_BRICK_FENCE.defaultBlockState(), false);
            BlockState weFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.EAST, true);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            for (int i = 1; i <= 11; i += 2) {
                this.generateBox(level, chunkBB, i, 10, 0, i, 11, 0, weFence, weFence, false);
                this.generateBox(level, chunkBB, i, 10, 12, i, 11, 12, weFence, weFence, false);
                this.generateBox(level, chunkBB, 0, 10, i, 0, 11, i, nsFence, nsFence, false);
                this.generateBox(level, chunkBB, 12, 10, i, 12, 11, i, nsFence, nsFence, false);
                this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, chunkBB);
                this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, chunkBB);
                this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, chunkBB);
                this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, chunkBB);
                if (i == 11) continue;
                this.placeBlock(level, weFence, i + 1, 13, 0, chunkBB);
                this.placeBlock(level, weFence, i + 1, 13, 12, chunkBB);
                this.placeBlock(level, nsFence, 0, 13, i + 1, chunkBB);
                this.placeBlock(level, nsFence, 12, 13, i + 1, chunkBB);
            }
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.EAST, true), 0, 13, 0, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true)).setValue(FenceBlock.EAST, true), 0, 13, 12, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true)).setValue(FenceBlock.WEST, true), 12, 13, 12, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.WEST, true), 12, 13, 0, chunkBB);
            for (int z2 = 3; z2 <= 9; z2 += 2) {
                this.generateBox(level, chunkBB, 1, 7, z2, 1, 8, z2, (BlockState)nsFence.setValue(FenceBlock.WEST, true), (BlockState)nsFence.setValue(FenceBlock.WEST, true), false);
                this.generateBox(level, chunkBB, 11, 7, z2, 11, 8, z2, (BlockState)nsFence.setValue(FenceBlock.EAST, true), (BlockState)nsFence.setValue(FenceBlock.EAST, true), false);
            }
            this.generateBox(level, chunkBB, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (x = 4; x <= 8; ++x) {
                for (z = 0; z <= 2; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, 12 - z, chunkBB);
                }
            }
            for (x = 0; x <= 2; ++x) {
                for (z = 4; z <= 8; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - x, -1, z, chunkBB);
                }
            }
            this.generateBox(level, chunkBB, 5, 5, 5, 7, 5, 7, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 1, 6, 6, 4, 6, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 6, 0, 6, chunkBB);
            this.placeBlock(level, Blocks.LAVA.defaultBlockState(), 6, 5, 6, chunkBB);
            BlockPos.MutableBlockPos pos = this.getWorldPos(6, 5, 6);
            if (chunkBB.isInside(pos)) {
                level.scheduleTick((BlockPos)pos, Fluids.LAVA, 0);
            }
        }
    }

    public static class CastleSmallCorridorPiece
    extends NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;

        public CastleSmallCorridorPiece(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public CastleSmallCorridorPiece(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 1, 0, true);
        }

        public static @Nullable CastleSmallCorridorPiece createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, 0, 0, 5, 7, 5, direction);
            if (!CastleSmallCorridorPiece.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new CastleSmallCorridorPiece(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            this.generateBox(level, chunkBB, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 3, 1, 0, 4, 1, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 0, 3, 3, 0, 4, 3, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 4, 3, 1, 4, 4, 1, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 4, 3, 3, 4, 4, 3, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (int x = 0; x <= 4; ++x) {
                for (int z = 0; z <= 4; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class CastleSmallCorridorRightTurnPiece
    extends NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;
        private boolean isNeedingChest;

        public CastleSmallCorridorRightTurnPiece(int genDepth, RandomSource random, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, genDepth, boundingBox);
            this.setOrientation(direction);
            this.isNeedingChest = random.nextInt(3) == 0;
        }

        public CastleSmallCorridorRightTurnPiece(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_RIGHT_TURN, tag);
            this.isNeedingChest = tag.getBooleanOr("Chest", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putBoolean("Chest", this.isNeedingChest);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildRight((StartPiece)startPiece, structurePieceAccessor, random, 0, 1, true);
        }

        public static @Nullable CastleSmallCorridorRightTurnPiece createPiece(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, 0, 0, 5, 7, 5, direction);
            if (!CastleSmallCorridorRightTurnPiece.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new CastleSmallCorridorRightTurnPiece(genDepth, random, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState weFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.EAST, true);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            this.generateBox(level, chunkBB, 0, 2, 0, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 3, 1, 0, 4, 1, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 0, 3, 3, 0, 4, 3, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 3, 4, 1, 4, 4, weFence, weFence, false);
            this.generateBox(level, chunkBB, 3, 3, 4, 3, 4, 4, weFence, weFence, false);
            if (this.isNeedingChest && chunkBB.isInside(this.getWorldPos(1, 2, 3))) {
                this.isNeedingChest = false;
                this.createChest(level, chunkBB, random, 1, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
            }
            this.generateBox(level, chunkBB, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (int x = 0; x <= 4; ++x) {
                for (int z = 0; z <= 4; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class CastleSmallCorridorLeftTurnPiece
    extends NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;
        private boolean isNeedingChest;

        public CastleSmallCorridorLeftTurnPiece(int genDepth, RandomSource random, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, genDepth, boundingBox);
            this.setOrientation(direction);
            this.isNeedingChest = random.nextInt(3) == 0;
        }

        public CastleSmallCorridorLeftTurnPiece(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_LEFT_TURN, tag);
            this.isNeedingChest = tag.getBooleanOr("Chest", false);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putBoolean("Chest", this.isNeedingChest);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildLeft((StartPiece)startPiece, structurePieceAccessor, random, 0, 1, true);
        }

        public static @Nullable CastleSmallCorridorLeftTurnPiece createPiece(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, 0, 0, 5, 7, 5, direction);
            if (!CastleSmallCorridorLeftTurnPiece.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new CastleSmallCorridorLeftTurnPiece(genDepth, random, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            BlockState weFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.EAST, true);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            this.generateBox(level, chunkBB, 4, 2, 0, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 3, 1, 4, 4, 1, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 4, 3, 3, 4, 4, 3, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 4, 3, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 3, 4, 1, 4, 4, weFence, weFence, false);
            this.generateBox(level, chunkBB, 3, 3, 4, 3, 4, 4, weFence, weFence, false);
            if (this.isNeedingChest && chunkBB.isInside(this.getWorldPos(3, 2, 3))) {
                this.isNeedingChest = false;
                this.createChest(level, chunkBB, random, 3, 2, 3, BuiltInLootTables.NETHER_BRIDGE);
            }
            this.generateBox(level, chunkBB, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (int x = 0; x <= 4; ++x) {
                for (int z = 0; z <= 4; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class CastleCorridorStairsPiece
    extends NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 10;

        public CastleCorridorStairsPiece(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public CastleCorridorStairsPiece(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_STAIRS, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 1, 0, true);
        }

        public static @Nullable CastleCorridorStairsPiece createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -7, 0, 5, 14, 10, direction);
            if (!CastleCorridorStairsPiece.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new CastleCorridorStairsPiece(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            BlockState stairs = (BlockState)Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            for (int step = 0; step <= 9; ++step) {
                int floor = Math.max(1, 7 - step);
                int roof = Math.min(Math.max(floor + 5, 14 - step), 13);
                int z = step;
                this.generateBox(level, chunkBB, 0, 0, z, 4, floor, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(level, chunkBB, 1, floor + 1, z, 3, roof - 1, z, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
                if (step <= 6) {
                    this.placeBlock(level, stairs, 1, floor + 1, z, chunkBB);
                    this.placeBlock(level, stairs, 2, floor + 1, z, chunkBB);
                    this.placeBlock(level, stairs, 3, floor + 1, z, chunkBB);
                }
                this.generateBox(level, chunkBB, 0, roof, z, 4, roof, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(level, chunkBB, 0, floor + 1, z, 0, roof - 1, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                this.generateBox(level, chunkBB, 4, floor + 1, z, 4, roof - 1, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                if ((step & 1) == 0) {
                    this.generateBox(level, chunkBB, 0, floor + 2, z, 0, floor + 3, z, nsFence, nsFence, false);
                    this.generateBox(level, chunkBB, 4, floor + 2, z, 4, floor + 3, z, nsFence, nsFence, false);
                }
                for (int x = 0; x <= 4; ++x) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class CastleCorridorTBalconyPiece
    extends NetherBridgePiece {
        private static final int WIDTH = 9;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 9;

        public CastleCorridorTBalconyPiece(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public CastleCorridorTBalconyPiece(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_CORRIDOR_T_BALCONY, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            int zOff = 1;
            Direction orientation = this.getOrientation();
            if (orientation == Direction.WEST || orientation == Direction.NORTH) {
                zOff = 5;
            }
            this.generateChildLeft((StartPiece)startPiece, structurePieceAccessor, random, 0, zOff, random.nextInt(8) > 0);
            this.generateChildRight((StartPiece)startPiece, structurePieceAccessor, random, 0, zOff, random.nextInt(8) > 0);
        }

        public static @Nullable CastleCorridorTBalconyPiece createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -3, 0, 0, 9, 7, 9, direction);
            if (!CastleCorridorTBalconyPiece.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new CastleCorridorTBalconyPiece(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            BlockState weFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.EAST, true);
            this.generateBox(level, chunkBB, 0, 0, 0, 8, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 8, 5, 8, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 6, 0, 8, 6, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 2, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 2, 0, 8, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 3, 0, 1, 4, 0, weFence, weFence, false);
            this.generateBox(level, chunkBB, 7, 3, 0, 7, 4, 0, weFence, weFence, false);
            this.generateBox(level, chunkBB, 0, 2, 4, 8, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 1, 4, 2, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 1, 4, 7, 2, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 3, 8, 7, 3, 8, weFence, weFence, false);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.EAST, true)).setValue(FenceBlock.SOUTH, true), 0, 3, 8, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.SOUTH, true), 8, 3, 8, chunkBB);
            this.generateBox(level, chunkBB, 0, 3, 6, 0, 3, 7, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 8, 3, 6, 8, 3, 7, nsFence, nsFence, false);
            this.generateBox(level, chunkBB, 0, 3, 4, 0, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 8, 3, 4, 8, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 3, 5, 2, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 6, 3, 5, 7, 5, 5, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 1, 4, 5, 1, 5, 5, weFence, weFence, false);
            this.generateBox(level, chunkBB, 7, 4, 5, 7, 5, 5, weFence, weFence, false);
            for (int z = 0; z <= 5; ++z) {
                for (int x = 0; x <= 8; ++x) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class CastleSmallCorridorCrossingPiece
    extends NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 7;
        private static final int DEPTH = 5;

        public CastleSmallCorridorCrossingPiece(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public CastleSmallCorridorCrossingPiece(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_SMALL_CORRIDOR_CROSSING, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 1, 0, true);
            this.generateChildLeft((StartPiece)startPiece, structurePieceAccessor, random, 0, 1, true);
            this.generateChildRight((StartPiece)startPiece, structurePieceAccessor, random, 0, 1, true);
        }

        public static @Nullable CastleSmallCorridorCrossingPiece createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, 0, 0, 5, 7, 5, direction);
            if (!CastleSmallCorridorCrossingPiece.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new CastleSmallCorridorCrossingPiece(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            this.generateBox(level, chunkBB, 0, 0, 0, 4, 1, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 4, 5, 4, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 0, 0, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 2, 0, 4, 5, 0, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 4, 0, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 2, 4, 4, 5, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 6, 0, 4, 6, 4, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (int x = 0; x <= 4; ++x) {
                for (int z = 0; z <= 4; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class CastleStalkRoom
    extends NetherBridgePiece {
        private static final int WIDTH = 13;
        private static final int HEIGHT = 14;
        private static final int DEPTH = 13;

        public CastleStalkRoom(int genDepth, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, genDepth, boundingBox);
            this.setOrientation(direction);
        }

        public CastleStalkRoom(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_CASTLE_STALK_ROOM, tag);
        }

        @Override
        public void addChildren(StructurePiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random) {
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 5, 3, true);
            this.generateChildForward((StartPiece)startPiece, structurePieceAccessor, random, 5, 11, true);
        }

        public static @Nullable CastleStalkRoom createPiece(StructurePieceAccessor structurePieceAccessor, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -5, -3, 0, 13, 14, 13, direction);
            if (!CastleStalkRoom.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new CastleStalkRoom(genDepth, box, direction);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            int z;
            int x;
            this.generateBox(level, chunkBB, 0, 3, 0, 12, 4, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 0, 12, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 5, 0, 1, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 11, 5, 0, 12, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 11, 4, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 8, 5, 11, 10, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 9, 11, 7, 12, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 0, 4, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 8, 5, 0, 10, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 5, 9, 0, 7, 12, 1, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 11, 2, 10, 12, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState weFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.WEST, true)).setValue(FenceBlock.EAST, true);
            BlockState nsFence = (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.SOUTH, true);
            BlockState nswFence = (BlockState)nsFence.setValue(FenceBlock.WEST, true);
            BlockState nseFence = (BlockState)nsFence.setValue(FenceBlock.EAST, true);
            for (int i = 1; i <= 11; i += 2) {
                this.generateBox(level, chunkBB, i, 10, 0, i, 11, 0, weFence, weFence, false);
                this.generateBox(level, chunkBB, i, 10, 12, i, 11, 12, weFence, weFence, false);
                this.generateBox(level, chunkBB, 0, 10, i, 0, 11, i, nsFence, nsFence, false);
                this.generateBox(level, chunkBB, 12, 10, i, 12, 11, i, nsFence, nsFence, false);
                this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 0, chunkBB);
                this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), i, 13, 12, chunkBB);
                this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 0, 13, i, chunkBB);
                this.placeBlock(level, Blocks.NETHER_BRICKS.defaultBlockState(), 12, 13, i, chunkBB);
                if (i == 11) continue;
                this.placeBlock(level, weFence, i + 1, 13, 0, chunkBB);
                this.placeBlock(level, weFence, i + 1, 13, 12, chunkBB);
                this.placeBlock(level, nsFence, 0, 13, i + 1, chunkBB);
                this.placeBlock(level, nsFence, 12, 13, i + 1, chunkBB);
            }
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.EAST, true), 0, 13, 0, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true)).setValue(FenceBlock.EAST, true), 0, 13, 12, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.SOUTH, true)).setValue(FenceBlock.WEST, true), 12, 13, 12, chunkBB);
            this.placeBlock(level, (BlockState)((BlockState)Blocks.NETHER_BRICK_FENCE.defaultBlockState().setValue(FenceBlock.NORTH, true)).setValue(FenceBlock.WEST, true), 12, 13, 0, chunkBB);
            for (int z2 = 3; z2 <= 9; z2 += 2) {
                this.generateBox(level, chunkBB, 1, 7, z2, 1, 8, z2, nswFence, nswFence, false);
                this.generateBox(level, chunkBB, 11, 7, z2, 11, 8, z2, nseFence, nseFence, false);
            }
            BlockState stairs = (BlockState)Blocks.NETHER_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.NORTH);
            for (int i = 0; i <= 6; ++i) {
                int z3 = i + 4;
                for (x = 5; x <= 7; ++x) {
                    this.placeBlock(level, stairs, x, 5 + i, z3, chunkBB);
                }
                if (z3 >= 5 && z3 <= 8) {
                    this.generateBox(level, chunkBB, 5, 5, z3, 7, i + 4, z3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                } else if (z3 >= 9 && z3 <= 10) {
                    this.generateBox(level, chunkBB, 5, 8, z3, 7, i + 4, z3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }
                if (i < 1) continue;
                this.generateBox(level, chunkBB, 5, 6 + i, z3, 7, 9 + i, z3, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            }
            for (int x2 = 5; x2 <= 7; ++x2) {
                this.placeBlock(level, stairs, x2, 12, 11, chunkBB);
            }
            this.generateBox(level, chunkBB, 5, 6, 7, 5, 7, 7, nseFence, nseFence, false);
            this.generateBox(level, chunkBB, 7, 6, 7, 7, 7, 7, nswFence, nswFence, false);
            this.generateBox(level, chunkBB, 5, 13, 12, 7, 13, 12, Blocks.AIR.defaultBlockState(), Blocks.AIR.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 2, 3, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 9, 3, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 2, 5, 4, 2, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 9, 5, 2, 10, 5, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 9, 5, 9, 10, 5, 10, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 10, 5, 4, 10, 5, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            BlockState eastStairs = (BlockState)stairs.setValue(StairBlock.FACING, Direction.EAST);
            BlockState westStairs = (BlockState)stairs.setValue(StairBlock.FACING, Direction.WEST);
            this.placeBlock(level, westStairs, 4, 5, 2, chunkBB);
            this.placeBlock(level, westStairs, 4, 5, 3, chunkBB);
            this.placeBlock(level, westStairs, 4, 5, 9, chunkBB);
            this.placeBlock(level, westStairs, 4, 5, 10, chunkBB);
            this.placeBlock(level, eastStairs, 8, 5, 2, chunkBB);
            this.placeBlock(level, eastStairs, 8, 5, 3, chunkBB);
            this.placeBlock(level, eastStairs, 8, 5, 9, chunkBB);
            this.placeBlock(level, eastStairs, 8, 5, 10, chunkBB);
            this.generateBox(level, chunkBB, 3, 4, 4, 4, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 8, 4, 4, 9, 4, 8, Blocks.SOUL_SAND.defaultBlockState(), Blocks.SOUL_SAND.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 3, 5, 4, 4, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 8, 5, 4, 9, 5, 8, Blocks.NETHER_WART.defaultBlockState(), Blocks.NETHER_WART.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 2, 0, 8, 2, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 2, 4, 12, 2, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 0, 0, 8, 1, 3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 4, 0, 9, 8, 1, 12, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 0, 0, 4, 3, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            this.generateBox(level, chunkBB, 9, 0, 4, 12, 1, 8, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (x = 4; x <= 8; ++x) {
                for (z = 0; z <= 2; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, 12 - z, chunkBB);
                }
            }
            for (x = 0; x <= 2; ++x) {
                for (z = 4; z <= 8; ++z) {
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), x, -1, z, chunkBB);
                    this.fillColumnDown(level, Blocks.NETHER_BRICKS.defaultBlockState(), 12 - x, -1, z, chunkBB);
                }
            }
        }
    }

    public static class BridgeEndFiller
    extends NetherBridgePiece {
        private static final int WIDTH = 5;
        private static final int HEIGHT = 10;
        private static final int DEPTH = 8;
        private final int selfSeed;

        public BridgeEndFiller(int genDepth, RandomSource random, BoundingBox boundingBox, Direction direction) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, genDepth, boundingBox);
            this.setOrientation(direction);
            this.selfSeed = random.nextInt();
        }

        public BridgeEndFiller(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_BRIDGE_END_FILLER, tag);
            this.selfSeed = tag.getIntOr("Seed", 0);
        }

        public static @Nullable BridgeEndFiller createPiece(StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int genDepth) {
            BoundingBox box = BoundingBox.orientBox(footX, footY, footZ, -1, -3, 0, 5, 10, 8, direction);
            if (!BridgeEndFiller.isOkBox(box) || structurePieceAccessor.findCollisionPiece(box) != null) {
                return null;
            }
            return new BridgeEndFiller(genDepth, random, box, direction);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            super.addAdditionalSaveData(context, tag);
            tag.putInt("Seed", this.selfSeed);
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
            int z;
            int y;
            int x;
            RandomSource selfRandom = RandomSource.createThreadLocalInstance(this.selfSeed);
            for (x = 0; x <= 4; ++x) {
                for (y = 3; y <= 4; ++y) {
                    z = selfRandom.nextInt(8);
                    this.generateBox(level, chunkBB, x, y, 0, x, y, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }
            }
            int z2 = selfRandom.nextInt(8);
            this.generateBox(level, chunkBB, 0, 5, 0, 0, 5, z2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            z2 = selfRandom.nextInt(8);
            this.generateBox(level, chunkBB, 4, 5, 0, 4, 5, z2, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            for (x = 0; x <= 4; ++x) {
                int z3 = selfRandom.nextInt(5);
                this.generateBox(level, chunkBB, x, 2, 0, x, 2, z3, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
            }
            for (x = 0; x <= 4; ++x) {
                for (y = 0; y <= 1; ++y) {
                    z = selfRandom.nextInt(3);
                    this.generateBox(level, chunkBB, x, y, 0, x, y, z, Blocks.NETHER_BRICKS.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(), false);
                }
            }
        }
    }

    public static class StartPiece
    extends BridgeCrossing {
        private @Nullable PieceWeight previousPiece;
        private final List<PieceWeight> availableBridgePieces = new ArrayList<PieceWeight>();
        private final List<PieceWeight> availableCastlePieces = new ArrayList<PieceWeight>();
        public final List<StructurePiece> pendingChildren = Lists.newArrayList();

        public StartPiece(RandomSource random, int west, int north) {
            super(west, north, StartPiece.getRandomHorizontalDirection(random));
            for (PieceWeight piece : BRIDGE_PIECE_WEIGHTS) {
                piece.placeCount = 0;
                this.availableBridgePieces.add(piece);
            }
            for (PieceWeight piece : CASTLE_PIECE_WEIGHTS) {
                piece.placeCount = 0;
                this.availableCastlePieces.add(piece);
            }
        }

        public StartPiece(CompoundTag tag) {
            super(StructurePieceType.NETHER_FORTRESS_START, tag);
        }
    }

    private static abstract class NetherBridgePiece
    extends StructurePiece {
        protected NetherBridgePiece(StructurePieceType type, int genDepth, BoundingBox boundingBox) {
            super(type, genDepth, boundingBox);
        }

        public NetherBridgePiece(StructurePieceType type, CompoundTag tag) {
            super(type, tag);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        }

        private int updatePieceWeight(List<PieceWeight> currentPieces) {
            boolean hasAnyPieces = false;
            int totalWeight = 0;
            for (PieceWeight piece : currentPieces) {
                if (piece.maxPlaceCount > 0 && piece.placeCount < piece.maxPlaceCount) {
                    hasAnyPieces = true;
                }
                totalWeight += piece.weight;
            }
            return hasAnyPieces ? totalWeight : -1;
        }

        private @Nullable NetherBridgePiece generatePiece(StartPiece startPiece, List<PieceWeight> currentPieces, StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int depth) {
            int totalWeight = this.updatePieceWeight(currentPieces);
            boolean doStuff = totalWeight > 0 && depth <= 30;
            int numAttempts = 0;
            block0: while (numAttempts < 5 && doStuff) {
                ++numAttempts;
                int weightSelection = random.nextInt(totalWeight);
                for (PieceWeight piece : currentPieces) {
                    if ((weightSelection -= piece.weight) >= 0) continue;
                    if (!piece.doPlace(depth) || piece == startPiece.previousPiece && !piece.allowInRow) continue block0;
                    NetherBridgePiece structurePiece = NetherFortressPieces.findAndCreateBridgePieceFactory(piece, structurePieceAccessor, random, footX, footY, footZ, direction, depth);
                    if (structurePiece == null) continue;
                    ++piece.placeCount;
                    startPiece.previousPiece = piece;
                    if (!piece.isValid()) {
                        currentPieces.remove(piece);
                    }
                    return structurePiece;
                }
            }
            return BridgeEndFiller.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
        }

        private @Nullable StructurePiece generateAndAddPiece(StartPiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random, int footX, int footY, int footZ, Direction direction, int depth, boolean isCastle) {
            NetherBridgePiece newPiece;
            if (Math.abs(footX - startPiece.getBoundingBox().minX()) > 112 || Math.abs(footZ - startPiece.getBoundingBox().minZ()) > 112) {
                return BridgeEndFiller.createPiece(structurePieceAccessor, random, footX, footY, footZ, direction, depth);
            }
            List<PieceWeight> availablePieces = startPiece.availableBridgePieces;
            if (isCastle) {
                availablePieces = startPiece.availableCastlePieces;
            }
            if ((newPiece = this.generatePiece(startPiece, availablePieces, structurePieceAccessor, random, footX, footY, footZ, direction, depth + 1)) != null) {
                structurePieceAccessor.addPiece(newPiece);
                startPiece.pendingChildren.add(newPiece);
            }
            return newPiece;
        }

        protected @Nullable StructurePiece generateChildForward(StartPiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random, int xOff, int yOff, boolean isCastle) {
            Direction orientation = this.getOrientation();
            if (orientation != null) {
                switch (orientation) {
                    case NORTH: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + xOff, this.boundingBox.minY() + yOff, this.boundingBox.minZ() - 1, orientation, this.getGenDepth(), isCastle);
                    }
                    case SOUTH: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + xOff, this.boundingBox.minY() + yOff, this.boundingBox.maxZ() + 1, orientation, this.getGenDepth(), isCastle);
                    }
                    case WEST: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + yOff, this.boundingBox.minZ() + xOff, orientation, this.getGenDepth(), isCastle);
                    }
                    case EAST: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + yOff, this.boundingBox.minZ() + xOff, orientation, this.getGenDepth(), isCastle);
                    }
                }
            }
            return null;
        }

        protected @Nullable StructurePiece generateChildLeft(StartPiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random, int yOff, int zOff, boolean isCastle) {
            Direction orientation = this.getOrientation();
            if (orientation != null) {
                switch (orientation) {
                    case NORTH: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + yOff, this.boundingBox.minZ() + zOff, Direction.WEST, this.getGenDepth(), isCastle);
                    }
                    case SOUTH: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() - 1, this.boundingBox.minY() + yOff, this.boundingBox.minZ() + zOff, Direction.WEST, this.getGenDepth(), isCastle);
                    }
                    case WEST: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + zOff, this.boundingBox.minY() + yOff, this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth(), isCastle);
                    }
                    case EAST: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + zOff, this.boundingBox.minY() + yOff, this.boundingBox.minZ() - 1, Direction.NORTH, this.getGenDepth(), isCastle);
                    }
                }
            }
            return null;
        }

        protected @Nullable StructurePiece generateChildRight(StartPiece startPiece, StructurePieceAccessor structurePieceAccessor, RandomSource random, int yOff, int zOff, boolean isCastle) {
            Direction orientation = this.getOrientation();
            if (orientation != null) {
                switch (orientation) {
                    case NORTH: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + yOff, this.boundingBox.minZ() + zOff, Direction.EAST, this.getGenDepth(), isCastle);
                    }
                    case SOUTH: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.maxX() + 1, this.boundingBox.minY() + yOff, this.boundingBox.minZ() + zOff, Direction.EAST, this.getGenDepth(), isCastle);
                    }
                    case WEST: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + zOff, this.boundingBox.minY() + yOff, this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth(), isCastle);
                    }
                    case EAST: {
                        return this.generateAndAddPiece(startPiece, structurePieceAccessor, random, this.boundingBox.minX() + zOff, this.boundingBox.minY() + yOff, this.boundingBox.maxZ() + 1, Direction.SOUTH, this.getGenDepth(), isCastle);
                    }
                }
            }
            return null;
        }

        protected static boolean isOkBox(BoundingBox box) {
            return box.minY() > 10;
        }
    }
}

