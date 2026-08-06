/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BannerBlockEntity
extends BlockEntity
implements Nameable {
    public static final int MAX_PATTERNS = 6;
    private static final String TAG_PATTERNS = "patterns";
    private static final Component DEFAULT_NAME = Component.translatable("block.minecraft.banner");
    private @Nullable Component name;
    private final DyeColor baseColor;
    private BannerPatternLayers patterns = BannerPatternLayers.EMPTY;

    public BannerBlockEntity(BlockPos worldPosition, BlockState blockState) {
        this(worldPosition, blockState, ((AbstractBannerBlock)blockState.getBlock()).getColor());
    }

    public BannerBlockEntity(BlockPos worldPosition, BlockState blockState, DyeColor color) {
        super(BlockEntityTypes.BANNER, worldPosition, blockState);
        this.baseColor = color;
    }

    @Override
    public Component getName() {
        if (this.name != null) {
            return this.name;
        }
        return DEFAULT_NAME;
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.name;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.patterns.equals(BannerPatternLayers.EMPTY)) {
            output.store(TAG_PATTERNS, BannerPatternLayers.CODEC, this.patterns);
        }
        output.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.name = BannerBlockEntity.parseCustomNameSafe(input, "CustomName");
        this.patterns = input.read(TAG_PATTERNS, BannerPatternLayers.CODEC).orElse(BannerPatternLayers.EMPTY);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    public BannerPatternLayers getPatterns() {
        return this.patterns;
    }

    public ItemStack getItem() {
        ItemStack itemStack = new ItemStack(this.getBlockState().getBlock());
        itemStack.applyComponents(this.collectComponents());
        return itemStack;
    }

    public DyeColor getBaseColor() {
        return this.baseColor;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.patterns = components.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        this.name = components.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.BANNER_PATTERNS, this.patterns);
        components.set(DataComponents.CUSTOM_NAME, this.name);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard(TAG_PATTERNS);
        output.discard("CustomName");
    }
}

