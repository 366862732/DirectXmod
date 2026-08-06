/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.metadata.gui;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.server.packs.metadata.MetadataSectionType;

@Environment(value=EnvType.CLIENT)
public record GuiMetadataSection(GuiSpriteScaling scaling) {
    public static final GuiMetadataSection DEFAULT = new GuiMetadataSection(GuiSpriteScaling.DEFAULT);
    public static final Codec<GuiMetadataSection> CODEC = RecordCodecBuilder.create(i -> i.group((App)GuiSpriteScaling.CODEC.optionalFieldOf("scaling", (Object)GuiSpriteScaling.DEFAULT).forGetter(GuiMetadataSection::scaling)).apply((Applicative)i, GuiMetadataSection::new));
    public static final MetadataSectionType<GuiMetadataSection> TYPE = new MetadataSectionType<GuiMetadataSection>("gui", CODEC);
}

