/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.server.packs.repository;

import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public interface PackSource {
    public static final UnaryOperator<Component> NO_DECORATION = UnaryOperator.identity();
    public static final PackSource DEFAULT = PackSource.create(NO_DECORATION, true);
    public static final PackSource BUILT_IN = PackSource.create(PackSource.decorateWithSource("pack.source.builtin"), true);
    public static final PackSource FEATURE = PackSource.create(PackSource.decorateWithSource("pack.source.feature"), false);
    public static final PackSource WORLD = PackSource.create(PackSource.decorateWithSource("pack.source.world"), true);
    public static final PackSource SERVER = PackSource.create(PackSource.decorateWithSource("pack.source.server"), true);

    public Component decorate(Component var1);

    public boolean shouldAddAutomatically();

    public static PackSource create(final UnaryOperator<Component> decorator, final boolean addAutomatically) {
        return new PackSource(){

            @Override
            public Component decorate(Component packDescription) {
                return (Component)decorator.apply(packDescription);
            }

            @Override
            public boolean shouldAddAutomatically() {
                return addAutomatically;
            }
        };
    }

    private static UnaryOperator<Component> decorateWithSource(String descriptionId) {
        MutableComponent description = Component.translatable(descriptionId);
        return packDescription -> Component.translatable("pack.nameAndSource", packDescription, description).withStyle(ChatFormatting.GRAY);
    }
}

