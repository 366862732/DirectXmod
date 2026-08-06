/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Joiner
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs;

import com.google.common.base.Joiner;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.FileUtil;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class PathPackResources
extends AbstractPackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Joiner PATH_JOINER = Joiner.on((String)"/");
    private final Path root;

    public PathPackResources(PackLocationInfo location, Path root) {
        super(location);
        this.root = root;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String ... path) {
        FileUtil.validatePath(path);
        Path pathInRoot = FileUtil.resolvePath(this.root, List.of(path));
        if (Files.exists(pathInRoot, new LinkOption[0])) {
            return IoSupplier.create(pathInRoot);
        }
        return null;
    }

    public static boolean validatePath(Path path) {
        if (!SharedConstants.DEBUG_VALIDATE_RESOURCE_PATH_CASE) {
            return true;
        }
        if (path.getFileSystem() != FileSystems.getDefault()) {
            return true;
        }
        try {
            return path.toRealPath(new LinkOption[0]).endsWith(path);
        }
        catch (IOException e) {
            LOGGER.warn("Failed to resolve real path for {}", (Object)path, (Object)e);
            return false;
        }
    }

    private Path topPackDir(PackType type) {
        return this.root.resolve(type.getDirectory());
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, Identifier location) {
        Path topDir = this.topPackDir(type);
        return PathPackResources.getResource(topDir, location);
    }

    public static @Nullable IoSupplier<InputStream> getResource(Path topDir, Identifier location) {
        Path namespaceDir = topDir.resolve(location.getNamespace());
        return (IoSupplier)FileUtil.decomposePath(location.getPath()).mapOrElse(decomposedPath -> {
            Path resolvedPath = FileUtil.resolvePath(namespaceDir, decomposedPath);
            return PathPackResources.returnFileIfExists(resolvedPath);
        }, error -> {
            LOGGER.error("Invalid path {}: {}", (Object)location, (Object)error.message());
            return null;
        });
    }

    private static @Nullable IoSupplier<InputStream> returnFileIfExists(Path resolvedPath) {
        if (Files.exists(resolvedPath, new LinkOption[0]) && PathPackResources.validatePath(resolvedPath)) {
            return IoSupplier.create(resolvedPath);
        }
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String directory, PackResources.ResourceOutput output) {
        Path topDir = this.topPackDir(type);
        PathPackResources.listResources(topDir, namespace, directory, output);
    }

    public static void listResources(Path topPath, String namespace, String directory, PackResources.ResourceOutput output) {
        FileUtil.decomposePath(directory).ifSuccess(decomposedPath -> {
            Path namespaceDir = topPath.resolve(namespace);
            PathPackResources.listPath(namespace, namespaceDir, decomposedPath, output);
        }).ifError(error -> LOGGER.error("Invalid path {}: {}", (Object)directory, (Object)error.message()));
    }

    public static void listPath(String namespace, Path topDir, List<String> decomposedPrefixPath, PackResources.ResourceOutput output) {
        Path targetPath = FileUtil.resolvePath(topDir, decomposedPrefixPath);
        try (Stream<Path> files2 = Files.find(targetPath, Integer.MAX_VALUE, PathPackResources::isRegularFile, new FileVisitOption[0]);){
            files2.forEach(file -> {
                String resourcePath = PATH_JOINER.join((Iterable)topDir.relativize((Path)file));
                Identifier identifier = Identifier.tryBuild(namespace, resourcePath);
                if (identifier == null) {
                    Util.logAndPauseIfInIde(String.format(Locale.ROOT, "Invalid path in pack: %s:%s, ignoring", namespace, resourcePath));
                } else {
                    output.accept(identifier, IoSupplier.create(file));
                }
            });
        }
        catch (NoSuchFileException | NotDirectoryException files2) {
        }
        catch (IOException e) {
            LOGGER.error("Failed to list path {}", (Object)targetPath, (Object)e);
        }
    }

    private static boolean isRegularFile(Path file, BasicFileAttributes attributes) {
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            return attributes.isRegularFile() && !StringUtils.equalsIgnoreCase((CharSequence)file.getFileName().toString(), (CharSequence)".ds_store");
        }
        return attributes.isRegularFile();
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Path assetRoot = this.topPackDir(type);
        return PathPackResources.getNamespaces(assetRoot);
    }

    public static Set<String> getNamespaces(Path rootDir) {
        HashSet<String> namespaces = new HashSet<String>();
        try (DirectoryStream<Path> directDirs2 = Files.newDirectoryStream(rootDir);){
            for (Path directDir : directDirs2) {
                if (!Files.isDirectory(directDir, new LinkOption[0])) {
                    LOGGER.warn("Non-directory entry {} found in namespace directory, rejecting", (Object)directDir);
                    continue;
                }
                String namespace = directDir.getFileName().toString();
                if (Identifier.isValidNamespace(namespace)) {
                    namespaces.add(namespace);
                    continue;
                }
                LOGGER.warn("Non {} character in namespace {} in pack directory {}, ignoring", new Object[]{"[a-z0-9_.-]", namespace, rootDir});
            }
        }
        catch (NoSuchFileException | NotDirectoryException directDirs2) {
        }
        catch (IOException e) {
            LOGGER.error("Failed to list path {}", (Object)rootDir, (Object)e);
        }
        return namespaces;
    }

    @Override
    public void close() {
    }

    public static class PathResourcesSupplier
    implements Pack.ResourcesSupplier {
        private final Path content;

        public PathResourcesSupplier(Path content) {
            this.content = content;
        }

        @Override
        public PackResources openPrimary(PackLocationInfo location) {
            return new PathPackResources(location, this.content);
        }

        @Override
        public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
            PackResources primary = this.openPrimary(location);
            List<String> overlays = metadata.overlays();
            if (overlays.isEmpty()) {
                return primary;
            }
            ArrayList<PackResources> overlayResources = new ArrayList<PackResources>(overlays.size());
            for (String overlay : overlays) {
                Path overlayRoot = this.content.resolve(overlay);
                overlayResources.add(new PathPackResources(location, overlayRoot));
            }
            return new CompositePackResources(primary, overlayResources);
        }
    }
}

