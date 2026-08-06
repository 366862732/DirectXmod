/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.ints.Int2ObjectMap
 *  it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
 */
package net.minecraft.world.level.pathfinder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.Target;

public abstract class NodeEvaluator {
    protected PathfindingContext currentContext;
    protected Mob mob;
    protected final Int2ObjectMap<Node> nodes = new Int2ObjectOpenHashMap();
    protected int entityWidth;
    protected int entityHeight;
    protected int entityDepth;
    protected boolean canPassDoors = true;
    protected boolean canOpenDoors;
    protected boolean canFloat;
    protected boolean canWalkOverFences;

    public void prepare(PathNavigationRegion level, Mob entity) {
        this.currentContext = new PathfindingContext(level, entity);
        this.mob = entity;
        this.nodes.clear();
        this.entityWidth = Mth.floor(entity.getBbWidth() + 1.0f);
        this.entityHeight = Mth.floor(entity.getBbHeight() + 1.0f);
        this.entityDepth = Mth.floor(entity.getBbWidth() + 1.0f);
    }

    public void done() {
        this.currentContext = null;
        this.mob = null;
    }

    protected Node getNode(BlockPos pos) {
        return this.getNode(pos.getX(), pos.getY(), pos.getZ());
    }

    protected Node getNode(int x, int y, int z) {
        return (Node)this.nodes.computeIfAbsent(Node.createHash(x, y, z), k -> new Node(x, y, z));
    }

    public abstract Node getStart();

    public abstract Target getTarget(double var1, double var3, double var5);

    protected Target getTargetNodeAt(double x, double y, double z) {
        return new Target(this.getNode(Mth.floor(x), Mth.floor(y), Mth.floor(z)));
    }

    public abstract int getNeighbors(Node[] var1, Node var2);

    public abstract PathType getPathTypeOfMob(PathfindingContext var1, int var2, int var3, int var4, Mob var5);

    public abstract PathType getPathType(PathfindingContext var1, int var2, int var3, int var4);

    public PathType getPathType(Mob mob, BlockPos pos) {
        return this.getPathType(new PathfindingContext(mob.level(), mob), pos.getX(), pos.getY(), pos.getZ());
    }

    public void setCanPassDoors(boolean canPassDoors) {
        this.canPassDoors = canPassDoors;
    }

    public void setCanOpenDoors(boolean canOpenDoors) {
        this.canOpenDoors = canOpenDoors;
    }

    public void setCanFloat(boolean canFloat) {
        this.canFloat = canFloat;
    }

    public void setCanWalkOverFences(boolean canWalkOverFences) {
        this.canWalkOverFences = canWalkOverFences;
    }

    public boolean canPassDoors() {
        return this.canPassDoors;
    }

    public boolean canOpenDoors() {
        return this.canOpenDoors;
    }

    public boolean canFloat() {
        return this.canFloat;
    }

    public boolean canWalkOverFences() {
        return this.canWalkOverFences;
    }

    public static boolean isBurningBlock(BlockState blockState) {
        return blockState.is(BlockTags.FIRE) || blockState.is(Blocks.LAVA) || blockState.is(Blocks.MAGMA_BLOCK) || CampfireBlock.isLitCampfire(blockState) || blockState.is(Blocks.LAVA_CAULDRON);
    }
}

