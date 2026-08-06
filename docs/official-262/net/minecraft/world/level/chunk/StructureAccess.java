/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.longs.LongSet
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jspecify.annotations.Nullable;

public interface StructureAccess {
    public @Nullable StructureStart getStartForStructure(Structure var1);

    public void setStartForStructure(Structure var1, StructureStart var2);

    public LongSet getReferencesForStructure(Structure var1);

    public void addReferenceForStructure(Structure var1, long var2);

    public Map<Structure, LongSet> getAllReferences();

    public void setAllReferences(Map<Structure, LongSet> var1);
}

