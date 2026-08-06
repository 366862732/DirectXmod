/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.structure.templatesystem.loader;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplateSource;
import org.slf4j.Logger;

public class DirectoryTemplateSource
extends TemplateSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path sourceDir;
    private final FileToIdConverter fileToIdConverter;
    private final boolean loadAsText;

    public DirectoryTemplateSource(DataFixer fixerUpper, HolderGetter<Block> blockLookup, Path sourceDir, PackType packType, FileToIdConverter fileToIdConverter, boolean loadAsText) {
        this(fixerUpper, blockLookup, sourceDir.resolve(packType.getDirectory()), fileToIdConverter, loadAsText);
    }

    public DirectoryTemplateSource(DataFixer fixerUpper, HolderGetter<Block> blockLookup, Path sourceDir, FileToIdConverter fileToIdConverter, boolean loadAsText) {
        super(fixerUpper, blockLookup);
        this.sourceDir = sourceDir;
        this.fileToIdConverter = fileToIdConverter;
        this.loadAsText = loadAsText;
    }

    @Override
    public Optional<StructureTemplate> load(Identifier id) {
        if (!Files.isDirectory(this.sourceDir, new LinkOption[0])) {
            return Optional.empty();
        }
        IoSupplier<InputStream> resource = PathPackResources.getResource(this.sourceDir, this.fileToIdConverter.idToFile(id));
        if (resource == null) {
            return Optional.empty();
        }
        return this.load(resource, this.loadAsText, e -> LOGGER.error("Couldn't load structure from {}:{}", new Object[]{this.sourceDir, id, e}));
    }

    @Override
    public Stream<Identifier> list() {
        if (!Files.isDirectory(this.sourceDir, new LinkOption[0])) {
            return Stream.empty();
        }
        Stream.Builder resultBuilder = Stream.builder();
        Set<String> namespaces = PathPackResources.getNamespaces(this.sourceDir);
        for (String namespace : namespaces) {
            PathPackResources.listResources(this.sourceDir, namespace, this.fileToIdConverter.prefix(), (id, ioSupplier) -> {
                if (this.fileToIdConverter.extensionMatches((Identifier)id)) {
                    resultBuilder.accept(this.fileToIdConverter.fileToId((Identifier)id));
                }
            });
        }
        return resultBuilder.build();
    }
}

