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
 *  com.mojang.datafixers.types.templates.List$ListType
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.types.templates.List;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class RemoveBlockEntityTagFix
extends DataFix {
    private final Set<String> blockEntityIdsToDrop;
    private final boolean useLegacyDataStructure;

    public RemoveBlockEntityTagFix(Schema outputSchema, Set<String> blockEntityIdsToDrop) {
        this(outputSchema, false, blockEntityIdsToDrop);
    }

    public RemoveBlockEntityTagFix(Schema outputSchema, boolean useLegacyDataStructure, Set<String> blockEntityIdsToDrop) {
        super(outputSchema, true);
        this.blockEntityIdsToDrop = blockEntityIdsToDrop;
        this.useLegacyDataStructure = useLegacyDataStructure;
    }

    public TypeRewriteRule makeRule() {
        OpticFinder blockEntityIdF = DSL.fieldFinder((String)"id", NamespacedSchema.namespacedString());
        if (this.useLegacyDataStructure) {
            return TypeRewriteRule.seq((TypeRewriteRule)this.createItemBlockEntityRemover((OpticFinder<String>)blockEntityIdF, "tag", "BlockEntityTag"), (TypeRewriteRule[])new TypeRewriteRule[]{this.createFallingBlockBlockEntityRemover((OpticFinder<String>)blockEntityIdF), this.createStructureBlockEntityRemover((OpticFinder<String>)blockEntityIdF), this.createUncheckedConverterHack()});
        }
        return TypeRewriteRule.seq((TypeRewriteRule)this.createItemBlockEntityRemover((OpticFinder<String>)blockEntityIdF, "components", "minecraft:block_entity_data"), (TypeRewriteRule[])new TypeRewriteRule[]{this.createFallingBlockBlockEntityRemover((OpticFinder<String>)blockEntityIdF), this.createStructureBlockEntityRemover((OpticFinder<String>)blockEntityIdF), this.createChunkBlockEntityRemover((OpticFinder<String>)blockEntityIdF), this.createUncheckedConverterHack()});
    }

    private TypeRewriteRule createItemBlockEntityRemover(OpticFinder<String> blockEntityIdF, String itemTagOrComponentKey, String itemBlockEntityDataKey) {
        Type itemStackType = this.getInputSchema().getType(References.ITEM_STACK);
        OpticFinder itemTagF = itemStackType.findField(itemTagOrComponentKey);
        OpticFinder itemBlockEntityF = itemTagF.type().findField(itemBlockEntityDataKey);
        return this.fixTypeEverywhereTyped("ItemRemoveBlockEntityTagFix" + this.getOutputSchema().getVersionKey(), itemStackType, input -> input.updateTyped(itemTagF, tag -> this.removeBlockEntity((Typed<?>)tag, (OpticFinder<?>)itemBlockEntityF, blockEntityIdF, itemBlockEntityDataKey)));
    }

    private TypeRewriteRule createFallingBlockBlockEntityRemover(OpticFinder<String> blockEntityIdF) {
        Type entityType = this.getInputSchema().getType(References.ENTITY);
        OpticFinder fallingBlockF = DSL.namedChoice((String)"minecraft:falling_block", (Type)this.getInputSchema().getChoiceType(References.ENTITY, "minecraft:falling_block"));
        OpticFinder fallingBlockEntityTagF = fallingBlockF.type().findField("TileEntityData");
        return this.fixTypeEverywhereTyped("FallingBlockEntityRemoveBlockEntityTagFix" + this.getOutputSchema().getVersionKey(), entityType, input -> input.updateTyped(fallingBlockF, tag -> this.removeBlockEntity((Typed<?>)tag, (OpticFinder<?>)fallingBlockEntityTagF, blockEntityIdF, "TileEntityData")));
    }

    private TypeRewriteRule createStructureBlockEntityRemover(OpticFinder<String> blockEntityIdF) {
        Type structureType = this.getInputSchema().getType(References.STRUCTURE);
        OpticFinder blocksF = structureType.findField("blocks");
        OpticFinder blockTypeF = DSL.typeFinder((Type)((List.ListType)blocksF.type()).getElement());
        OpticFinder blockNbtF = blockTypeF.type().findField("nbt");
        return this.fixTypeEverywhereTyped("StructureRemoveBlockEntityTagFix" + this.getOutputSchema().getVersionKey(), structureType, input -> input.updateTyped(blocksF, tag -> tag.updateTyped(blockTypeF, blockTag -> this.removeBlockEntity((Typed<?>)blockTag, (OpticFinder<?>)blockNbtF, blockEntityIdF, "nbt"))));
    }

    private TypeRewriteRule createChunkBlockEntityRemover(OpticFinder<String> blockEntityIdF) {
        Type chunkType = this.getInputSchema().getType(References.CHUNK);
        OpticFinder blockEntitiesF = chunkType.findField("block_entities");
        Type blockEntityElementsType = ((List.ListType)blockEntitiesF.type()).getElement();
        OpticFinder blockEntityTypeFinder = this.getInputSchema().getType(References.BLOCK_ENTITY).finder();
        Type chunkTypeOut = this.getOutputSchema().getType(References.CHUNK);
        Type blockEntitiesTypeOut = chunkType.findField("block_entities").type();
        return this.fixTypeEverywhereTyped("BlockEntityChunkRemover" + this.getOutputSchema().getVersionKey(), chunkType, chunkTypeOut, input -> input.update(blockEntitiesF, blockEntitiesTypeOut, listTag -> {
            ArrayList keptBlockEntities = new ArrayList();
            for (Object untypedBlockEntity : listTag) {
                Typed typedBlockEntity = ExtraDataFixUtils.cast(blockEntityElementsType, untypedBlockEntity, input.getOps());
                Typed typedBlockEntityUnwrapped = typedBlockEntity.getOrCreateTyped(blockEntityTypeFinder);
                String blockEntityId = typedBlockEntityUnwrapped.getOptional(blockEntityIdF).orElse("");
                if (this.blockEntityIdsToDrop.contains(blockEntityId)) continue;
                keptBlockEntities.add(untypedBlockEntity);
            }
            return List.copyOf(keptBlockEntities);
        }));
    }

    private TypeRewriteRule createUncheckedConverterHack() {
        return this.convertUnchecked("ItemRemoveBlockEntityTagFix - update block entity type" + this.getOutputSchema().getVersionKey(), this.getInputSchema().getType(References.BLOCK_ENTITY), this.getOutputSchema().getType(References.BLOCK_ENTITY));
    }

    private Typed<?> removeBlockEntity(Typed<?> tag, OpticFinder<?> blockEntityF, OpticFinder<String> blockEntityIdF, String blockEntityFieldName) {
        Optional maybeBlockEntity = tag.getOptionalTyped(blockEntityF);
        if (maybeBlockEntity.isEmpty()) {
            return tag;
        }
        String blockEntityId = ((Typed)maybeBlockEntity.get()).getOptional(blockEntityIdF).orElse("");
        if (!this.blockEntityIdsToDrop.contains(blockEntityId)) {
            return tag;
        }
        return Util.writeAndReadTypedOrThrow(tag, tag.getType(), tagData -> tagData.remove(blockEntityFieldName));
    }
}

