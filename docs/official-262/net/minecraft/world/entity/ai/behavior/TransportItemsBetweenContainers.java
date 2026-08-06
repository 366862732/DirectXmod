/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  org.apache.commons.lang3.function.TriConsumer
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.function.TriConsumer;
import org.jspecify.annotations.Nullable;

public class TransportItemsBetweenContainers
extends Behavior<PathfinderMob> {
    public static final int TARGET_INTERACTION_TIME = 60;
    private static final int VISITED_POSITIONS_MEMORY_TIME = 6000;
    private static final int TRANSPORTED_ITEM_MAX_STACK_SIZE = 16;
    private static final int MAX_VISITED_POSITIONS = 10;
    private static final int MAX_UNREACHABLE_POSITIONS = 50;
    private static final int PASSENGER_MOB_TARGET_SEARCH_DISTANCE = 1;
    private static final int IDLE_COOLDOWN = 140;
    private static final double CLOSE_ENOUGH_TO_START_QUEUING_DISTANCE = 3.0;
    private static final double CLOSE_ENOUGH_TO_START_INTERACTING_WITH_TARGET_DISTANCE = 0.5;
    private static final double CLOSE_ENOUGH_TO_START_INTERACTING_WITH_TARGET_PATH_END_DISTANCE = 1.0;
    private static final double CLOSE_ENOUGH_TO_CONTINUE_INTERACTING_WITH_TARGET = 2.0;
    private final float speedModifier;
    private final int horizontalSearchDistance;
    private final int verticalSearchDistance;
    private final Predicate<BlockState> sourceBlockType;
    private final Predicate<BlockState> destinationBlockType;
    private final Predicate<TransportItemTarget> shouldQueueForTarget;
    private final Consumer<PathfinderMob> onStartTravelling;
    private final Map<ContainerInteractionState, OnTargetReachedInteraction> onTargetInteractionActions;
    private @Nullable TransportItemTarget target = null;
    private TransportItemState state;
    private @Nullable ContainerInteractionState interactionState;
    private int ticksSinceReachingTarget;

    public TransportItemsBetweenContainers(float speedModifier, Predicate<BlockState> sourceBlockType, Predicate<BlockState> destinationBlockType, int horizontalSearchDistance, int verticalSearchDistance, Map<ContainerInteractionState, OnTargetReachedInteraction> onTargetInteractionActions, Consumer<PathfinderMob> onStartTravelling, Predicate<TransportItemTarget> shouldQueueForTarget) {
        super((Map<MemoryModuleType<?>, MemoryStatus>)ImmutableMap.of(MemoryModuleType.VISITED_BLOCK_POSITIONS, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS, (Object)((Object)MemoryStatus.REGISTERED), MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, (Object)((Object)MemoryStatus.VALUE_ABSENT), MemoryModuleType.IS_PANICKING, (Object)((Object)MemoryStatus.VALUE_ABSENT)));
        this.speedModifier = speedModifier;
        this.sourceBlockType = sourceBlockType;
        this.destinationBlockType = destinationBlockType;
        this.horizontalSearchDistance = horizontalSearchDistance;
        this.verticalSearchDistance = verticalSearchDistance;
        this.onStartTravelling = onStartTravelling;
        this.shouldQueueForTarget = shouldQueueForTarget;
        this.onTargetInteractionActions = onTargetInteractionActions;
        this.state = TransportItemState.TRAVELLING;
    }

    @Override
    protected void start(ServerLevel level, PathfinderMob body, long timestamp) {
        PathNavigation pathNavigation = body.getNavigation();
        if (pathNavigation instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigation2 = (GroundPathNavigation)pathNavigation;
            pathNavigation2.setCanPathToTargetsBelowSurface(true);
        }
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, PathfinderMob body) {
        return !body.isLeashed();
    }

    @Override
    protected boolean canStillUse(ServerLevel level, PathfinderMob body, long timestamp) {
        return body.getBrain().getMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS).isEmpty() && !body.isPanicking() && !body.isLeashed();
    }

    @Override
    protected boolean timedOut(long timestamp) {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, PathfinderMob body, long timestamp) {
        boolean updatedInvalidTarget = this.updateInvalidTarget(level, body);
        if (this.target == null) {
            this.stop(level, body, timestamp);
            return;
        }
        if (updatedInvalidTarget) {
            return;
        }
        if (this.state.equals((Object)TransportItemState.QUEUING)) {
            this.onQueuingForTarget(this.target, level, body);
        }
        if (this.state.equals((Object)TransportItemState.TRAVELLING)) {
            this.onTravelToTarget(this.target, level, body);
        }
        if (this.state.equals((Object)TransportItemState.INTERACTING)) {
            this.onReachedTarget(this.target, level, body);
        }
    }

    private boolean updateInvalidTarget(ServerLevel level, PathfinderMob body) {
        if (!this.hasValidTarget(level, body)) {
            this.stopTargetingCurrentTarget(body);
            Optional<TransportItemTarget> targetBlockPosition = this.getTransportTarget(level, body);
            if (targetBlockPosition.isPresent()) {
                this.target = targetBlockPosition.get();
                this.onStartTravelling(body);
                this.setVisitedBlockPos(body, level, this.target.pos);
                return true;
            }
            this.enterCooldownAfterNoMatchingTargetFound(body);
            return true;
        }
        return false;
    }

    private void onQueuingForTarget(TransportItemTarget target, Level level, PathfinderMob body) {
        if (!this.isAnotherMobInteractingWithTarget(target, level)) {
            this.resumeTravelling(body);
        }
    }

    protected void onTravelToTarget(TransportItemTarget target, Level level, PathfinderMob body) {
        if (this.isWithinTargetDistance(3.0, target, level, body, this.getCenterPos(body)) && this.isAnotherMobInteractingWithTarget(target, level)) {
            this.startQueuing(body);
        } else if (this.isWithinTargetDistance(TransportItemsBetweenContainers.getInteractionRange(body), target, level, body, this.getCenterPos(body))) {
            this.startOnReachedTargetInteraction(target, body);
        } else {
            this.walkTowardsTarget(body);
        }
    }

    private Vec3 getCenterPos(PathfinderMob body) {
        return this.setMiddleYPosition(body, body.position());
    }

    protected void onReachedTarget(TransportItemTarget target, Level level, PathfinderMob body) {
        if (!this.isWithinTargetDistance(2.0, target, level, body, this.getCenterPos(body))) {
            this.onStartTravelling(body);
        } else {
            ++this.ticksSinceReachingTarget;
            this.onTargetInteraction(target, body);
            if (this.ticksSinceReachingTarget >= 60) {
                this.doReachedTargetInteraction(body, target.container, this::pickUpItems, (mob, container) -> this.stopTargetingCurrentTarget(body), this::putDownItem, (mob, container) -> this.stopTargetingCurrentTarget(body));
                this.onStartTravelling(body);
            }
        }
    }

    private void startQueuing(PathfinderMob body) {
        this.stopInPlace(body);
        this.setTransportingState(TransportItemState.QUEUING);
    }

    private void resumeTravelling(PathfinderMob body) {
        this.setTransportingState(TransportItemState.TRAVELLING);
        this.walkTowardsTarget(body);
    }

    private void walkTowardsTarget(PathfinderMob body) {
        if (this.target != null) {
            BehaviorUtils.setWalkAndLookTargetMemories((LivingEntity)body, this.target.pos, this.speedModifier, 0);
        }
    }

    private void startOnReachedTargetInteraction(TransportItemTarget target, PathfinderMob body) {
        this.doReachedTargetInteraction(body, target.container, this.onReachedInteraction(ContainerInteractionState.PICKUP_ITEM), this.onReachedInteraction(ContainerInteractionState.PICKUP_NO_ITEM), this.onReachedInteraction(ContainerInteractionState.PLACE_ITEM), this.onReachedInteraction(ContainerInteractionState.PLACE_NO_ITEM));
        this.setTransportingState(TransportItemState.INTERACTING);
    }

    private void onStartTravelling(PathfinderMob body) {
        this.onStartTravelling.accept(body);
        this.setTransportingState(TransportItemState.TRAVELLING);
        this.interactionState = null;
        this.ticksSinceReachingTarget = 0;
    }

    private BiConsumer<PathfinderMob, Container> onReachedInteraction(ContainerInteractionState state) {
        return (mob, container) -> this.setInteractionState(state);
    }

    private void setTransportingState(TransportItemState state) {
        this.state = state;
    }

    private void setInteractionState(ContainerInteractionState state) {
        this.interactionState = state;
    }

    private void onTargetInteraction(TransportItemTarget target, PathfinderMob body) {
        body.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(target.pos));
        this.stopInPlace(body);
        if (this.interactionState != null) {
            Optional.ofNullable(this.onTargetInteractionActions.get((Object)this.interactionState)).ifPresent(action -> action.accept(body, target, this.ticksSinceReachingTarget));
        }
    }

    private void doReachedTargetInteraction(PathfinderMob body, Container container, BiConsumer<PathfinderMob, Container> onPickupSuccess, BiConsumer<PathfinderMob, Container> onPickupFailure, BiConsumer<PathfinderMob, Container> onPlaceSuccess, BiConsumer<PathfinderMob, Container> onPlaceFailure) {
        if (TransportItemsBetweenContainers.isPickingUpItems(body)) {
            if (TransportItemsBetweenContainers.matchesGettingItemsRequirement(container)) {
                onPickupSuccess.accept(body, container);
            } else {
                onPickupFailure.accept(body, container);
            }
        } else if (TransportItemsBetweenContainers.matchesLeavingItemsRequirement(body, container)) {
            onPlaceSuccess.accept(body, container);
        } else {
            onPlaceFailure.accept(body, container);
        }
    }

    private Optional<TransportItemTarget> getTransportTarget(ServerLevel level, PathfinderMob body) {
        AABB targetBlockSearchArea = this.getTargetSearchArea(body);
        Set<GlobalPos> visitedPositions = TransportItemsBetweenContainers.getVisitedPositions(body);
        Set<GlobalPos> unreachablePositions = TransportItemsBetweenContainers.getUnreachablePositions(body);
        List<ChunkPos> list = ChunkPos.rangeClosed(ChunkPos.containing(body.blockPosition()), Math.floorDiv(this.getHorizontalSearchDistance(body), 16) + 1).toList();
        TransportItemTarget target = null;
        double closestDistance = 3.4028234663852886E38;
        for (ChunkPos chunkPos : list) {
            LevelChunk levelChunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (levelChunk == null) continue;
            for (BlockEntity potentialTarget : levelChunk.getBlockEntities().values()) {
                TransportItemTarget targetValidToPick;
                ChestBlockEntity chestBlockEntity;
                double distance;
                if (!(potentialTarget instanceof ChestBlockEntity) || !((distance = (chestBlockEntity = (ChestBlockEntity)potentialTarget).getBlockPos().distToCenterSqr(body.position())) < closestDistance) || (targetValidToPick = this.isTargetValidToPick(body, level, chestBlockEntity, visitedPositions, unreachablePositions, targetBlockSearchArea)) == null) continue;
                target = targetValidToPick;
                closestDistance = distance;
            }
        }
        return target == null ? Optional.empty() : Optional.of(target);
    }

    private @Nullable TransportItemTarget isTargetValidToPick(PathfinderMob body, Level level, BlockEntity blockEntity, Set<GlobalPos> visitedPositions, Set<GlobalPos> unreachablePositions, AABB targetBlockSearchArea) {
        BlockPos blockPos = blockEntity.getBlockPos();
        boolean isWithinSearchArea = targetBlockSearchArea.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        if (!isWithinSearchArea) {
            return null;
        }
        TransportItemTarget transportItemTarget = TransportItemTarget.tryCreatePossibleTarget(blockEntity, level);
        if (transportItemTarget == null) {
            return null;
        }
        boolean isValidTarget = this.isWantedBlock(body, transportItemTarget.state) && !this.isPositionAlreadyVisited(visitedPositions, unreachablePositions, transportItemTarget, level) && !this.isContainerLocked(transportItemTarget);
        return isValidTarget ? transportItemTarget : null;
    }

    private boolean isContainerLocked(TransportItemTarget transportItemTarget) {
        BaseContainerBlockEntity blockEntity;
        BlockEntity blockEntity2 = transportItemTarget.blockEntity;
        return blockEntity2 instanceof BaseContainerBlockEntity && (blockEntity = (BaseContainerBlockEntity)blockEntity2).isLocked();
    }

    private boolean hasValidTarget(Level level, PathfinderMob body) {
        boolean targetIsOfValidType;
        boolean bl = targetIsOfValidType = this.target != null && this.isWantedBlock(body, this.target.state) && this.targetHasNotChanged(level, this.target);
        if (targetIsOfValidType && !this.isTargetBlocked(level, this.target)) {
            if (!this.state.equals((Object)TransportItemState.TRAVELLING)) {
                return true;
            }
            if (this.hasValidTravellingPath(level, this.target, body)) {
                return true;
            }
            this.markVisitedBlockPosAsUnreachable(body, level, this.target.pos);
        }
        return false;
    }

    private boolean hasValidTravellingPath(Level level, TransportItemTarget target, PathfinderMob body) {
        Path path = body.getNavigation().getPath() == null ? body.getNavigation().createPath(target.pos, 0) : body.getNavigation().getPath();
        Vec3 posFromWhichToReachTarget = this.getPositionToReachTargetFrom(path, body);
        boolean canReachTarget = this.isWithinTargetDistance(TransportItemsBetweenContainers.getInteractionRange(body), target, level, body, posFromWhichToReachTarget);
        boolean hasNotYetCreatedPathToTarget = path == null && !canReachTarget;
        return hasNotYetCreatedPathToTarget || this.targetIsReachableFromPosition(level, canReachTarget, posFromWhichToReachTarget, target, body);
    }

    private Vec3 getPositionToReachTargetFrom(@Nullable Path path, PathfinderMob body) {
        boolean haveNoValidPath = path == null || path.getEndNode() == null;
        Vec3 bottomCenter = haveNoValidPath ? body.position() : Vec3.atBottomCenterOf(path.getEndNode().asBlockPos());
        return this.setMiddleYPosition(body, bottomCenter);
    }

    private Vec3 setMiddleYPosition(PathfinderMob body, Vec3 pos) {
        return pos.add(0.0, body.getBoundingBox().getYsize() / 2.0, 0.0);
    }

    private boolean isTargetBlocked(Level level, TransportItemTarget target) {
        return ChestBlock.isChestBlockedAt(level, target.pos);
    }

    private boolean targetHasNotChanged(Level level, TransportItemTarget target) {
        return target.blockEntity.equals(level.getBlockEntity(target.pos));
    }

    private Stream<TransportItemTarget> getConnectedTargets(TransportItemTarget target, Level level) {
        if (target.state.getValueOrElse(ChestBlock.TYPE, ChestType.SINGLE) != ChestType.SINGLE) {
            TransportItemTarget connectedTarget = TransportItemTarget.tryCreatePossibleTarget(ChestBlock.getConnectedBlockPos(target.pos, target.state), level);
            return connectedTarget != null ? Stream.of(target, connectedTarget) : Stream.of(target);
        }
        return Stream.of(target);
    }

    private AABB getTargetSearchArea(PathfinderMob mob) {
        int horizontalSearchDistance = this.getHorizontalSearchDistance(mob);
        return new AABB(mob.blockPosition()).inflate(horizontalSearchDistance, this.getVerticalSearchDistance(mob), horizontalSearchDistance);
    }

    private int getHorizontalSearchDistance(PathfinderMob mob) {
        return mob.isPassenger() ? 1 : this.horizontalSearchDistance;
    }

    private int getVerticalSearchDistance(PathfinderMob mob) {
        return mob.isPassenger() ? 1 : this.verticalSearchDistance;
    }

    private static Set<GlobalPos> getVisitedPositions(PathfinderMob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS).orElse(Set.of());
    }

    private static Set<GlobalPos> getUnreachablePositions(PathfinderMob mob) {
        return mob.getBrain().getMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS).orElse(Set.of());
    }

    private boolean isPositionAlreadyVisited(Set<GlobalPos> visitedPositions, Set<GlobalPos> unreachablePositions, TransportItemTarget target, Level level) {
        return this.getConnectedTargets(target, level).map(transportItemTarget -> new GlobalPos(level.dimension(), transportItemTarget.pos)).anyMatch(pos -> visitedPositions.contains(pos) || unreachablePositions.contains(pos));
    }

    private static boolean hasFinishedPath(PathfinderMob body) {
        return body.getNavigation().getPath() != null && body.getNavigation().getPath().isDone();
    }

    protected void setVisitedBlockPos(PathfinderMob body, Level level, BlockPos target) {
        HashSet<GlobalPos> visitedPositions = new HashSet<GlobalPos>(TransportItemsBetweenContainers.getVisitedPositions(body));
        visitedPositions.add(new GlobalPos(level.dimension(), target));
        if (visitedPositions.size() > 10) {
            this.enterCooldownAfterNoMatchingTargetFound(body);
        } else {
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, visitedPositions, 6000L);
        }
    }

    protected void markVisitedBlockPosAsUnreachable(PathfinderMob body, Level level, BlockPos target) {
        HashSet<GlobalPos> visitedPositions = new HashSet<GlobalPos>(TransportItemsBetweenContainers.getVisitedPositions(body));
        visitedPositions.remove(new GlobalPos(level.dimension(), target));
        HashSet<GlobalPos> unreachablePositions = new HashSet<GlobalPos>(TransportItemsBetweenContainers.getUnreachablePositions(body));
        unreachablePositions.add(new GlobalPos(level.dimension(), target));
        if (unreachablePositions.size() > 50) {
            this.enterCooldownAfterNoMatchingTargetFound(body);
        } else {
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.VISITED_BLOCK_POSITIONS, visitedPositions, 6000L);
            body.getBrain().setMemoryWithExpiry(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS, unreachablePositions, 6000L);
        }
    }

    private boolean isWantedBlock(PathfinderMob mob, BlockState block) {
        return TransportItemsBetweenContainers.isPickingUpItems(mob) ? this.sourceBlockType.test(block) : this.destinationBlockType.test(block);
    }

    private static double getInteractionRange(PathfinderMob body) {
        return TransportItemsBetweenContainers.hasFinishedPath(body) ? 1.0 : 0.5;
    }

    private boolean isWithinTargetDistance(double distance, TransportItemTarget target, Level level, PathfinderMob body, Vec3 fromPos) {
        AABB boundingBox = body.getBoundingBox();
        AABB movedBoundBox = AABB.ofSize(fromPos, boundingBox.getXsize(), boundingBox.getYsize(), boundingBox.getZsize());
        return target.state.getCollisionShape(level, target.pos).bounds().inflate(distance, 0.5, distance).move(target.pos).intersects(movedBoundBox);
    }

    private boolean targetIsReachableFromPosition(Level level, boolean canReachTarget, Vec3 pos, TransportItemTarget target, PathfinderMob body) {
        return canReachTarget && this.canSeeAnyTargetSide(target, level, body, pos);
    }

    private boolean canSeeAnyTargetSide(TransportItemTarget target, Level level, PathfinderMob body, Vec3 eyePosition) {
        Vec3 center = Vec3.atCenterOf(target.pos);
        return Direction.stream().map(direction -> center.add(0.5 * (double)direction.getStepX(), 0.5 * (double)direction.getStepY(), 0.5 * (double)direction.getStepZ())).map(hitTarget -> level.clip(new ClipContext(eyePosition, (Vec3)hitTarget, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body))).anyMatch(hitResult -> hitResult.getType() == HitResult.Type.BLOCK && hitResult.getBlockPos().equals(target.pos));
    }

    private boolean isAnotherMobInteractingWithTarget(TransportItemTarget target, Level level) {
        return this.getConnectedTargets(target, level).anyMatch(this.shouldQueueForTarget);
    }

    private static boolean isPickingUpItems(PathfinderMob body) {
        return body.getMainHandItem().isEmpty();
    }

    private static boolean matchesGettingItemsRequirement(Container container) {
        return !container.isEmpty();
    }

    private static boolean matchesLeavingItemsRequirement(PathfinderMob body, Container container) {
        return container.isEmpty() || TransportItemsBetweenContainers.hasItemMatchingHandItem(body, container);
    }

    private static boolean hasItemMatchingHandItem(PathfinderMob body, Container container) {
        ItemStack mainHandItem = body.getMainHandItem();
        for (ItemStack itemStack : container) {
            if (!ItemStack.isSameItem(itemStack, mainHandItem)) continue;
            return true;
        }
        return false;
    }

    private void pickUpItems(PathfinderMob body, Container container) {
        body.setItemSlot(EquipmentSlot.MAINHAND, TransportItemsBetweenContainers.pickupItemFromContainer(container));
        body.setGuaranteedDrop(EquipmentSlot.MAINHAND);
        container.setChanged();
        this.clearMemoriesAfterMatchingTargetFound(body);
    }

    private void putDownItem(PathfinderMob body, Container container) {
        ItemStack itemsLeftAfterVisitingChest = TransportItemsBetweenContainers.addItemsToContainer(body, container);
        container.setChanged();
        body.setItemSlot(EquipmentSlot.MAINHAND, itemsLeftAfterVisitingChest);
        if (itemsLeftAfterVisitingChest.isEmpty()) {
            this.clearMemoriesAfterMatchingTargetFound(body);
        } else {
            this.stopTargetingCurrentTarget(body);
        }
    }

    private static ItemStack pickupItemFromContainer(Container container) {
        int slot = 0;
        for (ItemStack itemStack : container) {
            if (!itemStack.isEmpty()) {
                int itemCount = Math.min(itemStack.getCount(), 16);
                return container.removeItem(slot, itemCount);
            }
            ++slot;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack addItemsToContainer(PathfinderMob body, Container container) {
        int slot = 0;
        ItemStack itemStack = body.getMainHandItem();
        for (ItemStack containerItemStack : container) {
            if (containerItemStack.isEmpty()) {
                container.setItem(slot, itemStack);
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(containerItemStack, itemStack) && containerItemStack.getCount() < containerItemStack.getMaxStackSize()) {
                int countThatCanBeAdded = containerItemStack.getMaxStackSize() - containerItemStack.getCount();
                int countToAdd = Math.min(countThatCanBeAdded, itemStack.getCount());
                containerItemStack.setCount(containerItemStack.getCount() + countToAdd);
                itemStack.setCount(itemStack.getCount() - countThatCanBeAdded);
                container.setItem(slot, containerItemStack);
                if (itemStack.isEmpty()) {
                    return ItemStack.EMPTY;
                }
            }
            ++slot;
        }
        return itemStack;
    }

    protected void stopTargetingCurrentTarget(PathfinderMob body) {
        this.ticksSinceReachingTarget = 0;
        this.target = null;
        body.getNavigation().stop();
        body.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    protected void clearMemoriesAfterMatchingTargetFound(PathfinderMob body) {
        this.stopTargetingCurrentTarget(body);
        body.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
        body.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
    }

    private void enterCooldownAfterNoMatchingTargetFound(PathfinderMob body) {
        this.stopTargetingCurrentTarget(body);
        body.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 140);
        body.getBrain().eraseMemory(MemoryModuleType.VISITED_BLOCK_POSITIONS);
        body.getBrain().eraseMemory(MemoryModuleType.UNREACHABLE_TRANSPORT_BLOCK_POSITIONS);
    }

    @Override
    protected void stop(ServerLevel level, PathfinderMob body, long timestamp) {
        this.onStartTravelling(body);
        PathNavigation pathNavigation = body.getNavigation();
        if (pathNavigation instanceof GroundPathNavigation) {
            GroundPathNavigation pathNavigation2 = (GroundPathNavigation)pathNavigation;
            pathNavigation2.setCanPathToTargetsBelowSurface(false);
        }
    }

    private void stopInPlace(PathfinderMob mob) {
        mob.getNavigation().stop();
        mob.setXxa(0.0f);
        mob.setYya(0.0f);
        mob.setSpeed(0.0f);
        mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
    }

    public record TransportItemTarget(BlockPos pos, Container container, BlockEntity blockEntity, BlockState state) {
        public static @Nullable TransportItemTarget tryCreatePossibleTarget(BlockEntity blockEntity, Level level) {
            BlockPos blockPos = blockEntity.getBlockPos();
            BlockState blockState = blockEntity.getBlockState();
            Container container = TransportItemTarget.getBlockEntityContainer(blockEntity, blockState, level, blockPos);
            if (container != null) {
                return new TransportItemTarget(blockPos, container, blockEntity, blockState);
            }
            return null;
        }

        public static @Nullable TransportItemTarget tryCreatePossibleTarget(BlockPos blockPos, Level level) {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            return blockEntity == null ? null : TransportItemTarget.tryCreatePossibleTarget(blockEntity, level);
        }

        private static @Nullable Container getBlockEntityContainer(BlockEntity blockEntity, BlockState blockState, Level level, BlockPos blockPos) {
            Block block = blockState.getBlock();
            if (block instanceof ChestBlock) {
                ChestBlock chestBlock = (ChestBlock)block;
                return ChestBlock.getContainer(chestBlock, blockState, level, blockPos, false);
            }
            if (blockEntity instanceof Container) {
                Container container = (Container)((Object)blockEntity);
                return container;
            }
            return null;
        }
    }

    public static enum TransportItemState {
        TRAVELLING,
        QUEUING,
        INTERACTING;

    }

    public static enum ContainerInteractionState {
        PICKUP_ITEM,
        PICKUP_NO_ITEM,
        PLACE_ITEM,
        PLACE_NO_ITEM;

    }

    @FunctionalInterface
    public static interface OnTargetReachedInteraction
    extends TriConsumer<PathfinderMob, TransportItemTarget, Integer> {
    }
}

