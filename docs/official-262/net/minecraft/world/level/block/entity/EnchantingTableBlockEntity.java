/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class EnchantingTableBlockEntity
extends BlockEntity
implements Nameable {
    private static final Component DEFAULT_NAME = Component.translatable("container.enchant");
    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;
    private static final RandomSource RANDOM = RandomSource.create();
    private @Nullable Component name;

    public EnchantingTableBlockEntity(BlockPos worldPosition, BlockState blockState) {
        super(BlockEntityTypes.ENCHANTING_TABLE, worldPosition, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.name = EnchantingTableBlockEntity.parseCustomNameSafe(input, "CustomName");
    }

    public static void bookAnimationTick(Level level, BlockPos worldPosition, BlockState state, EnchantingTableBlockEntity entity) {
        float rotDir;
        entity.oOpen = entity.open;
        entity.oRot = entity.rot;
        Player player = level.getNearestPlayer((double)worldPosition.getX() + 0.5, (double)worldPosition.getY() + 0.5, (double)worldPosition.getZ() + 0.5, 3.0, false);
        if (player != null) {
            double xd = player.getX() - ((double)worldPosition.getX() + 0.5);
            double zd = player.getZ() - ((double)worldPosition.getZ() + 0.5);
            entity.tRot = (float)Mth.atan2(zd, xd);
            entity.open += 0.1f;
            if (entity.open < 0.5f || RANDOM.nextInt(40) == 0) {
                float old = entity.flipT;
                do {
                    entity.flipT += (float)(RANDOM.nextInt(4) - RANDOM.nextInt(4));
                } while (old == entity.flipT);
            }
        } else {
            entity.tRot += 0.02f;
            entity.open -= 0.1f;
        }
        while (entity.rot >= (float)Math.PI) {
            entity.rot -= (float)Math.PI * 2;
        }
        while (entity.rot < (float)(-Math.PI)) {
            entity.rot += (float)Math.PI * 2;
        }
        while (entity.tRot >= (float)Math.PI) {
            entity.tRot -= (float)Math.PI * 2;
        }
        while (entity.tRot < (float)(-Math.PI)) {
            entity.tRot += (float)Math.PI * 2;
        }
        for (rotDir = entity.tRot - entity.rot; rotDir >= (float)Math.PI; rotDir -= (float)Math.PI * 2) {
        }
        while (rotDir < (float)(-Math.PI)) {
            rotDir += (float)Math.PI * 2;
        }
        entity.rot += rotDir * 0.4f;
        entity.open = Mth.clamp(entity.open, 0.0f, 1.0f);
        ++entity.time;
        entity.oFlip = entity.flip;
        float diff = (entity.flipT - entity.flip) * 0.4f;
        float max = 0.2f;
        diff = Mth.clamp(diff, -0.2f, 0.2f);
        entity.flipA += (diff - entity.flipA) * 0.9f;
        entity.flip += entity.flipA;
    }

    @Override
    public Component getName() {
        if (this.name != null) {
            return this.name;
        }
        return DEFAULT_NAME;
    }

    public void setCustomName(@Nullable Component name) {
        this.name = name;
    }

    @Override
    public @Nullable Component getCustomName() {
        return this.name;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        this.name = components.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.name);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("CustomName");
    }
}

