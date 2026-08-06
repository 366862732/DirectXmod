/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.DSL
 *  com.mojang.datafixers.DataFix
 *  com.mojang.datafixers.TypeRewriteRule
 *  com.mojang.datafixers.schemas.Schema
 *  com.mojang.datafixers.types.Type
 *  com.mojang.datafixers.util.Either
 *  com.mojang.datafixers.util.Pair
 *  com.mojang.serialization.Dynamic
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.ExtraDataFixUtils;
import net.minecraft.util.datafix.fixes.References;
import org.jspecify.annotations.Nullable;

public class TextComponentHoverAndClickEventFix
extends DataFix {
    public TextComponentHoverAndClickEventFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    protected TypeRewriteRule makeRule() {
        Type hoverEventType = this.getInputSchema().getType(References.TEXT_COMPONENT).findFieldType("hoverEvent");
        return this.createFixer(this.getInputSchema().getTypeRaw(References.TEXT_COMPONENT), this.getOutputSchema().getType(References.TEXT_COMPONENT), hoverEventType);
    }

    private <C1, C2, H extends Pair<String, ?>> TypeRewriteRule createFixer(Type<C1> oldRawTextComponentType, Type<C2> newTextComponentType, Type<H> hoverEventType) {
        Type oldTextComponentType = DSL.named((String)References.TEXT_COMPONENT.typeName(), (Type)DSL.or((Type)DSL.or((Type)DSL.string(), (Type)DSL.list(oldRawTextComponentType)), (Type)DSL.and((Type)DSL.optional((Type)DSL.field((String)"extra", (Type)DSL.list(oldRawTextComponentType))), (Type)DSL.optional((Type)DSL.field((String)"separator", oldRawTextComponentType)), (Type)DSL.optional((Type)DSL.field((String)"hoverEvent", hoverEventType)), (Type)DSL.remainderType())));
        if (!oldTextComponentType.equals((Object)this.getInputSchema().getType(References.TEXT_COMPONENT))) {
            throw new IllegalStateException("Text component type did not match, expected " + String.valueOf(oldTextComponentType) + " but got " + String.valueOf(this.getInputSchema().getType(References.TEXT_COMPONENT)));
        }
        Type<?> patchedInputType = ExtraDataFixUtils.patchSubType(oldTextComponentType, oldTextComponentType, newTextComponentType);
        return this.fixTypeEverywhere("TextComponentHoverAndClickEventFix", oldTextComponentType, newTextComponentType, ops -> textComponent -> {
            boolean hasHoverOrClick = (Boolean)((Either)textComponent.getSecond()).map(simple -> false, full -> {
                Pair hoverAndRemainder = (Pair)((Pair)full.getSecond()).getSecond();
                boolean hasHover = ((Either)hoverAndRemainder.getFirst()).left().isPresent();
                boolean hasClick = ((Dynamic)hoverAndRemainder.getSecond()).get("clickEvent").result().isPresent();
                return hasHover || hasClick;
            });
            if (!hasHoverOrClick) {
                return textComponent;
            }
            return Util.writeAndReadTypedOrThrow(ExtraDataFixUtils.cast(patchedInputType, textComponent, ops), newTextComponentType, TextComponentHoverAndClickEventFix::fixTextComponent).getValue();
        });
    }

    private static Dynamic<?> fixTextComponent(Dynamic<?> dynamic) {
        return dynamic.renameAndFixField("hoverEvent", "hover_event", TextComponentHoverAndClickEventFix::fixHoverEvent).renameAndFixField("clickEvent", "click_event", TextComponentHoverAndClickEventFix::fixClickEvent);
    }

    private static Dynamic<?> copyFields(Dynamic<?> target, Dynamic<?> source, String ... fields) {
        for (String field : fields) {
            target = Dynamic.copyField(source, (String)field, target, (String)field);
        }
        return target;
    }

    private static Dynamic<?> fixHoverEvent(Dynamic<?> dynamic) {
        String action;
        return switch (action = dynamic.get("action").asString("")) {
            case "show_text" -> dynamic.renameField("contents", "value");
            case "show_item" -> {
                Dynamic contents = dynamic.get("contents").orElseEmptyMap();
                Optional simpleId = contents.asString().result();
                if (simpleId.isPresent()) {
                    yield dynamic.renameField("contents", "id");
                }
                yield TextComponentHoverAndClickEventFix.copyFields(dynamic.remove("contents"), contents, "id", "count", "components");
            }
            case "show_entity" -> {
                Dynamic contents = dynamic.get("contents").orElseEmptyMap();
                yield TextComponentHoverAndClickEventFix.copyFields(dynamic.remove("contents"), contents, "id", "type", "name").renameField("id", "uuid").renameField("type", "id");
            }
            default -> dynamic;
        };
    }

    private static <T> @Nullable Dynamic<T> fixClickEvent(Dynamic<T> dynamic) {
        String action = dynamic.get("action").asString("");
        String value = dynamic.get("value").asString("");
        return switch (action) {
            case "open_url" -> {
                if (!TextComponentHoverAndClickEventFix.validateUri(value)) {
                    yield null;
                }
                yield dynamic.renameField("value", "url");
            }
            case "open_file" -> dynamic.renameField("value", "path");
            case "run_command", "suggest_command" -> {
                if (!TextComponentHoverAndClickEventFix.validateChat(value)) {
                    yield null;
                }
                yield dynamic.renameField("value", "command");
            }
            case "change_page" -> {
                Integer oldPage = dynamic.get("value").result().map(TextComponentHoverAndClickEventFix::parseOldPage).orElse(null);
                if (oldPage == null) {
                    yield null;
                }
                int page = Math.max(oldPage, 1);
                yield dynamic.remove("value").set("page", dynamic.createInt(page));
            }
            default -> dynamic;
        };
    }

    private static @Nullable Integer parseOldPage(Dynamic<?> value) {
        Optional numberValue = value.asNumber().result();
        if (numberValue.isPresent()) {
            return ((Number)numberValue.get()).intValue();
        }
        try {
            return Integer.parseInt(value.asString(""));
        }
        catch (Exception ignored) {
            return null;
        }
    }

    private static boolean validateUri(String uri) {
        try {
            URI parsedUri = new URI(uri);
            String scheme = parsedUri.getScheme();
            if (scheme == null) {
                return false;
            }
            String protocol = scheme.toLowerCase(Locale.ROOT);
            return "http".equals(protocol) || "https".equals(protocol);
        }
        catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean validateChat(String string) {
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (c != '\u00a7' && c >= ' ' && c != '\u007f') continue;
            return false;
        }
        return true;
    }
}

