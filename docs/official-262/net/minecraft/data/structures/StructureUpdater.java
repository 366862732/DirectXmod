/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.data.structures;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.structures.SnbtToNbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

public class StructureUpdater
implements SnbtToNbt.Filter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREFIX = PackType.SERVER_DATA.getDirectory() + "/minecraft/structure/";

    @Override
    public CompoundTag apply(String name, CompoundTag input) {
        if (name.startsWith(PREFIX)) {
            return StructureUpdater.update(name, input);
        }
        return input;
    }

    public static CompoundTag update(String name, CompoundTag tag) {
        StructureTemplate structureTemplate = new StructureTemplate();
        int fromVersion = NbtUtils.getDataVersion(tag, 500);
        int toVersion = 4882;
        if (fromVersion < 4882) {
            LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", new Object[]{fromVersion, 4882, name});
        }
        CompoundTag updated = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), tag, fromVersion);
        structureTemplate.load(BuiltInRegistries.BLOCK, updated);
        return structureTemplate.save(new CompoundTag());
    }
}

