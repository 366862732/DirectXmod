/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.apache.commons.lang3.StringUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.repository.FolderRepositorySource;
import net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BuiltInPackSource
implements RepositorySource {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String VANILLA_ID = "vanilla";
    public static final String TESTS_ID = "tests";
    public static final KnownPack CORE_PACK_INFO = KnownPack.vanilla("core");
    private final PackType packType;
    private final VanillaPackResources vanillaPack;
    private final Identifier packDir;
    private final DirectoryValidator validator;

    public BuiltInPackSource(PackType packType, VanillaPackResources vanillaPack, Identifier packDir, DirectoryValidator validator) {
        this.packType = packType;
        this.vanillaPack = vanillaPack;
        this.packDir = packDir;
        this.validator = validator;
    }

    @Override
    public void loadPacks(Consumer<Pack> result) {
        Pack vanilla = this.createVanillaPack(this.vanillaPack);
        if (vanilla != null) {
            result.accept(vanilla);
        }
        this.listBundledPacks(result);
    }

    protected abstract @Nullable Pack createVanillaPack(PackResources var1);

    protected abstract Component getPackTitle(String var1);

    public VanillaPackResources getVanillaPack() {
        return this.vanillaPack;
    }

    /*
     * Issues handling annotations - annotations may be inaccurate
     */
    private void listBundledPacks(Consumer<Pack> packConsumer) {
        HashMap<String, @Nullable Function> discoveredPacks = new HashMap<String, Function>();
        this.populatePackList(discoveredPacks::put);
        discoveredPacks.forEach((id, packSupplier) -> {
            Pack pack = (Pack)packSupplier.apply(id);
            if (pack != null) {
                packConsumer.accept(pack);
            }
        });
    }

    protected void populatePackList(BiConsumer<String, Function<String, Pack>> discoveredPacks) {
        this.vanillaPack.listRawPaths(this.packType, this.packDir, path -> this.discoverPacksInPath((Path)path, discoveredPacks));
    }

    protected void discoverPacksInPath(@Nullable Path targetDir, BiConsumer<String, Function<String, @Nullable Pack>> discoveredPacks) {
        if (targetDir != null && Files.isDirectory(targetDir, new LinkOption[0])) {
            try {
                FolderRepositorySource.discoverPacks(targetDir, this.validator, (path, resources) -> discoveredPacks.accept(BuiltInPackSource.pathToId(path), id -> this.createBuiltinPack((String)id, (Pack.ResourcesSupplier)resources, this.getPackTitle((String)id))));
            }
            catch (IOException e) {
                LOGGER.warn("Failed to discover packs in {}", (Object)targetDir, (Object)e);
            }
        }
    }

    private static String pathToId(Path path) {
        return StringUtils.removeEnd((String)path.getFileName().toString(), (String)".zip");
    }

    protected abstract @Nullable Pack createBuiltinPack(String var1, Pack.ResourcesSupplier var2, Component var3);

    protected static Pack.ResourcesSupplier fixedResources(final PackResources instance) {
        return new Pack.ResourcesSupplier(){

            @Override
            public PackResources openPrimary(PackLocationInfo location) {
                return instance;
            }

            @Override
            public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return instance;
            }
        };
    }
}

