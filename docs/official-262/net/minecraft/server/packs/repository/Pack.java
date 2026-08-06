/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonParseException
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs.repository;

import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FeatureFlagsMetadataSection;
import net.minecraft.server.packs.OverlayMetadataSection;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackFormat;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Pack {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackLocationInfo location;
    private final ResourcesSupplier resources;
    private final Metadata metadata;
    private final PackSelectionConfig selectionConfig;

    public static @Nullable Pack readMetaAndCreate(PackLocationInfo location, ResourcesSupplier resources, PackType packType, PackSelectionConfig selectionConfig) {
        PackFormat currentPackVersion = SharedConstants.getCurrentVersion().packVersion(packType);
        Metadata meta = Pack.readPackMetadata(location, resources, currentPackVersion, packType);
        return meta != null ? new Pack(location, resources, meta, selectionConfig) : null;
    }

    public Pack(PackLocationInfo location, ResourcesSupplier resources, Metadata metadata, PackSelectionConfig selectionConfig) {
        this.location = location;
        this.resources = resources;
        this.metadata = metadata;
        this.selectionConfig = selectionConfig;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static @Nullable Metadata readPackMetadata(PackLocationInfo location, ResourcesSupplier resources, PackFormat currentPackVersion, PackType type) {
        try (PackResources pack = resources.openPrimary(location);){
            PackMetadataSection meta;
            try {
                meta = pack.getMetadataSection(PackMetadataSection.forPackType(type));
            }
            catch (JsonParseException e) {
                LOGGER.warn("Error reading pack metadata, attempting fallback type", (Throwable)e);
                meta = pack.getMetadataSection(PackMetadataSection.FALLBACK_TYPE);
            }
            if (meta == null) {
                LOGGER.warn("Missing metadata in pack {}", (Object)location.id());
                Metadata e = null;
                return e;
            }
            FeatureFlagsMetadataSection featureFlagMeta = pack.getMetadataSection(FeatureFlagsMetadataSection.TYPE);
            FeatureFlagSet requiredFlags = featureFlagMeta != null ? featureFlagMeta.flags() : FeatureFlagSet.of();
            PackCompatibility packCompatibility = PackCompatibility.forVersion(meta.supportedFormats(), currentPackVersion);
            OverlayMetadataSection overlays = pack.getMetadataSection(OverlayMetadataSection.forPackType(type));
            List<String> overlaySet = overlays != null ? overlays.overlaysForVersion(currentPackVersion) : List.of();
            Metadata metadata = new Metadata(meta.description(), packCompatibility, requiredFlags, overlaySet);
            return metadata;
        }
        catch (Exception e) {
            LOGGER.warn("Failed to read pack {} metadata", (Object)location.id(), (Object)e);
            return null;
        }
    }

    public PackLocationInfo location() {
        return this.location;
    }

    public Component getTitle() {
        return this.location.title();
    }

    public Component getDescription() {
        return this.metadata.description();
    }

    public Component getChatLink(boolean enabled) {
        return this.location.createChatLink(enabled, this.metadata.description);
    }

    public PackCompatibility getCompatibility() {
        return this.metadata.compatibility();
    }

    public FeatureFlagSet getRequestedFeatures() {
        return this.metadata.requestedFeatures();
    }

    public PackResources open() {
        return this.resources.openFull(this.location, this.metadata);
    }

    public String getId() {
        return this.location.id();
    }

    public PackSelectionConfig selectionConfig() {
        return this.selectionConfig;
    }

    public boolean isRequired() {
        return this.selectionConfig.required();
    }

    public boolean isFixedPosition() {
        return this.selectionConfig.fixedPosition();
    }

    public Position getDefaultPosition() {
        return this.selectionConfig.defaultPosition();
    }

    public PackSource getPackSource() {
        return this.location.source();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pack)) {
            return false;
        }
        Pack that = (Pack)o;
        return this.location.equals(that.location);
    }

    public int hashCode() {
        return this.location.hashCode();
    }

    public static interface ResourcesSupplier {
        public PackResources openPrimary(PackLocationInfo var1);

        public PackResources openFull(PackLocationInfo var1, Metadata var2);
    }

    public record Metadata(Component description, PackCompatibility compatibility, FeatureFlagSet requestedFeatures, List<String> overlays) {
    }

    public static enum Position {
        TOP,
        BOTTOM;


        public <T> int insert(List<T> list, T value, Function<T, PackSelectionConfig> converter, boolean reverse) {
            PackSelectionConfig pack;
            int index;
            Position self;
            Position position = self = reverse ? this.opposite() : this;
            if (self == BOTTOM) {
                PackSelectionConfig pack2;
                int index2;
                for (index2 = 0; index2 < list.size() && (pack2 = converter.apply(list.get(index2))).fixedPosition() && pack2.defaultPosition() == this; ++index2) {
                }
                list.add(index2, value);
                return index2;
            }
            for (index = list.size() - 1; index >= 0 && (pack = converter.apply(list.get(index))).fixedPosition() && pack.defaultPosition() == this; --index) {
            }
            list.add(index + 1, value);
            return index + 1;
        }

        public Position opposite() {
            return this == TOP ? BOTTOM : TOP;
        }
    }
}

