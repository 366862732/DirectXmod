/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableList
 *  com.google.common.collect.ImmutableList$Builder
 *  com.mojang.datafixers.DataFixer
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FileUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.DirectoryTemplateSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.ResourceManagerTemplateSource;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplatePathFactory;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplateSource;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class StructureTemplateManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
    private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
    public static final String STRUCTURE_DIRECTORY_NAME = "structure";
    public static final FileToIdConverter WORLD_STRUCTURE_LISTER = new FileToIdConverter("structure", ".nbt");
    private static final FileToIdConverter WORLD_TEXT_STRUCTURE_LISTER = new FileToIdConverter("structure", ".snbt");
    private static final FileToIdConverter RESOURCE_STRUCTURE_LISTER = new FileToIdConverter("structure", ".nbt");
    public static final FileToIdConverter RESOURCE_TEXT_STRUCTURE_LISTER = new FileToIdConverter("structure", ".snbt");
    private final Map<Identifier, Optional<StructureTemplate>> structureRepository = new ConcurrentHashMap<Identifier, Optional<StructureTemplate>>();
    private final ResourceManagerTemplateSource resourceManagerSource;
    private final List<TemplateSource> sources;
    private final TemplatePathFactory worldTemplates;
    private final @Nullable TemplatePathFactory testTemplates;

    public StructureTemplateManager(ResourceManager resourceManager, LevelStorageSource.LevelStorageAccess storage, DataFixer fixerUpper, HolderGetter<Block> blockLookup) {
        this.resourceManagerSource = new ResourceManagerTemplateSource(fixerUpper, blockLookup, resourceManager, RESOURCE_STRUCTURE_LISTER);
        Path generatedDir = storage.getLevelPath(LevelResource.GENERATED_DIR).normalize();
        this.worldTemplates = new TemplatePathFactory(generatedDir);
        this.testTemplates = StructureUtils.testStructuresTargetDir != null ? new TemplatePathFactory(StructureUtils.testStructuresTargetDir, PackType.SERVER_DATA) : null;
        ImmutableList.Builder sources = ImmutableList.builder();
        sources.add((Object)new DirectoryTemplateSource(fixerUpper, blockLookup, generatedDir, WORLD_STRUCTURE_LISTER, false));
        if (StructureUtils.testStructuresSourceDir != null) {
            sources.add((Object)new DirectoryTemplateSource(fixerUpper, blockLookup, StructureUtils.testStructuresSourceDir, PackType.SERVER_DATA, RESOURCE_TEXT_STRUCTURE_LISTER, true));
        }
        sources.add((Object)this.resourceManagerSource);
        this.sources = sources.build();
    }

    public StructureTemplate getOrCreate(Identifier id) {
        Optional<StructureTemplate> cachedTemplate = this.get(id);
        if (cachedTemplate.isPresent()) {
            return cachedTemplate.get();
        }
        StructureTemplate template = new StructureTemplate();
        this.structureRepository.put(id, Optional.of(template));
        return template;
    }

    public Optional<StructureTemplate> get(Identifier id) {
        return this.structureRepository.computeIfAbsent(id, this::tryLoad);
    }

    public Stream<Identifier> listTemplates() {
        return this.sources.stream().flatMap(TemplateSource::list).distinct();
    }

    private Optional<StructureTemplate> tryLoad(Identifier id) {
        for (TemplateSource source : this.sources) {
            try {
                Optional<StructureTemplate> loaded = source.load(id);
                if (!loaded.isPresent()) continue;
                return loaded;
            }
            catch (Exception exception) {
            }
        }
        return Optional.empty();
    }

    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.resourceManagerSource.setResourceManager(resourceManager);
        this.structureRepository.clear();
    }

    public boolean save(Identifier id) {
        boolean saveAsText;
        Path file;
        Optional<StructureTemplate> structureTemplate = this.structureRepository.get(id);
        if (structureTemplate.isEmpty()) {
            return false;
        }
        if (SharedConstants.DEBUG_SAVE_STRUCTURES_AS_SNBT) {
            file = this.worldTemplates.createAndValidatePathToStructure(id, WORLD_TEXT_STRUCTURE_LISTER);
            saveAsText = true;
        } else {
            file = this.worldTemplates.createAndValidatePathToStructure(id, WORLD_STRUCTURE_LISTER);
            saveAsText = false;
        }
        try {
            return StructureTemplateManager.save(file, structureTemplate.get(), saveAsText);
        }
        catch (Exception e) {
            LOGGER.warn("Failed to save structure file {} to {}", new Object[]{id, file, e});
            return false;
        }
    }

    public static boolean save(Path file, StructureTemplate structureTemplate, boolean asText) throws IOException {
        Path parent = file.getParent();
        if (parent == null) {
            return false;
        }
        FileUtil.createDirectoriesSafe(parent);
        CompoundTag tag = structureTemplate.save(new CompoundTag());
        if (asText) {
            NbtToSnbt.writeSnbt(CachedOutput.NO_CACHE, file, NbtUtils.structureToSnbt(tag));
        } else {
            try (FileOutputStream output = new FileOutputStream(file.toFile());){
                NbtIo.writeCompressed(tag, output);
            }
        }
        return true;
    }

    public TemplatePathFactory worldTemplates() {
        return this.worldTemplates;
    }

    public @Nullable TemplatePathFactory testTemplates() {
        return this.testTemplates;
    }

    public void remove(Identifier id) {
        this.structureRepository.remove(id);
    }
}

