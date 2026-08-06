/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.DataResult
 *  org.apache.commons.io.FilenameUtils
 */
package net.minecraft.util;

import com.mojang.serialization.DataResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.SharedConstants;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {
    private static final Pattern COPY_COUNTER_PATTERN = Pattern.compile("(<name>.*) \\((<count>\\d*)\\)", 66);
    private static final int MAX_FILE_NAME = 255;
    private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", 2);
    private static final Pattern STRICT_PATH_SEGMENT_CHECK = Pattern.compile("[-._a-z0-9]+");

    public static String sanitizeName(String baseName) {
        for (char replacer : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
            baseName = baseName.replace(replacer, '_');
        }
        return baseName.replaceAll("[./\"]", "_");
    }

    public static String findAvailableName(Path baseDir, String baseName, String suffix) throws IOException {
        if (!FileUtil.isPathPartPortable((String)(baseName = FileUtil.sanitizeName((String)baseName)))) {
            baseName = "_" + (String)baseName + "_";
        }
        Matcher matcher = COPY_COUNTER_PATTERN.matcher((CharSequence)baseName);
        int count = 0;
        if (matcher.matches()) {
            baseName = matcher.group("name");
            count = Integer.parseInt(matcher.group("count"));
        }
        if (((String)baseName).length() > 255 - suffix.length()) {
            baseName = ((String)baseName).substring(0, 255 - suffix.length());
        }
        while (true) {
            Object nameToTest = baseName;
            if (count != 0) {
                String countSuffix = " (" + count + ")";
                int length = 255 - countSuffix.length();
                if (((String)nameToTest).length() > length) {
                    nameToTest = ((String)nameToTest).substring(0, length);
                }
                nameToTest = (String)nameToTest + countSuffix;
            }
            nameToTest = (String)nameToTest + suffix;
            Path fullPath = baseDir.resolve((String)nameToTest);
            try {
                Path created = Files.createDirectory(fullPath, new FileAttribute[0]);
                Files.deleteIfExists(created);
                return baseDir.relativize(created).toString();
            }
            catch (FileAlreadyExistsException e) {
                ++count;
                continue;
            }
            break;
        }
    }

    public static boolean isPathPortable(Path path) {
        for (Path part : path) {
            if (FileUtil.isPathPartPortable(part.toString())) continue;
            return false;
        }
        return true;
    }

    public static boolean isPathPartPortable(String name) {
        return !RESERVED_WINDOWS_FILENAMES.matcher(name).matches();
    }

    public static String getFullResourcePath(String filename) {
        return FilenameUtils.getFullPath((String)filename).replace(File.separator, "/");
    }

    public static String normalizeResourcePath(String filename) {
        return FilenameUtils.normalize((String)filename).replace(File.separator, "/");
    }

    public static DataResult<List<String>> decomposePath(String path) {
        int segmentEnd = path.indexOf(47);
        if (segmentEnd == -1) {
            return switch (path) {
                case "", ".", ".." -> DataResult.error(() -> "Invalid path '" + path + "'");
                default -> !FileUtil.containsAllowedCharactersOnly(path) ? DataResult.error(() -> "Invalid path '" + path + "'") : DataResult.success(List.of(path));
            };
        }
        ArrayList<String> result = new ArrayList<String>();
        int segmentStart = 0;
        boolean lastSegment = false;
        while (true) {
            String segment;
            switch (segment = path.substring(segmentStart, segmentEnd)) {
                case "": 
                case ".": 
                case "..": {
                    return DataResult.error(() -> "Invalid segment '" + segment + "' in path '" + path + "'");
                }
            }
            if (!FileUtil.containsAllowedCharactersOnly(segment)) {
                return DataResult.error(() -> "Invalid segment '" + segment + "' in path '" + path + "'");
            }
            result.add(segment);
            if (lastSegment) {
                return DataResult.success(result);
            }
            segmentStart = segmentEnd + 1;
            if ((segmentEnd = path.indexOf(47, segmentStart)) != -1) continue;
            segmentEnd = path.length();
            lastSegment = true;
        }
    }

    public static Path resolvePath(Path root, List<String> segments) {
        int size = segments.size();
        return switch (size) {
            case 0 -> root;
            case 1 -> root.resolve(segments.get(0));
            default -> {
                String[] rest = new String[size - 1];
                for (int i = 1; i < size; ++i) {
                    rest[i - 1] = segments.get(i);
                }
                yield root.resolve(root.getFileSystem().getPath(segments.get(0), rest));
            }
        };
    }

    private static boolean containsAllowedCharactersOnly(String segment) {
        return STRICT_PATH_SEGMENT_CHECK.matcher(segment).matches();
    }

    public static boolean isValidPathSegment(String segment) {
        return !segment.equals("..") && !segment.equals(".") && FileUtil.containsAllowedCharactersOnly(segment);
    }

    public static void validatePath(String ... path) {
        if (path.length == 0) {
            throw new IllegalArgumentException("Path must have at least one element");
        }
        for (String segment : path) {
            if (FileUtil.isValidPathSegment(segment)) continue;
            throw new IllegalArgumentException("Illegal segment " + segment + " in path " + Arrays.toString(path));
        }
    }

    public static void createDirectoriesSafe(Path dir) throws IOException {
        Files.createDirectories(Files.exists(dir, new LinkOption[0]) ? dir.toRealPath(new LinkOption[0]) : dir, new FileAttribute[0]);
    }

    public static boolean isEmptyPath(Path path) {
        return path.getNameCount() == 1 && path.getFileName().toString().isEmpty();
    }
}

