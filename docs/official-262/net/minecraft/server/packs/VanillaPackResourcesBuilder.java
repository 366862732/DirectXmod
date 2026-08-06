/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.ImmutableMap$Builder
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.FileSystemUtil;
import net.minecraft.util.Util;
import org.slf4j.Logger;

public class VanillaPackResourcesBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Consumer<VanillaPackResourcesBuilder> developmentConfig = builder -> {};
    private static final Map<PackType, Path> ROOT_DIR_BY_TYPE = (Map)Util.make(() -> {
        Class<VanillaPackResources> clazz = VanillaPackResources.class;
        synchronized (VanillaPackResources.class) {
            ImmutableMap.Builder result = ImmutableMap.builder();
            for (PackType type : PackType.values()) {
                String probeName = "/" + type.getDirectory() + "/.mcassetsroot";
                URL probeUrl = VanillaPackResources.class.getResource(probeName);
                if (probeUrl == null) {
                    LOGGER.error("File {} does not exist in classpath", (Object)probeName);
                    continue;
                }
                try {
                    URI probeUri = probeUrl.toURI();
                    String scheme = probeUri.getScheme();
                    if (!"jar".equals(scheme) && !"file".equals(scheme)) {
                        LOGGER.warn("Assets URL '{}' uses unexpected schema", (Object)probeUri);
                    }
                    Path probePath = FileSystemUtil.safeGetPath(probeUri);
                    result.put((Object)type, (Object)probePath.getParent());
                }
                catch (Exception e) {
                    LOGGER.error("Couldn't resolve path to vanilla assets", (Throwable)e);
                }
            }
            // ** MonitorExit[var0] (shouldn't be in output)
            return result.build();
        }
    });
    private final Set<Path> rootPaths = new LinkedHashSet<Path>();
    private final Map<PackType, Set<Path>> pathsForType = new EnumMap<PackType, Set<Path>>(PackType.class);
    private ResourceMetadata metadata = ResourceMetadata.EMPTY;
    private final Set<String> namespaces = new HashSet<String>();

    private boolean validateDirPath(Path path) {
        if (!Files.exists(path, new LinkOption[0])) {
            return false;
        }
        if (!Files.isDirectory(path, new LinkOption[0])) {
            throw new IllegalArgumentException("Path " + String.valueOf(path.toAbsolutePath()) + " is not directory");
        }
        return true;
    }

    private void pushRootPath(Path path) {
        if (this.validateDirPath(path)) {
            this.rootPaths.add(path);
        }
    }

    private void pushPathForType(PackType packType, Path path) {
        if (this.validateDirPath(path)) {
            this.pathsForType.computeIfAbsent(packType, k -> new LinkedHashSet()).add(path);
        }
    }

    public VanillaPackResourcesBuilder pushJarResources() {
        ROOT_DIR_BY_TYPE.forEach((packType, path) -> {
            this.pushRootPath(path.getParent());
            this.pushPathForType((PackType)((Object)packType), (Path)path);
        });
        return this;
    }

    public VanillaPackResourcesBuilder pushClasspathResources(PackType packType, Class<?> source) {
        Enumeration<URL> resources = null;
        try {
            resources = source.getClassLoader().getResources(packType.getDirectory() + "/");
        }
        catch (IOException iOException) {
            // empty catch block
        }
        while (resources != null && resources.hasMoreElements()) {
            URL url = resources.nextElement();
            try {
                URI uri = url.toURI();
                if (!"file".equals(uri.getScheme())) continue;
                Path assetsPath = Paths.get(uri);
                this.pushRootPath(assetsPath.getParent());
                this.pushPathForType(packType, assetsPath);
            }
            catch (Exception e) {
                LOGGER.error("Failed to extract path from {}", (Object)url, (Object)e);
            }
        }
        return this;
    }

    public VanillaPackResourcesBuilder applyDevelopmentConfig() {
        developmentConfig.accept(this);
        return this;
    }

    public VanillaPackResourcesBuilder pushUniversalPath(Path path) {
        this.pushRootPath(path);
        for (PackType packType : PackType.values()) {
            this.pushPathForType(packType, path.resolve(packType.getDirectory()));
        }
        return this;
    }

    public VanillaPackResourcesBuilder pushAssetPath(PackType packType, Path path) {
        this.pushRootPath(path);
        this.pushPathForType(packType, path);
        return this;
    }

    public VanillaPackResourcesBuilder setMetadata(ResourceMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public VanillaPackResourcesBuilder exposeNamespace(String ... namespaces) {
        this.namespaces.addAll(Arrays.asList(namespaces));
        return this;
    }

    public VanillaPackResources build(PackLocationInfo location) {
        return new VanillaPackResources(location, this.metadata, Set.copyOf(this.namespaces), VanillaPackResourcesBuilder.copyAndReverse(this.rootPaths), Util.makeEnumMap(PackType.class, packType -> VanillaPackResourcesBuilder.copyAndReverse(this.pathsForType.getOrDefault(packType, Set.of()))));
    }

    private static List<Path> copyAndReverse(Collection<Path> input) {
        ArrayList<Path> paths = new ArrayList<Path>(input);
        Collections.reverse(paths);
        return List.copyOf(paths);
    }
}

