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
package net.minecraft.client.resources.metadata.animation;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.StringRepresentable;

@Environment(value=EnvType.CLIENT)
public record VillagerMetadataSection(Hat hat) {
    public static final Codec<VillagerMetadataSection> CODEC = RecordCodecBuilder.create(i -> i.group((App)Hat.CODEC.optionalFieldOf("hat", (Object)Hat.NONE).forGetter(VillagerMetadataSection::hat)).apply((Applicative)i, VillagerMetadataSection::new));
    public static final MetadataSectionType<VillagerMetadataSection> TYPE = new MetadataSectionType<VillagerMetadataSection>("villager", CODEC);

    @Environment(value=EnvType.CLIENT)
    public static enum Hat implements StringRepresentable
    {
        NONE("none"),
        PARTIAL("partial"),
        FULL("full");

        public static final Codec<Hat> CODEC;
        private final String name;

        private Hat(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        static {
            CODEC = StringRepresentable.fromEnum(Hat::values);
        }
    }
}

