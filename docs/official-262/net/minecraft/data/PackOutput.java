/*
 * Decompiled with CFR 0.152.
 */
package net.minecraft.data;

import java.nio.file.Path;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public class PackOutput {
    private final Path outputFolder;

    public PackOutput(Path outputFolder) {
        this.outputFolder = outputFolder;
    }

    public Path getOutputFolder() {
        return this.outputFolder;
    }

    public Path getOutputFolder(Target target) {
        return this.getOutputFolder().resolve(target.directory);
    }

    public PathProvider createPathProvider(Target target, String kind) {
        return new PathProvider(this, target, kind);
    }

    public PathProvider createRegistryElementsPathProvider(ResourceKey<? extends Registry<?>> registryKey) {
        return this.createPathProvider(Target.DATA_PACK, Registries.elementsDirPath(registryKey));
    }

    public PathProvider createRegistryTagsPathProvider(ResourceKey<? extends Registry<?>> registryKey) {
        return this.createPathProvider(Target.DATA_PACK, Registries.tagsDirPath(registryKey));
    }

    public PathProvider createRegistryComponentPathProvider(ResourceKey<? extends Registry<?>> registryKey) {
        return this.createPathProvider(Target.REPORTS, Registries.componentsDirPath(registryKey));
    }

    public static enum Target {
        DATA_PACK("data"),
        RESOURCE_PACK("assets"),
        REPORTS("reports");

        private final String directory;

        private Target(String directory) {
            this.directory = directory;
        }
    }

    public static class PathProvider {
        private final Path root;
        private final String kind;

        private PathProvider(PackOutput output, Target target, String kind) {
            this.root = output.getOutputFolder(target);
            this.kind = kind;
        }

        public Path file(Identifier element, String extension) {
            return element.withPath(path -> this.kind + "/" + path + "." + extension).resolveAgainst(this.root);
        }

        public Path json(Identifier element) {
            return element.withPath(path -> this.kind + "/" + path + ".json").resolveAgainst(this.root);
        }

        public Path json(ResourceKey<?> element) {
            return this.json(element.identifier());
        }
    }
}

