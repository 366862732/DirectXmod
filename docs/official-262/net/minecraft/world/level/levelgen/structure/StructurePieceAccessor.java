/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.world.level.levelgen.structure;

import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.jspecify.annotations.Nullable;

public interface StructurePieceAccessor {
    public void addPiece(StructurePiece var1);

    public @Nullable StructurePiece findCollisionPiece(BoundingBox var1);
}

