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
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.datafixers.util.Unit
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
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.DynamicOps;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;

public class EntityRidingToPassengersFix
extends DataFix {
    public EntityRidingToPassengersFix(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    public TypeRewriteRule makeRule() {
        Schema inputSchema = this.getInputSchema();
        Schema outputSchema = this.getOutputSchema();
        Type oldEntityTreeType = inputSchema.getTypeRaw(References.ENTITY_TREE);
        Type newEntityTreeType = outputSchema.getTypeRaw(References.ENTITY_TREE);
        Type entityType = inputSchema.getTypeRaw(References.ENTITY);
        return this.cap(inputSchema, outputSchema, oldEntityTreeType, newEntityTreeType, entityType);
    }

    private <OldEntityTree, NewEntityTree, Entity> TypeRewriteRule cap(Schema inputSchema, Schema outputType, Type<OldEntityTree> oldEntityTreeType, Type<NewEntityTree> newEntityTreeType, Type<Entity> entityType) {
        Type oldType = DSL.named((String)References.ENTITY_TREE.typeName(), (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"Riding", oldEntityTreeType)), entityType));
        Type newType = DSL.named((String)References.ENTITY_TREE.typeName(), (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"Passengers", (Type)DSL.list(newEntityTreeType))), entityType));
        Type oldEntityType = inputSchema.getType(References.ENTITY_TREE);
        Type newEntityType = outputType.getType(References.ENTITY_TREE);
        if (!Objects.equals(oldEntityType, oldType)) {
            throw new IllegalStateException("Old entity type is not what was expected.");
        }
        if (!newEntityType.equals((Object)newType, true, true)) {
            throw new IllegalStateException("New entity type is not what was expected.");
        }
        Type<?> patchedEntityTreeType = ExtraDataFixUtils.patchSubType(oldType, oldType, newType);
        OpticFinder entityFinder = DSL.typeFinder(entityType);
        OpticFinder newEntityTreeValueFinder = DSL.typeFinder((Type)newType);
        OpticFinder ridingFinder = DSL.fieldFinder((String)"Riding", newEntityTreeType);
        Type oldPlayerType = inputSchema.getType(References.PLAYER);
        Type newPlayerType = outputType.getType(References.PLAYER);
        return TypeRewriteRule.seq((TypeRewriteRule)this.fixTypeEverywhere("EntityRidingToPassengerFix", oldType, newType, ops -> badlyTypedInput -> {
            Typed input = ExtraDataFixUtils.cast(patchedEntityTreeType, badlyTypedInput, ops);
            Optional maybeRiding = input.getOptionalTyped(ridingFinder).flatMap(t -> t.getOptional(newEntityTreeValueFinder));
            Object entity = input.getOptional(entityFinder).orElseThrow();
            if (maybeRiding.isEmpty()) {
                Either passengers = Either.right((Object)Unit.INSTANCE);
                return Pair.of((Object)References.ENTITY_TREE.typeName(), (Object)Pair.of((Object)passengers, entity));
            }
            return EntityRidingToPassengersFix.addPassengerToTop((Pair)maybeRiding.get(), entity, ops, newEntityTreeType, newEntityTreeValueFinder);
        }), (TypeRewriteRule)this.writeAndRead("player RootVehicle injecter", oldPlayerType, newPlayerType));
    }

    private static <Entity, EntityTree> Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>> addPassengerToTop(Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>> root, Entity passengerEntity, DynamicOps<?> ops, Type<EntityTree> rawEntityTreeType, OpticFinder<Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>>> entityTreeFinder) {
        Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>> newPassenger;
        Object rootEntity = ((Pair)root.getSecond()).getSecond();
        Optional passengers = ((Either)((Pair)root.getSecond()).getFirst()).left();
        if (passengers.isPresent() && !((List)passengers.get()).isEmpty()) {
            Pair<String, Pair<Either<List<EntityTree>, Unit>, Entity>> unwrappedPassenger = EntityRidingToPassengersFix.unwrapRecursiveValue(((List)passengers.get()).getFirst(), ops, rawEntityTreeType, entityTreeFinder);
            newPassenger = EntityRidingToPassengersFix.addPassengerToTop(unwrappedPassenger, passengerEntity, ops, rawEntityTreeType, entityTreeFinder);
        } else {
            newPassenger = Pair.of((Object)References.ENTITY_TREE.typeName(), (Object)Pair.of((Object)Either.right((Object)Unit.INSTANCE), passengerEntity));
        }
        List<EntityTree> newPassengers = List.of(EntityRidingToPassengersFix.wrapRecursiveValue(newPassenger, ops, rawEntityTreeType, entityTreeFinder));
        return Pair.of((Object)References.ENTITY_TREE.typeName(), (Object)Pair.of((Object)Either.left(newPassengers), (Object)rootEntity));
    }

    private static <Raw, Value> Value unwrapRecursiveValue(Raw raw, DynamicOps<?> ops, Type<Raw> rawType, OpticFinder<Value> valueFinder) {
        return (Value)new Typed(rawType, ops, raw).getOptional(valueFinder).orElseThrow();
    }

    private static <Raw, Value> Raw wrapRecursiveValue(Value value, DynamicOps<?> ops, Type<Raw> rawType, OpticFinder<Value> valueFinder) {
        return (Raw)((Typed)rawType.pointTyped(ops).orElseThrow()).set(valueFinder, value).getValue();
    }
}

