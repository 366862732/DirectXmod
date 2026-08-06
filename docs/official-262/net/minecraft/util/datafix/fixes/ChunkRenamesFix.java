/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.OpticFinder
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.Typed
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.DataResult
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.function.Function;
import net.minecraft.util.datafix.fixes.References;

public class ChunkRenamesFix
extends DataFix {
    public ChunkRenamesFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        OpticFinder levelFinder = chunkType.findField("Level");
        OpticFinder structureFinder = levelFinder.type().findField("Structures");
        Type newChunkType = this.getOutputSchema().getType(References.CHUNK);
        Type newStructuresType = newChunkType.findFieldType("structures");
        return this.fixTypeEverywhereTyped("Chunk Renames; purge Level-tag", chunkType, newChunkType, chunk -> {
            Typed level = chunk.getTyped(levelFinder);
            Typed<?> chunkTyped = ChunkRenamesFix.appendChunkName(level);
            chunkTyped = chunkTyped.set(DSL.remainderFinder(), ChunkRenamesFix.mergeRemainders(chunk, (Dynamic)level.get(DSL.remainderFinder())));
            chunkTyped = ChunkRenamesFix.renameField(chunkTyped, "TileEntities", "block_entities");
            chunkTyped = ChunkRenamesFix.renameField(chunkTyped, "TileTicks", "block_ticks");
            chunkTyped = ChunkRenamesFix.renameField(chunkTyped, "Entities", "entities");
            chunkTyped = ChunkRenamesFix.renameField(chunkTyped, "Sections", "sections");
            chunkTyped = chunkTyped.updateTyped(structureFinder, newStructuresType, structure -> ChunkRenamesFix.renameField(structure, "Starts", "starts"));
            chunkTyped = ChunkRenamesFix.renameField(chunkTyped, "Structures", "structures");
            return chunkTyped.update(DSL.remainderFinder(), remainder -> remainder.remove("Level"));
        });
    }

    private static Typed<?> renameField(Typed<?> input, String oldName, String newName) {
        return ChunkRenamesFix.renameFieldHelper(input, oldName, newName, input.getType().findFieldType(oldName)).update(DSL.remainderFinder(), tag -> tag.remove(oldName));
    }

    private static <A> Typed<?> renameFieldHelper(Typed<?> input, String oldName, String newName, Type<A> fieldType) {
        Type oldType = DSL.optional((Type)DSL.field((String)oldName, fieldType));
        Type newType = DSL.optional((Type)DSL.field((String)newName, fieldType));
        return input.update(oldType.finder(), newType, Function.identity());
    }

    private static <A> Typed<Pair<String, A>> appendChunkName(Typed<A> input) {
        return new Typed(DSL.named((String)"chunk", (Type)input.getType()), input.getOps(), (Object)Pair.of((Object)"chunk", (Object)input.getValue()));
    }

    private static <T> Dynamic<T> mergeRemainders(Typed<?> chunk, Dynamic<T> levelRemainder) {
        DynamicOps ops = levelRemainder.getOps();
        Dynamic chunkRemainder = ((Dynamic)chunk.get(DSL.remainderFinder())).convert(ops);
        DataResult toMap = ops.getMap(levelRemainder.getValue()).flatMap(map -> ops.mergeToMap(chunkRemainder.getValue(), map));
        return toMap.result().map(v -> new Dynamic(ops, v)).orElse(levelRemainder);
    }
}

