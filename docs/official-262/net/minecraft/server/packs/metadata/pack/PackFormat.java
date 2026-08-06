/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.DataResult$Error
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs.metadata.pack;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.InclusiveRange;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public record PackFormat(int major, int minor) implements Comparable<PackFormat>
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<PackFormat> BOTTOM_CODEC = PackFormat.fullCodec(0);
    public static final Codec<PackFormat> TOP_CODEC = PackFormat.fullCodec(Integer.MAX_VALUE);

    private static Codec<PackFormat> fullCodec(int defaultMinor) {
        return ExtraCodecs.compactListCodec(ExtraCodecs.NON_NEGATIVE_INT, ExtraCodecs.NON_NEGATIVE_INT.listOf(1, 256)).xmap(list -> list.size() > 1 ? PackFormat.of((Integer)list.getFirst(), (Integer)list.get(1)) : PackFormat.of((Integer)list.getFirst(), defaultMinor), pf -> pf.minor != defaultMinor ? List.of(Integer.valueOf(pf.major()), Integer.valueOf(pf.minor())) : List.of(Integer.valueOf(pf.major())));
    }

    public static <ResultType, HolderType extends IntermediaryFormatHolder> DataResult<List<ResultType>> validateHolderList(List<HolderType> list, int lastPreMinorVersion, BiFunction<HolderType, InclusiveRange<PackFormat>, ResultType> constructor) {
        int minVersion = list.stream().map(IntermediaryFormatHolder::format).mapToInt(IntermediaryFormat::effectiveMinMajorVersion).min().orElse(Integer.MAX_VALUE);
        ArrayList<ResultType> result = new ArrayList<ResultType>(list.size());
        for (IntermediaryFormatHolder entry : list) {
            IntermediaryFormat format = entry.format();
            if (format.min().isEmpty() && format.max().isEmpty() && format.supported().isEmpty()) {
                LOGGER.warn("Unknown or broken overlay entry {}", (Object)entry);
                continue;
            }
            DataResult<InclusiveRange<PackFormat>> entryResult = format.validate(lastPreMinorVersion, false, minVersion <= lastPreMinorVersion, "Overlay \"" + String.valueOf(entry) + "\"", "formats");
            if (entryResult.isSuccess()) {
                result.add(constructor.apply(entry, (InclusiveRange)entryResult.getOrThrow()));
                continue;
            }
            return DataResult.error(() -> ((DataResult.Error)((DataResult.Error)entryResult.error().get())).message());
        }
        return DataResult.success(List.copyOf(result));
    }

    @VisibleForTesting
    public static int lastPreMinorVersion(PackType type) {
        return switch (type) {
            default -> throw new MatchException(null, null);
            case PackType.CLIENT_RESOURCES -> 64;
            case PackType.SERVER_DATA -> 81;
        };
    }

    public static MapCodec<InclusiveRange<PackFormat>> packCodec(PackType type) {
        int lastPreMinorVersion = PackFormat.lastPreMinorVersion(type);
        return IntermediaryFormat.PACK_CODEC.flatXmap(intermediaryFormat -> intermediaryFormat.validate(lastPreMinorVersion, true, false, "Pack", "supported_formats"), range -> DataResult.success((Object)IntermediaryFormat.fromRange(range, lastPreMinorVersion)));
    }

    public static PackFormat of(int major, int minor) {
        return new PackFormat(major, minor);
    }

    public static PackFormat of(int major) {
        return new PackFormat(major, 0);
    }

    public InclusiveRange<PackFormat> minorRange() {
        return new InclusiveRange<PackFormat>(this, PackFormat.of(this.major, Integer.MAX_VALUE));
    }

    @Override
    public int compareTo(PackFormat other) {
        int majorDiff = Integer.compare(this.major(), other.major());
        if (majorDiff != 0) {
            return majorDiff;
        }
        return Integer.compare(this.minor(), other.minor());
    }

    @Override
    public String toString() {
        if (this.minor == Integer.MAX_VALUE) {
            return String.format(Locale.ROOT, "%d.*", this.major());
        }
        return String.format(Locale.ROOT, "%d.%d", this.major(), this.minor());
    }

    public static interface IntermediaryFormatHolder {
        public IntermediaryFormat format();
    }

    public record IntermediaryFormat(Optional<PackFormat> min, Optional<PackFormat> max, Optional<Integer> format, Optional<InclusiveRange<Integer>> supported) {
        private static final MapCodec<IntermediaryFormat> PACK_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(IntermediaryFormat::min), (App)TOP_CODEC.optionalFieldOf("max_format").forGetter(IntermediaryFormat::max), (App)Codec.INT.optionalFieldOf("pack_format").forGetter(IntermediaryFormat::format), (App)InclusiveRange.codec(Codec.INT).optionalFieldOf("supported_formats").forGetter(IntermediaryFormat::supported)).apply((Applicative)i, IntermediaryFormat::new));
        public static final MapCodec<IntermediaryFormat> OVERLAY_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)BOTTOM_CODEC.optionalFieldOf("min_format").forGetter(IntermediaryFormat::min), (App)TOP_CODEC.optionalFieldOf("max_format").forGetter(IntermediaryFormat::max), (App)InclusiveRange.codec(Codec.INT).optionalFieldOf("formats").forGetter(IntermediaryFormat::supported)).apply((Applicative)i, (min, max, formats) -> new IntermediaryFormat((Optional<PackFormat>)min, (Optional<PackFormat>)max, min.map(PackFormat::major), (Optional<InclusiveRange<Integer>>)formats)));

        public static IntermediaryFormat fromRange(InclusiveRange<PackFormat> range, int lastPreMinorVersion) {
            InclusiveRange<Integer> majorRange = range.map(PackFormat::major);
            return new IntermediaryFormat(Optional.of(range.minInclusive()), Optional.of(range.maxInclusive()), majorRange.isValueInRange(lastPreMinorVersion) ? Optional.of(majorRange.minInclusive()) : Optional.empty(), majorRange.isValueInRange(lastPreMinorVersion) ? Optional.of(new InclusiveRange<Integer>(majorRange.minInclusive(), majorRange.maxInclusive())) : Optional.empty());
        }

        public int effectiveMinMajorVersion() {
            if (this.min.isPresent()) {
                if (this.supported.isPresent()) {
                    return Math.min(this.min.get().major(), this.supported.get().minInclusive());
                }
                return this.min.get().major();
            }
            if (this.supported.isPresent()) {
                return this.supported.get().minInclusive();
            }
            return Integer.MAX_VALUE;
        }

        public DataResult<InclusiveRange<PackFormat>> validate(int lastPreMinorVersion, boolean hasPackFormatField, boolean requireOldField, String context, String oldFieldName) {
            if (this.min.isPresent() != this.max.isPresent()) {
                return DataResult.error(() -> context + " missing field, must declare both min_format and max_format");
            }
            if (requireOldField && this.supported.isEmpty()) {
                return DataResult.error(() -> context + " missing required field " + oldFieldName + ", must be present in all overlays for any overlays to work across game versions");
            }
            if (this.min.isPresent()) {
                return this.validateNewFormat(lastPreMinorVersion, hasPackFormatField, requireOldField, context, oldFieldName);
            }
            if (this.supported.isPresent()) {
                return this.validateOldFormat(lastPreMinorVersion, hasPackFormatField, context, oldFieldName);
            }
            if (hasPackFormatField && this.format.isPresent()) {
                int mainFormat = this.format.get();
                if (mainFormat > lastPreMinorVersion) {
                    return DataResult.error(() -> context + " declares support for version newer than " + lastPreMinorVersion + ", but is missing mandatory fields min_format and max_format");
                }
                return DataResult.success(new InclusiveRange<PackFormat>(PackFormat.of(mainFormat)));
            }
            return DataResult.error(() -> context + " could not be parsed, missing format version information");
        }

        private DataResult<InclusiveRange<PackFormat>> validateNewFormat(int lastPreMinorVersion, boolean hasPackFormatField, boolean requireOldField, String context, String oldFieldName) {
            int majorMin = this.min.get().major();
            int majorMax = this.max.get().major();
            if (this.min.get().compareTo(this.max.get()) > 0) {
                return DataResult.error(() -> context + " min_format (" + String.valueOf(this.min.get()) + ") is greater than max_format (" + String.valueOf(this.max.get()) + ")");
            }
            if (majorMin > lastPreMinorVersion && !requireOldField) {
                String packFormatError;
                if (this.supported.isPresent()) {
                    return DataResult.error(() -> context + " key " + oldFieldName + " is deprecated starting from pack format " + (lastPreMinorVersion + 1) + ". Remove " + oldFieldName + " from your pack.mcmeta.");
                }
                if (hasPackFormatField && this.format.isPresent() && (packFormatError = this.validatePackFormatForRange(majorMin, majorMax)) != null) {
                    return DataResult.error(() -> packFormatError);
                }
            } else {
                if (this.supported.isPresent()) {
                    InclusiveRange<Integer> oldSupportedVersions = this.supported.get();
                    if (oldSupportedVersions.minInclusive() != majorMin) {
                        return DataResult.error(() -> context + " version declaration mismatch between " + oldFieldName + " (from " + String.valueOf(oldSupportedVersions.minInclusive()) + ") and min_format (" + String.valueOf(this.min.get()) + ")");
                    }
                    if (oldSupportedVersions.maxInclusive() != majorMax && oldSupportedVersions.maxInclusive() != lastPreMinorVersion) {
                        return DataResult.error(() -> context + " version declaration mismatch between " + oldFieldName + " (up to " + String.valueOf(oldSupportedVersions.maxInclusive()) + ") and max_format (" + String.valueOf(this.max.get()) + ")");
                    }
                } else {
                    return DataResult.error(() -> context + " declares support for format " + majorMin + ", but game versions supporting formats 17 to " + lastPreMinorVersion + " require a " + oldFieldName + " field. Add \"" + oldFieldName + "\": [" + majorMin + ", " + lastPreMinorVersion + "] or require a version greater or equal to " + (lastPreMinorVersion + 1) + ".0.");
                }
                if (hasPackFormatField) {
                    if (this.format.isPresent()) {
                        String packFormatError = this.validatePackFormatForRange(majorMin, majorMax);
                        if (packFormatError != null) {
                            return DataResult.error(() -> packFormatError);
                        }
                    } else {
                        return DataResult.error(() -> context + " declares support for formats up to " + lastPreMinorVersion + ", but game versions supporting formats 17 to " + lastPreMinorVersion + " require a pack_format field. Add \"pack_format\": " + majorMin + " or require a version greater or equal to " + (lastPreMinorVersion + 1) + ".0.");
                    }
                }
            }
            return DataResult.success(new InclusiveRange<PackFormat>(this.min.get(), this.max.get()));
        }

        private DataResult<InclusiveRange<PackFormat>> validateOldFormat(int lastPreMinorVersion, boolean hasPackFormatField, String context, String oldFieldName) {
            InclusiveRange<Integer> oldSupportedVersions = this.supported.get();
            int min = oldSupportedVersions.minInclusive();
            int max = oldSupportedVersions.maxInclusive();
            if (max > lastPreMinorVersion) {
                return DataResult.error(() -> context + " declares support for version newer than " + lastPreMinorVersion + ", but is missing mandatory fields min_format and max_format");
            }
            if (hasPackFormatField) {
                if (this.format.isPresent()) {
                    String packFormatError = this.validatePackFormatForRange(min, max);
                    if (packFormatError != null) {
                        return DataResult.error(() -> packFormatError);
                    }
                } else {
                    return DataResult.error(() -> context + " declares support for formats up to " + lastPreMinorVersion + ", but game versions supporting formats 17 to " + lastPreMinorVersion + " require a pack_format field. Add \"pack_format\": " + min + " or require a version greater or equal to " + (lastPreMinorVersion + 1) + ".0.");
                }
            }
            return DataResult.success(new InclusiveRange<Integer>(min, max).map(PackFormat::of));
        }

        private @Nullable String validatePackFormatForRange(int min, int max) {
            int mainFormat = this.format.get();
            if (mainFormat < min || mainFormat > max) {
                return "Pack declared support for versions " + min + " to " + max + " but declared main format is " + mainFormat;
            }
            if (mainFormat < 15) {
                return "Multi-version packs cannot support minimum version of less than 15, since this will leave versions in range unable to load pack.";
            }
            return null;
        }
    }
}

