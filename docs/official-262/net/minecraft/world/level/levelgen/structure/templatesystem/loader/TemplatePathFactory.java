/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.world.level.levelgen.structure.templatesystem.loader;

import java.nio.file.Path;
import java.util.List;
import net.minecraft.IdentifierException;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.FileUtil;

public class TemplatePathFactory {
    private final Path sourceDir;

    public TemplatePathFactory(Path sourceDir, PackType packType) {
        this(sourceDir.resolve(packType.getDirectory()));
    }

    public TemplatePathFactory(Path sourceDir) {
        this.sourceDir = sourceDir;
    }

    public Path createAndValidatePathToStructure(Identifier id, FileToIdConverter converter) {
        return this.createAndValidatePathToStructure(converter.idToFile(id));
    }

    public Path createAndValidatePathToStructure(Identifier resourceLocation) {
        Path namespacePath = this.sourceDir.resolve(resourceLocation.getNamespace());
        List decomposedPath = (List)FileUtil.decomposePath(resourceLocation.getPath()).getOrThrow(msg -> new IdentifierException("Invalid file path '" + String.valueOf(resourceLocation) + "': " + msg));
        if (!decomposedPath.stream().allMatch(FileUtil::isPathPartPortable)) {
            throw new IdentifierException("Resource path '" + String.valueOf(resourceLocation) + "' is not portable");
        }
        return FileUtil.resolvePath(namespacePath, decomposedPath);
    }
}

