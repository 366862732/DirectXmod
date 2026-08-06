/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 */
package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class JigsawBlockEntity
extends BlockEntity {
    public static final Codec<ResourceKey<StructureTemplatePool>> POOL_CODEC = ResourceKey.codec(Registries.TEMPLATE_POOL);
    public static final Identifier EMPTY_ID = Identifier.withDefaultNamespace("empty");
    private static final int DEFAULT_PLACEMENT_PRIORITY = 0;
    private static final int DEFAULT_SELECTION_PRIORITY = 0;
    public static final String TARGET = "target";
    public static final String POOL = "pool";
    public static final String JOINT = "joint";
    public static final String PLACEMENT_PRIORITY = "placement_priority";
    public static final String SELECTION_PRIORITY = "selection_priority";
    public static final String NAME = "name";
    public static final String FINAL_STATE = "final_state";
    public static final String DEFAULT_FINAL_STATE = "minecraft:air";
    private Identifier name = EMPTY_ID;
    private Identifier target = EMPTY_ID;
    private ResourceKey<StructureTemplatePool> pool = Pools.EMPTY;
    private JointType joint = JointType.ROLLABLE;
    private String finalState = "minecraft:air";
    private int placementPriority = 0;
    private int selectionPriority = 0;

    public JigsawBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.JIGSAW, worldPosition, blockState);
    }

    public Identifier getName() {
        return this.name;
    }

    public Identifier getTarget() {
        return this.target;
    }

    public ResourceKey<StructureTemplatePool> getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public JointType getJoint() {
        return this.joint;
    }

    public int getPlacementPriority() {
        return this.placementPriority;
    }

    public int getSelectionPriority() {
        return this.selectionPriority;
    }

    public void setName(Identifier name) {
        this.name = name;
    }

    public void setTarget(Identifier target) {
        this.target = target;
    }

    public void setPool(ResourceKey<StructureTemplatePool> pool) {
        this.pool = pool;
    }

    public void setFinalState(String finalState) {
        this.finalState = finalState;
    }

    public void setJoint(JointType joint) {
        this.joint = joint;
    }

    public void setPlacementPriority(int placementPriority) {
        this.placementPriority = placementPriority;
    }

    public void setSelectionPriority(int selectionPriority) {
        this.selectionPriority = selectionPriority;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(NAME, Identifier.CODEC, this.name);
        output.store(TARGET, Identifier.CODEC, this.target);
        output.store(POOL, POOL_CODEC, this.pool);
        output.putString(FINAL_STATE, this.finalState);
        output.store(JOINT, JointType.CODEC, this.joint);
        output.putInt(PLACEMENT_PRIORITY, this.placementPriority);
        output.putInt(SELECTION_PRIORITY, this.selectionPriority);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.name = input.read(NAME, Identifier.CODEC).orElse(EMPTY_ID);
        this.target = input.read(TARGET, Identifier.CODEC).orElse(EMPTY_ID);
        this.pool = input.read(POOL, POOL_CODEC).orElse(Pools.EMPTY);
        this.finalState = input.getStringOr(FINAL_STATE, DEFAULT_FINAL_STATE);
        this.joint = input.read(JOINT, JointType.CODEC).orElseGet(() -> StructureTemplate.getDefaultJointType(this.getBlockState()));
        this.placementPriority = input.getIntOr(PLACEMENT_PRIORITY, 0);
        this.selectionPriority = input.getIntOr(SELECTION_PRIORITY, 0);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    public void generate(ServerLevel level, int levels, boolean keepJigsaws) {
        BlockPos position = this.getBlockPos().relative(this.getBlockState().getValue(JigsawBlock.ORIENTATION).front());
        HolderLookup.RegistryLookup poolRegistry = level.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        Holder.Reference pool = poolRegistry.getOrThrow(this.pool);
        JigsawPlacement.generateJigsaw(level, pool, this.target, levels, position, keepJigsaws);
    }

    public static enum JointType implements StringRepresentable
    {
        ROLLABLE("rollable"),
        ALIGNED("aligned");

        public static final StringRepresentable.EnumCodec<JointType> CODEC;
        private final String name;

        private JointType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Component getTranslatedName() {
            return Component.translatable("jigsaw_block.joint." + this.name);
        }

        static {
            CODEC = StringRepresentable.fromEnum(JointType::values);
        }
    }
}

