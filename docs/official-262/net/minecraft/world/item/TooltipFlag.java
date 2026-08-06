/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.item;

public interface TooltipFlag {
    public static final Default NORMAL = new Default(false, false);
    public static final Default ADVANCED = new Default(true, false);

    public boolean isAdvanced();

    public boolean isCreative();

    public record Default(boolean advanced, boolean creative) implements TooltipFlag
    {
        @Override
        public boolean isAdvanced() {
            return this.advanced;
        }

        @Override
        public boolean isCreative() {
            return this.creative;
        }

        public Default asCreative() {
            return new Default(this.advanced, true);
        }
    }
}

