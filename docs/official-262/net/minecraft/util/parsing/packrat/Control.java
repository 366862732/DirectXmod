/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.util.parsing.packrat;

public interface Control {
    public static final Control UNBOUND = new Control(){

        @Override
        public void cut() {
        }

        @Override
        public boolean hasCut() {
            return false;
        }
    };

    public void cut();

    public boolean hasCut();
}

