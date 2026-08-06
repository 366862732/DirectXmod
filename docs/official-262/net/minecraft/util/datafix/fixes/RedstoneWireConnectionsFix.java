/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.serialization.Dynamic
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.References;

public class RedstoneWireConnectionsFix
extends DataFix {
    public RedstoneWireConnectionsFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Schema inputSchema = this.getInputSchema();
        return this.fixTypeEverywhereTyped("RedstoneConnectionsFix", inputSchema.getType(References.BLOCK_STATE), input -> input.update(DSL.remainderFinder(), this::updateRedstoneConnections));
    }

    private <T> Dynamic<T> updateRedstoneConnections(Dynamic<T> state) {
        boolean isRedstone = state.get("Name").asString().result().filter("minecraft:redstone_wire"::equals).isPresent();
        if (!isRedstone) {
            return state;
        }
        return state.update("Properties", props -> {
            String east = props.get("east").asString("none");
            String west = props.get("west").asString("none");
            String north = props.get("north").asString("none");
            String south = props.get("south").asString("none");
            boolean eastwest = RedstoneWireConnectionsFix.isConnected(east) || RedstoneWireConnectionsFix.isConnected(west);
            boolean northsouth = RedstoneWireConnectionsFix.isConnected(north) || RedstoneWireConnectionsFix.isConnected(south);
            String newEast = !RedstoneWireConnectionsFix.isConnected(east) && !northsouth ? "side" : east;
            String newWest = !RedstoneWireConnectionsFix.isConnected(west) && !northsouth ? "side" : west;
            String newNorth = !RedstoneWireConnectionsFix.isConnected(north) && !eastwest ? "side" : north;
            String newSouth = !RedstoneWireConnectionsFix.isConnected(south) && !eastwest ? "side" : south;
            return props.update("east", value -> value.createString(newEast)).update("west", value -> value.createString(newWest)).update("north", value -> value.createString(newNorth)).update("south", value -> value.createString(newSouth));
        });
    }

    private static boolean isConnected(String connectionType) {
        return !"none".equals(connectionType);
    }
}

