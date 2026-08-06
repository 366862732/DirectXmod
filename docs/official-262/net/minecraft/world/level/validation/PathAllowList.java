/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.validation;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

public class PathAllowList
implements PathMatcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String COMMENT_PREFIX = "#";
    private final List<ConfigEntry> entries;
    private final Map<String, PathMatcher> compiledPaths = new ConcurrentHashMap<String, PathMatcher>();

    public PathAllowList(List<ConfigEntry> entries) {
        this.entries = entries;
    }

    public PathMatcher getForFileSystem(FileSystem fileSystem) {
        return this.compiledPaths.computeIfAbsent(fileSystem.provider().getScheme(), scheme -> {
            List<PathMatcher> compiledMatchers;
            try {
                compiledMatchers = this.entries.stream().map(e -> e.compile(fileSystem)).toList();
            }
            catch (Exception e2) {
                LOGGER.error("Failed to compile file pattern list", (Throwable)e2);
                return path -> false;
            }
            return switch (compiledMatchers.size()) {
                case 0 -> path -> false;
                case 1 -> compiledMatchers.get(0);
                default -> path -> {
                    for (PathMatcher matcher : compiledMatchers) {
                        if (!matcher.matches(path)) continue;
                        return true;
                    }
                    return false;
                };
            };
        });
    }

    @Override
    public boolean matches(Path path) {
        return this.getForFileSystem(path.getFileSystem()).matches(path);
    }

    public static PathAllowList readPlain(BufferedReader reader) {
        return new PathAllowList(reader.lines().flatMap(line -> ConfigEntry.parse(line).stream()).toList());
    }

    public record ConfigEntry(EntryType type, String pattern) {
        public PathMatcher compile(FileSystem fileSystem) {
            return this.type().compile(fileSystem, this.pattern);
        }

        public static Optional<ConfigEntry> parse(String definition) {
            if (definition.isBlank() || definition.startsWith(PathAllowList.COMMENT_PREFIX)) {
                return Optional.empty();
            }
            if (!definition.startsWith("[")) {
                return Optional.of(new ConfigEntry(EntryType.PREFIX, definition));
            }
            int split = definition.indexOf(93, 1);
            if (split == -1) {
                throw new IllegalArgumentException("Unterminated type in line '" + definition + "'");
            }
            String type = definition.substring(1, split);
            String contents = definition.substring(split + 1);
            return switch (type) {
                case "glob", "regex" -> Optional.of(new ConfigEntry(EntryType.FILESYSTEM, type + ":" + contents));
                case "prefix" -> Optional.of(new ConfigEntry(EntryType.PREFIX, contents));
                default -> throw new IllegalArgumentException("Unsupported definition type in line '" + definition + "'");
            };
        }

        @VisibleForTesting
        static ConfigEntry glob(String pattern) {
            return new ConfigEntry(EntryType.FILESYSTEM, "glob:" + pattern);
        }

        @VisibleForTesting
        static ConfigEntry regex(String pattern) {
            return new ConfigEntry(EntryType.FILESYSTEM, "regex:" + pattern);
        }

        @VisibleForTesting
        static ConfigEntry prefix(String pattern) {
            return new ConfigEntry(EntryType.PREFIX, pattern);
        }
    }

    @FunctionalInterface
    public static interface EntryType {
        public static final EntryType FILESYSTEM = FileSystem::getPathMatcher;
        public static final EntryType PREFIX = (fileSystem, pattern) -> path -> path.toString().startsWith(pattern);

        public PathMatcher compile(FileSystem var1, String var2);
    }
}

