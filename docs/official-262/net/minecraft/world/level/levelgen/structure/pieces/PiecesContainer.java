/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.ImmutableMap
 *  com.google.common.collect.Lists
 *  com.mojang.logging.LogUtils
 *  org.slf4j.Logger
 */
package net.minecraft.world.level.levelgen.structure.pieces;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.slf4j.Logger;

public record PiecesContainer(List<StructurePiece> pieces) {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Identifier JIGSAW_RENAME = Identifier.withDefaultNamespace("jigsaw");
    private static final Map<Identifier, Identifier> RENAMES = ImmutableMap.builder().put((Object)Identifier.withDefaultNamespace("nvi"), (Object)JIGSAW_RENAME).put((Object)Identifier.withDefaultNamespace("pcp"), (Object)JIGSAW_RENAME).put((Object)Identifier.withDefaultNamespace("bastionremnant"), (Object)JIGSAW_RENAME).put((Object)Identifier.withDefaultNamespace("runtime"), (Object)JIGSAW_RENAME).build();

    public PiecesContainer(List<StructurePiece> pieces) {
        this.pieces = List.copyOf(pieces);
    }

    public boolean isEmpty() {
        return this.pieces.isEmpty();
    }

    public boolean isInsidePiece(BlockPos startPos) {
        for (StructurePiece piece : this.pieces) {
            if (!piece.getBoundingBox().isInside(startPos)) continue;
            return true;
        }
        return false;
    }

    public Tag save(StructurePieceSerializationContext context) {
        ListTag childrenTags = new ListTag();
        for (StructurePiece piece : this.pieces) {
            childrenTags.add(piece.createTag(context));
        }
        return childrenTags;
    }

    public static PiecesContainer load(ListTag children, StructurePieceSerializationContext context) {
        ArrayList pieces = Lists.newArrayList();
        for (int i = 0; i < children.size(); ++i) {
            CompoundTag pieceTag = children.getCompoundOrEmpty(i);
            String oldId = pieceTag.getStringOr("id", "").toLowerCase(Locale.ROOT);
            Identifier oldPieceKey = Identifier.parse(oldId);
            Identifier pieceId = RENAMES.getOrDefault(oldPieceKey, oldPieceKey);
            StructurePieceType pieceType = BuiltInRegistries.STRUCTURE_PIECE.getValue(pieceId);
            if (pieceType == null) {
                LOGGER.error("Unknown structure piece id: {}", (Object)pieceId);
                continue;
            }
            try {
                StructurePiece piece = pieceType.load(context, pieceTag);
                pieces.add(piece);
                continue;
            }
            catch (Exception e) {
                LOGGER.error("Exception loading structure piece with id {}", (Object)pieceId, (Object)e);
            }
        }
        return new PiecesContainer(pieces);
    }

    public BoundingBox calculateBoundingBox() {
        return StructurePiece.createBoundingBox(this.pieces.stream());
    }
}

