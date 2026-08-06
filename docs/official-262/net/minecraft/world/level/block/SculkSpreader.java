/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Lists
 *  com.google.common.collect.Sets
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  it.unimi.dsi.fastutil.objects.Object2IntMap$Entry
 *  it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.SculkBehaviour;
import net.minecraft.world.level.block.SculkVeinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SculkSpreader {
    public static final int MAX_GROWTH_RATE_RADIUS = 24;
    public static final int MAX_CHARGE = 1000;
    public static final float MAX_DECAY_FACTOR = 0.5f;
    private static final int MAX_CURSORS = 32;
    public static final int SHRIEKER_PLACEMENT_RATE = 11;
    public static final int MAX_CURSOR_DISTANCE = 1024;
    private final boolean isWorldGeneration;
    private final TagKey<Block> replaceableBlocks;
    private final int growthSpawnCost;
    private final int noGrowthRadius;
    private final int chargeDecayRate;
    private final int additionalDecayRate;
    private List<ChargeCursor> cursors = new ArrayList<ChargeCursor>();

    public SculkSpreader(boolean isWorldGeneration, TagKey<Block> replaceableBlocks, int growthSpawnCost, int noGrowthRadius, int chargeDecayRate, int additionalDecayRate) {
        this.isWorldGeneration = isWorldGeneration;
        this.replaceableBlocks = replaceableBlocks;
        this.growthSpawnCost = growthSpawnCost;
        this.noGrowthRadius = noGrowthRadius;
        this.chargeDecayRate = chargeDecayRate;
        this.additionalDecayRate = additionalDecayRate;
    }

    public static SculkSpreader createLevelSpreader() {
        return new SculkSpreader(false, BlockTags.SCULK_REPLACEABLE, 10, 4, 10, 5);
    }

    public static SculkSpreader createWorldGenSpreader() {
        return new SculkSpreader(true, BlockTags.SCULK_REPLACEABLE_WORLD_GEN, 50, 1, 5, 10);
    }

    public TagKey<Block> replaceableBlocks() {
        return this.replaceableBlocks;
    }

    public int growthSpawnCost() {
        return this.growthSpawnCost;
    }

    public int noGrowthRadius() {
        return this.noGrowthRadius;
    }

    public int chargeDecayRate() {
        return this.chargeDecayRate;
    }

    public int additionalDecayRate() {
        return this.additionalDecayRate;
    }

    public boolean isWorldGeneration() {
        return this.isWorldGeneration;
    }

    @VisibleForTesting
    public List<ChargeCursor> getCursors() {
        return this.cursors;
    }

    public void clear() {
        this.cursors.clear();
    }

    public void load(ValueInput input) {
        this.cursors.clear();
        input.read("cursors", ChargeCursor.CODEC.sizeLimitedListOf(32)).orElse(List.of()).forEach(this::addCursor);
    }

    public void save(ValueOutput output) {
        output.store("cursors", ChargeCursor.CODEC.listOf(), this.cursors);
        if (SharedConstants.DEBUG_SCULK_CATALYST) {
            int charge = this.getCursors().stream().map(ChargeCursor::getCharge).reduce(0, Integer::sum);
            int charges = this.getCursors().stream().map(c -> 1).reduce(0, Integer::sum);
            int max = this.getCursors().stream().map(ChargeCursor::getCharge).reduce(0, Math::max);
            output.putInt("stats.total", charge);
            output.putInt("stats.count", charges);
            output.putInt("stats.max", max);
            output.putInt("stats.avg", charge / (charges + 1));
        }
    }

    public void addCursors(BlockPos startPos, int charge) {
        while (charge > 0) {
            int currentCharge = Math.min(charge, 1000);
            this.addCursor(new ChargeCursor(startPos, currentCharge));
            charge -= currentCharge;
        }
    }

    private void addCursor(ChargeCursor cursor) {
        if (this.cursors.size() >= 32) {
            return;
        }
        this.cursors.add(cursor);
    }

    public void updateCursors(LevelAccessor level, BlockPos originPos, RandomSource random, boolean spreadVeins) {
        BlockPos pos;
        if (this.cursors.isEmpty()) {
            return;
        }
        ArrayList<ChargeCursor> processedCursors = new ArrayList<ChargeCursor>();
        HashMap<BlockPos, ChargeCursor> mergeableCursors = new HashMap<BlockPos, ChargeCursor>();
        Object2IntOpenHashMap chargeMap = new Object2IntOpenHashMap();
        for (ChargeCursor cursor : this.cursors) {
            if (cursor.isPosUnreasonable(originPos)) continue;
            cursor.update(level, originPos, random, this, spreadVeins);
            if (cursor.charge <= 0) {
                level.levelEvent(3006, cursor.getPos(), 0);
                continue;
            }
            pos = cursor.getPos();
            chargeMap.computeInt((Object)pos, (k, count) -> (count == null ? 0 : count) + cursor.charge);
            ChargeCursor existing = (ChargeCursor)mergeableCursors.get(pos);
            if (existing == null) {
                mergeableCursors.put(pos, cursor);
                processedCursors.add(cursor);
                continue;
            }
            if (!this.isWorldGeneration() && cursor.charge + existing.charge <= 1000) {
                existing.mergeWith(cursor);
                continue;
            }
            processedCursors.add(cursor);
            if (cursor.charge >= existing.charge) continue;
            mergeableCursors.put(pos, cursor);
        }
        for (Object2IntMap.Entry entry : chargeMap.object2IntEntrySet()) {
            Set<Direction> faces;
            pos = (BlockPos)entry.getKey();
            int charge = entry.getIntValue();
            ChargeCursor cursor = (ChargeCursor)mergeableCursors.get(pos);
            Set<Direction> set = faces = cursor == null ? null : cursor.getFacingData();
            if (charge <= 0 || faces == null) continue;
            int numParticles = (int)(Math.log1p(charge) / (double)2.3f) + 1;
            int data = (numParticles << 6) + MultifaceBlock.pack(faces);
            level.levelEvent(3006, pos, data);
        }
        this.cursors = processedCursors;
    }

    public static class ChargeCursor {
        private static final ObjectArrayList<Vec3i> NON_CORNER_NEIGHBOURS = Util.make(new ObjectArrayList(18), list -> BlockPos.betweenClosedStream(new BlockPos(-1, -1, -1), new BlockPos(1, 1, 1)).filter(position -> (position.getX() == 0 || position.getY() == 0 || position.getZ() == 0) && !position.equals(BlockPos.ZERO)).map(BlockPos::immutable).forEach(arg_0 -> ((ObjectArrayList)list).add(arg_0)));
        public static final int MAX_CURSOR_DECAY_DELAY = 1;
        private BlockPos pos;
        private int charge;
        private int updateDelay;
        private int decayDelay;
        private @Nullable Set<Direction> facings;
        private static final Codec<Set<Direction>> DIRECTION_SET = Direction.CODEC.listOf().xmap(l -> Sets.newEnumSet((Iterable)l, Direction.class), Lists::newArrayList);
        public static final Codec<ChargeCursor> CODEC = RecordCodecBuilder.create(i -> i.group((App)BlockPos.CODEC.fieldOf("pos").forGetter(ChargeCursor::getPos), (App)Codec.intRange((int)0, (int)1000).optionalFieldOf("charge", (Object)0).forGetter(ChargeCursor::getCharge), (App)Codec.intRange((int)0, (int)1).optionalFieldOf("decay_delay", (Object)1).forGetter(ChargeCursor::getDecayDelay), (App)Codec.intRange((int)0, (int)Integer.MAX_VALUE).optionalFieldOf("update_delay", (Object)0).forGetter(o -> o.updateDelay), (App)DIRECTION_SET.lenientOptionalFieldOf("facings").forGetter(o -> Optional.ofNullable(o.getFacingData()))).apply((Applicative)i, ChargeCursor::new));

        private ChargeCursor(BlockPos pos, int charge, int decayDelay, int updateDelay, Optional<Set<Direction>> facings) {
            this.pos = pos;
            this.charge = charge;
            this.decayDelay = decayDelay;
            this.updateDelay = updateDelay;
            this.facings = facings.orElse(null);
        }

        public ChargeCursor(BlockPos pos, int charge) {
            this(pos, charge, 1, 0, Optional.empty());
        }

        public BlockPos getPos() {
            return this.pos;
        }

        private boolean isPosUnreasonable(BlockPos originPos) {
            return this.pos.distChessboard(originPos) > 1024;
        }

        public int getCharge() {
            return this.charge;
        }

        public int getDecayDelay() {
            return this.decayDelay;
        }

        public @Nullable Set<Direction> getFacingData() {
            return this.facings;
        }

        private boolean shouldUpdate(LevelAccessor level, BlockPos pos, boolean isWorldGen) {
            if (this.charge <= 0) {
                return false;
            }
            if (isWorldGen) {
                return true;
            }
            if (level instanceof ServerLevel) {
                ServerLevel serverLevel = (ServerLevel)level;
                return serverLevel.shouldTickBlocksAt(pos);
            }
            return false;
        }

        public void update(LevelAccessor level, BlockPos originPos, RandomSource random, SculkSpreader spreader, boolean spreadVeins) {
            if (!this.shouldUpdate(level, originPos, spreader.isWorldGeneration)) {
                return;
            }
            if (this.updateDelay > 0) {
                --this.updateDelay;
                return;
            }
            BlockState currentState = level.getBlockState(this.pos);
            SculkBehaviour sculkBehaviour = ChargeCursor.getBlockBehaviour(currentState);
            if (spreadVeins && sculkBehaviour.attemptSpreadVein(level, this.pos, currentState, this.facings, spreader.isWorldGeneration())) {
                if (sculkBehaviour.canChangeBlockStateOnSpread()) {
                    currentState = level.getBlockState(this.pos);
                    sculkBehaviour = ChargeCursor.getBlockBehaviour(currentState);
                }
                level.playSound(null, this.pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            this.charge = sculkBehaviour.attemptUseCharge(this, level, originPos, random, spreader, spreadVeins);
            if (this.charge <= 0) {
                sculkBehaviour.onDischarged(level, currentState, this.pos, random);
                return;
            }
            BlockPos transferPos = ChargeCursor.getValidMovementPos(level, this.pos, random);
            if (transferPos != null) {
                sculkBehaviour.onDischarged(level, currentState, this.pos, random);
                this.pos = transferPos.immutable();
                if (spreader.isWorldGeneration() && !this.pos.closerThan(new Vec3i(originPos.getX(), this.pos.getY(), originPos.getZ()), 15.0)) {
                    this.charge = 0;
                    return;
                }
                currentState = level.getBlockState(transferPos);
            }
            if (currentState.getBlock() instanceof SculkBehaviour) {
                this.facings = MultifaceBlock.availableFaces(currentState);
            }
            this.decayDelay = sculkBehaviour.updateDecayDelay(this.decayDelay);
            this.updateDelay = sculkBehaviour.getSculkSpreadDelay();
        }

        private void mergeWith(ChargeCursor other) {
            this.charge += other.charge;
            other.charge = 0;
            this.updateDelay = Math.min(this.updateDelay, other.updateDelay);
        }

        private static SculkBehaviour getBlockBehaviour(BlockState state) {
            SculkBehaviour behaviour;
            Block block = state.getBlock();
            return block instanceof SculkBehaviour ? (behaviour = (SculkBehaviour)((Object)block)) : SculkBehaviour.DEFAULT;
        }

        private static List<Vec3i> getRandomizedNonCornerNeighbourOffsets(RandomSource random) {
            return Util.shuffledCopy(NON_CORNER_NEIGHBOURS, random);
        }

        private static @Nullable BlockPos getValidMovementPos(LevelAccessor level, BlockPos pos, RandomSource random) {
            BlockPos.MutableBlockPos sculkPosition = pos.mutable();
            BlockPos.MutableBlockPos neighbour = pos.mutable();
            for (Vec3i offset : ChargeCursor.getRandomizedNonCornerNeighbourOffsets(random)) {
                neighbour.setWithOffset((Vec3i)pos, offset);
                BlockState transferee = level.getBlockState(neighbour);
                if (!(transferee.getBlock() instanceof SculkBehaviour) || !ChargeCursor.isMovementUnobstructed(level, pos, neighbour)) continue;
                sculkPosition.set(neighbour);
                if (!SculkVeinBlock.hasSubstrateAccess(level, transferee, neighbour)) continue;
                break;
            }
            return sculkPosition.equals(pos) ? null : sculkPosition;
        }

        private static boolean isMovementUnobstructed(LevelAccessor level, BlockPos from, BlockPos to) {
            if (from.distManhattan(to) == 1) {
                return true;
            }
            BlockPos delta = to.subtract(from);
            Direction directionX = Direction.fromAxisAndDirection(Direction.Axis.X, delta.getX() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            Direction directionY = Direction.fromAxisAndDirection(Direction.Axis.Y, delta.getY() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            Direction directionZ = Direction.fromAxisAndDirection(Direction.Axis.Z, delta.getZ() < 0 ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            if (delta.getX() == 0) {
                return ChargeCursor.isUnobstructed(level, from, directionY) || ChargeCursor.isUnobstructed(level, from, directionZ);
            }
            if (delta.getY() == 0) {
                return ChargeCursor.isUnobstructed(level, from, directionX) || ChargeCursor.isUnobstructed(level, from, directionZ);
            }
            return ChargeCursor.isUnobstructed(level, from, directionX) || ChargeCursor.isUnobstructed(level, from, directionY);
        }

        private static boolean isUnobstructed(LevelAccessor level, BlockPos from, Direction direction) {
            BlockPos testPos = from.relative(direction);
            return !level.getBlockState(testPos).isFaceSturdy(level, testPos, direction.getOpposite());
        }
    }
}

