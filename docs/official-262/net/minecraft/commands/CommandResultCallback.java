/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.commands;

@FunctionalInterface
public interface CommandResultCallback {
    public static final CommandResultCallback EMPTY = new CommandResultCallback(){

        @Override
        public void onResult(boolean success, int result) {
        }

        public String toString() {
            return "<empty>";
        }
    };

    public void onResult(boolean var1, int var2);

    default public void onSuccess(int result) {
        this.onResult(true, result);
    }

    default public void onFailure() {
        this.onResult(false, 0);
    }

    public static CommandResultCallback chain(CommandResultCallback first, CommandResultCallback second) {
        if (first == EMPTY) {
            return second;
        }
        if (second == EMPTY) {
            return first;
        }
        return (success, result) -> {
            first.onResult(success, result);
            second.onResult(success, result);
        };
    }
}

