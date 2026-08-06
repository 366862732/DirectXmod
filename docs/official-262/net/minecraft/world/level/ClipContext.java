/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClipContext {
    private final Vec3 from;
    private final Vec3 to;
    private final Block block;
    private final Fluid fluid;
    private final CollisionContext collisionContext;

    public ClipContext(Vec3 from, Vec3 to, Block block, Fluid fluid, Entity entity) {
        this(from, to, block, fluid, CollisionContext.of(entity));
    }

    public ClipContext(Vec3 from, Vec3 to, Block block, Fluid fluid, CollisionContext collisionContext) {
        this.from = from;
        this.to = to;
        this.block = block;
        this.fluid = fluid;
        this.collisionContext = collisionContext;
    }

    public Vec3 getTo() {
        return this.to;
    }

    public Vec3 getFrom() {
        return this.from;
    }

    public VoxelShape getBlockShape(BlockState blockState, BlockGetter level, BlockPos pos) {
        return this.block.get(blockState, level, pos, this.collisionContext);
    }

    public VoxelShape getFluidShape(FluidState fluidState, BlockGetter level, BlockPos pos) {
        return this.fluid.canPick(fluidState) ? fluidState.getShape(level, pos) : Shapes.empty();
    }

    public static enum Block implements ShapeGetter
    {
        COLLIDER(BlockBehaviour.BlockStateBase::getCollisionShape),
        OUTLINE(BlockBehaviour.BlockStateBase::getShape),
        VISUAL(BlockBehaviour.BlockStateBase::getVisualShape),
        FALLDAMAGE_RESETTING((state, level, pos, collisionContext) -> {
            EntityCollisionContext entityCollisionContext;
            if (state.is(BlockTags.FALL_DAMAGE_RESETTING)) {
                return Shapes.block();
            }
            if (collisionContext instanceof EntityCollisionContext && (entityCollisionContext = (EntityCollisionContext)collisionContext).getEntity() != null && entityCollisionContext.getEntity().is(EntityTypes.PLAYER)) {
                if (state.is(Blocks.END_GATEWAY) || state.is(Blocks.END_PORTAL)) {
                    return Shapes.block();
                }
                if (level instanceof ServerLevel) {
                    ServerLevel serverLevel = (ServerLevel)level;
                    if (state.is(Blocks.NETHER_PORTAL) && serverLevel.getGameRules().get(GameRules.PLAYERS_NETHER_PORTAL_DEFAULT_DELAY) == 0) {
                        return Shapes.block();
                    }
                }
            }
            return Shapes.empty();
        });

        private final ShapeGetter shapeGetter;

        private Block(ShapeGetter getShape) {
            this.shapeGetter = getShape;
        }

        @Override
        public VoxelShape get(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return this.shapeGetter.get(state, level, pos, context);
        }
    }

    public static enum Fluid {
        NONE(state -> false),
        SOURCE_ONLY(FluidState::isSource),
        ANY(state -> !state.isEmpty()),
        WATER(fluidState -> fluidState.is(FluidTags.WATER));

        private final Predicate<FluidState> canPick;

        private Fluid(Predicate<FluidState> canPick) {
            this.canPick = canPick;
        }

        public boolean canPick(FluidState fluidState) {
            return this.canPick.test(fluidState);
        }
    }

    public static interface ShapeGetter {
        public VoxelShape get(BlockState var1, BlockGetter var2, BlockPos var3, CollisionContext var4);
    }
}

