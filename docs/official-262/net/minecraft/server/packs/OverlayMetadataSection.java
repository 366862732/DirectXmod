/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.util.InclusiveRange;

public record OverlayMetadataSection(List<OverlayEntry> overlays) {
    private static final Pattern DIR_VALIDATOR = Pattern.compile("[-_a-zA-Z0-9.]+");
    public static final MetadataSectionType<OverlayMetadataSection> CLIENT_TYPE = new MetadataSectionType<OverlayMetadataSection>("overlays", OverlayMetadataSection.codecForPackType(PackType.CLIENT_RESOURCES));
    public static final MetadataSectionType<OverlayMetadataSection> SERVER_TYPE = new MetadataSectionType<OverlayMetadataSection>("overlays", OverlayMetadataSection.codecForPackType(PackType.SERVER_DATA));

    private static DataResult<String> validateOverlayDir(String path) {
        if (!DIR_VALIDATOR.matcher(path).matches()) {
            return DataResult.error(() -> path + " is not accepted directory name");
        }
        return DataResult.success((Object)path);
    }

    @VisibleForTesting
    public static Codec<OverlayMetadataSection> codecForPackType(PackType packType) {
        return RecordCodecBuilder.create(i -> i.group((App)OverlayEntry.listCodecForPackType(packType).fieldOf("entries").forGetter(OverlayMetadataSection::overlays)).apply((Applicative)i, OverlayMetadataSection::new));
    }

    public static MetadataSectionType<OverlayMetadataSection> forPackType(PackType packType) {
        return switch (packType) {
            default -> throw new MatchException(null, null);
            case PackType.CLIENT_RESOURCES -> CLIENT_TYPE;
            case PackType.SERVER_DATA -> SERVER_TYPE;
        };
    }

    public List<String> overlaysForVersion(PackFormat version) {
        return this.overlays.stream().filter(entry -> entry.isApplicable(version)).map(OverlayEntry::overlay).toList();
    }

    public record OverlayEntry(InclusiveRange<PackFormat> format, String overlay) {
        private static Codec<List<OverlayEntry>> listCodecForPackType(PackType packType) {
            int lastPreMinorVersion = PackFormat.lastPreMinorVersion(packType);
            return IntermediateEntry.CODEC.listOf().flatXmap(list -> PackFormat.validateHolderList(list, lastPreMinorVersion, (entry, formats) -> new OverlayEntry((InclusiveRange<PackFormat>)formats, entry.overlay())), list -> DataResult.success(list.stream().map(entry -> new IntermediateEntry(PackFormat.IntermediaryFormat.fromRange(entry.format(), lastPreMinorVersion), entry.overlay())).toList()));
        }

        public boolean isApplicable(PackFormat formatToTest) {
            return this.format.isValueInRange(formatToTest);
        }

        private record IntermediateEntry(PackFormat.IntermediaryFormat format, String overlay) implements PackFormat.IntermediaryFormatHolder
        {
            private static final Codec<IntermediateEntry> CODEC = RecordCodecBuilder.create(i -> i.group((App)PackFormat.IntermediaryFormat.OVERLAY_CODEC.forGetter(IntermediateEntry::format), (App)Codec.STRING.validate(OverlayMetadataSection::validateOverlayDir).fieldOf("directory").forGetter(IntermediateEntry::overlay)).apply((Applicative)i, IntermediateEntry::new));

            @Override
            public String toString() {
                return this.overlay;
            }
        }
    }
}

