/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JavaOps
 *  com.mojang.serialization.JsonOps
 */
package net.minecraft.util.datafix.fixes;

import com.google.gson.JsonElement;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JavaOps;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.fixes.References;

public class LegacyHoverEventFix
extends DataFix {
    public LegacyHoverEventFix(Schema outputSchema) {
        super(outputSchema, false);
    }

    protected TypeRewriteRule makeRule() {
        Type hoverEventType = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
        return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), hoverEventType);
    }

    private <C, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C> rawTextComponentType, Type<H> hoverEventType) {
        Type textComponentType = DSL.named((String)References.TEXT_COMPONENT.typeName(), (Type)DSL.or((Type)DSL.or((Type)DSL.string(), (Type)DSL.list(rawTextComponentType)), (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"extra", (Type)DSL.list(rawTextComponentType))), (Type)DSL.optional((Type)DSL.field((String)"separator", rawTextComponentType)), (Type)DSL.optional((Type)DSL.field((String)"hoverEvent", hoverEventType)), (Type)DSL.remainderType())));
        if (!textComponentType.equals((Object)this.getInputSchema().getType(References.TEXT_COMPONENT))) {
            throw new IllegalStateException("Text component type did not match, expected " + String.valueOf(textComponentType) + " but got " + String.valueOf(this.getInputSchema().getType(References.TEXT_COMPONENT)));
        }
        return this.fixTypeEverywhere("LegacyHoverEventFix", textComponentType, ops -> named -> named.mapSecond(simpleOrFull -> simpleOrFull.mapRight(full -> full.mapSecond(separatorHoverRemainder -> separatorHoverRemainder.mapSecond(hoverAndRemainder -> {
            Dynamic remainder = (Dynamic)hoverAndRemainder.getSecond();
            Optional hoverEvent = remainder.get("hoverEvent").result();
            if (hoverEvent.isEmpty()) {
                return hoverAndRemainder;
            }
            Optional legacyHoverValue = ((Dynamic)hoverEvent.get()).get("value").result();
            if (legacyHoverValue.isEmpty()) {
                return hoverAndRemainder;
            }
            String hoverAction = ((Either)hoverAndRemainder.getFirst()).left().map(Pair::getFirst).orElse("");
            Pair newHoverEvent = (Pair)this.fixHoverEvent(hoverEventType, hoverAction, (Dynamic)hoverEvent.get());
            return hoverAndRemainder.mapFirst(ignored -> Either.left((Object)newHoverEvent));
        })))));
    }

    private <H> H fixHoverEvent(Type<H> hoverEventType, String action, Dynamic<?> oldHoverEvent) {
        if ("show_text".equals(action)) {
            return LegacyHoverEventFix.fixShowTextHover(hoverEventType, oldHoverEvent);
        }
        return LegacyHoverEventFix.createPlaceholderHover(hoverEventType, oldHoverEvent);
    }

    private static <H> H fixShowTextHover(Type<H> hoverEventType, Dynamic<?> oldHoverEvent) {
        Dynamic newHoverEvent = oldHoverEvent.renameField("value", "contents");
        return (H)Util.readTypedOrThrow(hoverEventType, newHoverEvent).getValue();
    }

    private static <H> H createPlaceholderHover(Type<H> hoverEventType, Dynamic<?> oldHoverEvent) {
        JsonElement oldJson = (JsonElement)oldHoverEvent.convert((DynamicOps)JsonOps.INSTANCE).getValue();
        Dynamic placeholderHoverEvent = new Dynamic((DynamicOps)JavaOps.INSTANCE, Map.of("action", "show_text", "contents", Map.of("text", "Legacy hoverEvent: " + GsonHelper.toStableString(oldJson))));
        return (H)Util.readTypedOrThrow(hoverEventType, placeholderHoverEvent).getValue();
    }
}

