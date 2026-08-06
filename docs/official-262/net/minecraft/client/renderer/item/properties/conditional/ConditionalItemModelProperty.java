/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.MapCodec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.renderer.item.properties.conditional;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;

@Environment(value=EnvType.CLIENT)
public interface ConditionalItemModelProperty
extends ItemModelPropertyTest {
    public MapCodec<? extends ConditionalItemModelProperty> type();
}

