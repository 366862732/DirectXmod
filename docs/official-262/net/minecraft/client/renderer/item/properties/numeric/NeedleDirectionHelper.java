/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.item.properties.numeric;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public abstract class NeedleDirectionHelper {
    private final boolean wobble;

    protected NeedleDirectionHelper(boolean wobble) {
        this.wobble = wobble;
    }

    public float get(ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable ItemOwner owner, int seed) {
        Level level;
        if (owner == null) {
            return 0.0f;
        }
        if (clientLevel == null && (level = owner.level()) instanceof ClientLevel) {
            ClientLevel level2;
            clientLevel = level2 = (ClientLevel)level;
        }
        if (clientLevel == null) {
            return 0.0f;
        }
        return this.calculate(itemStack, clientLevel, seed, owner);
    }

    protected abstract float calculate(ItemStack var1, ClientLevel var2, int var3, ItemOwner var4);

    protected boolean wobble() {
        return this.wobble;
    }

    protected Wobbler newWobbler(float factor) {
        return this.wobble ? NeedleDirectionHelper.standardWobbler(factor) : NeedleDirectionHelper.nonWobbler();
    }

    public static Wobbler standardWobbler(final float factor) {
        return new Wobbler(){
            private float rotation;
            private float deltaRotation;
            private long lastUpdateTick;

            @Override
            public float rotation() {
                return this.rotation;
            }

            @Override
            public boolean shouldUpdate(long tick) {
                return this.lastUpdateTick != tick;
            }

            @Override
            public void update(long tick, float targetRotation) {
                this.lastUpdateTick = tick;
                float tempDeltaRotation = Mth.positiveModulo(targetRotation - this.rotation + 0.5f, 1.0f) - 0.5f;
                this.deltaRotation += tempDeltaRotation * 0.1f;
                this.deltaRotation *= factor;
                this.rotation = Mth.positiveModulo(this.rotation + this.deltaRotation, 1.0f);
            }
        };
    }

    public static Wobbler nonWobbler() {
        return new Wobbler(){
            private float targetValue;

            @Override
            public float rotation() {
                return this.targetValue;
            }

            @Override
            public boolean shouldUpdate(long tick) {
                return true;
            }

            @Override
            public void update(long tick, float targetRotation) {
                this.targetValue = targetRotation;
            }
        };
    }

    @Environment(value=EnvType.CLIENT)
    public static interface Wobbler {
        public float rotation();

        public boolean shouldUpdate(long var1);

        public void update(long var1, float var3);
    }
}

