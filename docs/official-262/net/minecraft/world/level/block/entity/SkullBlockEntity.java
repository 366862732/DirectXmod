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
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SkullBlockEntity
extends BlockEntity {
    private static final String TAG_PROFILE = "profile";
    private static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
    private static final String TAG_CUSTOM_NAME = "custom_name";
    private @Nullable ResolvableProfile owner;
    private @Nullable Identifier noteBlockSound;
    private int animationTickCount;
    private boolean isAnimating;
    private @Nullable Component customName;

    public SkullBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.SKULL, worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable(TAG_PROFILE, ResolvableProfile.CODEC, this.owner);
        output.storeNullable(TAG_NOTE_BLOCK_SOUND, Identifier.CODEC, this.noteBlockSound);
        output.storeNullable(TAG_CUSTOM_NAME, ComponentSerialization.CODEC, this.customName);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.owner = input.read(TAG_PROFILE, ResolvableProfile.CODEC).orElse(null);
        this.noteBlockSound = input.read(TAG_NOTE_BLOCK_SOUND, Identifier.CODEC).orElse(null);
        this.customName = SkullBlockEntity.parseCustomNameSafe(input, TAG_CUSTOM_NAME);
    }

    public static void animation(Level level, BlockPos pos, BlockState state, SkullBlockEntity entity) {
        if (state.hasProperty(SkullBlock.POWERED) && state.getValue(SkullBlock.POWERED).booleanValue()) {
            entity.isAnimating = true;
            ++entity.animationTickCount;
        } else {
            entity.isAnimating = false;
        }
    }

    public float getAnimation(float a) {
        if (this.isAnimating) {
            return (float)this.animationTickCount + a;
        }
        return this.animationTickCount;
    }

    public @Nullable ResolvableProfile getOwnerProfile() {
        return this.owner;
    }

    public @Nullable Identifier getNoteBlockSound() {
        return this.noteBlockSound;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.owner = components.get(DataComponents.PROFILE);
        this.noteBlockSound = components.get(DataComponents.NOTE_BLOCK_SOUND);
        this.customName = components.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.PROFILE, this.owner);
        components.set(DataComponents.NOTE_BLOCK_SOUND, this.noteBlockSound);
        components.set(DataComponents.CUSTOM_NAME, this.customName);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard(TAG_PROFILE);
        output.discard(TAG_NOTE_BLOCK_SOUND);
        output.discard(TAG_CUSTOM_NAME);
    }
}

