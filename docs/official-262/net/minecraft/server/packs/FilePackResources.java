/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.common.collect.Sets
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.io.IOUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import org.apache.commons.io.IOUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FilePackResources
extends AbstractPackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final SharedZipFileAccess zipFileAccess;
    private final String prefix;

    private FilePackResources(PackLocationInfo location, SharedZipFileAccess zipFileAccess, String prefix) {
        super(location);
        this.zipFileAccess = zipFileAccess;
        this.prefix = prefix;
    }

    private static String getPathFromLocation(PackType type, Identifier location) {
        return String.format(Locale.ROOT, "%s/%s/%s", type.getDirectory(), location.getNamespace(), location.getPath());
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String ... path) {
        return this.getResource(String.join((CharSequence)"/", path));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
        return this.getResource(FilePackResources.getPathFromLocation(type, location));
    }

    private String addPrefix(String path) {
        if (this.prefix.isEmpty()) {
            return path;
        }
        return this.prefix + "/" + path;
    }

    private @Nullable IoSupplier<InputStream> getResource(String path) {
        ZipFile zipFile = this.zipFileAccess.getOrCreateZipFile();
        if (zipFile == null) {
            return null;
        }
        ZipEntry entry = zipFile.getEntry(this.addPrefix(path));
        if (entry == null) {
            return null;
        }
        return IoSupplier.create(zipFile, entry);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        ZipFile zipFile = this.zipFileAccess.getOrCreateZipFile();
        if (zipFile == null) {
            return Set.of();
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        HashSet namespaces = Sets.newHashSet();
        String typePrefix = this.addPrefix(type.getDirectory() + "/");
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();
            String name = zipEntry.getName();
            String namespace = FilePackResources.extractNamespace(typePrefix, name);
            if (namespace.isEmpty()) continue;
            if (Identifier.isValidNamespace(namespace)) {
                namespaces.add(namespace);
                continue;
            }
            LOGGER.warn("Non {} character in namespace {} in pack {}, ignoring", new Object[]{"[a-z0-9_.-]", namespace, this.zipFileAccess.file});
        }
        return namespaces;
    }

    @VisibleForTesting
    public static String extractNamespace(String prefix, String name) {
        if (!name.startsWith(prefix)) {
            return "";
        }
        int prefixLength = prefix.length();
        int firstPart = name.indexOf(47, prefixLength);
        if (firstPart == -1) {
            return name.substring(prefixLength);
        }
        return name.substring(prefixLength, firstPart);
    }

    @Override
    public void close() {
        this.zipFileAccess.close();
    }

    @Override
    public void listResources(PackType type, String namespace, String directory, PackResources.ResourceOutput output) {
        ZipFile zipFile = this.zipFileAccess.getOrCreateZipFile();
        if (zipFile == null) {
            return;
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        String root = this.addPrefix(type.getDirectory() + "/" + namespace + "/");
        String prefix = root + directory + "/";
        while (entries.hasMoreElements()) {
            String name;
            ZipEntry zipEntry = entries.nextElement();
            if (zipEntry.isDirectory() || !(name = zipEntry.getName()).startsWith(prefix)) continue;
            String path = name.substring(root.length());
            Identifier id = Identifier.tryBuild(namespace, path);
            if (id != null) {
                output.accept(id, IoSupplier.create(zipFile, zipEntry));
                continue;
            }
            LOGGER.warn("Invalid path in datapack: {}:{}, ignoring", (Object)namespace, (Object)path);
        }
    }

    private static class SharedZipFileAccess
    implements AutoCloseable {
        private final File file;
        private @Nullable ZipFile zipFile;
        private boolean failedToLoad;

        private SharedZipFileAccess(File file) {
            this.file = file;
        }

        private @Nullable ZipFile getOrCreateZipFile() {
            if (this.failedToLoad) {
                return null;
            }
            if (this.zipFile == null) {
                try {
                    this.zipFile = new ZipFile(this.file);
                }
                catch (IOException e) {
                    LOGGER.error("Failed to open pack {}", (Object)this.file, (Object)e);
                    this.failedToLoad = true;
                    return null;
                }
            }
            return this.zipFile;
        }

        @Override
        public void close() {
            if (this.zipFile != null) {
                IOUtils.closeQuietly((Closeable)this.zipFile);
                this.zipFile = null;
            }
        }

        protected void finalize() throws Throwable {
            this.close();
            super.finalize();
        }
    }

    public static class FileResourcesSupplier
    implements Pack.ResourcesSupplier {
        private final File content;

        public FileResourcesSupplier(Path content) {
            this(content.toFile());
        }

        public FileResourcesSupplier(File content) {
            this.content = content;
        }

        @Override
        public PackResources openPrimary(PackLocationInfo location) {
            SharedZipFileAccess fileAccess = new SharedZipFileAccess(this.content);
            return new FilePackResources(location, fileAccess, "");
        }

        @Override
        public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            SharedZipFileAccess fileAccess = new SharedZipFileAccess(this.content);
            FilePackResources primary = new FilePackResources(location, fileAccess, "");
            List<String> overlays = metadata.overlays();
            if (overlays.isEmpty()) {
                return primary;
            }
            ArrayList<PackResources> overlayResources = new ArrayList<PackResources>(overlays.size());
            for (String overlay : overlays) {
                overlayResources.add(new FilePackResources(location, fileAccess, overlay));
            }
            return new CompositePackResources(primary, overlayResources);
        }
    }
}

