/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonDeserializer
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.resources.model.cuboid;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;

@Environment(value=EnvType.CLIENT)
public record ItemTransforms(ItemTransform thirdPersonLeftHand, ItemTransform thirdPersonRightHand, ItemTransform firstPersonLeftHand, ItemTransform firstPersonRightHand, ItemTransform head, ItemTransform gui, ItemTransform ground, ItemTransform fixed, ItemTransform fixedFromBottom) {
    public static final ItemTransforms NO_TRANSFORMS = new ItemTransforms(ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM, ItemTransform.NO_TRANSFORM);

    public ItemTransform getTransform(ItemDisplayContext type) {
        return switch (type) {
            case ItemDisplayContext.THIRD_PERSON_LEFT_HAND -> this.thirdPersonLeftHand;
            case ItemDisplayContext.THIRD_PERSON_RIGHT_HAND -> this.thirdPersonRightHand;
            case ItemDisplayContext.FIRST_PERSON_LEFT_HAND -> this.firstPersonLeftHand;
            case ItemDisplayContext.FIRST_PERSON_RIGHT_HAND -> this.firstPersonRightHand;
            case ItemDisplayContext.HEAD -> this.head;
            case ItemDisplayContext.GUI -> this.gui;
            case ItemDisplayContext.GROUND -> this.ground;
            case ItemDisplayContext.FIXED -> this.fixed;
            case ItemDisplayContext.ON_SHELF -> this.fixedFromBottom;
            default -> ItemTransform.NO_TRANSFORM;
        };
    }

    @Environment(value=EnvType.CLIENT)
    protected static class Deserializer
    implements JsonDeserializer<ItemTransforms> {
        protected Deserializer() {
        }

        public ItemTransforms deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            ItemTransform thirdPersonRightHand = this.getTransform(context, object, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
            ItemTransform thirdPersonLeftHand = this.getTransform(context, object, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
            if (thirdPersonLeftHand == ItemTransform.NO_TRANSFORM) {
                thirdPersonLeftHand = thirdPersonRightHand;
            }
            ItemTransform firstPersonRightHand = this.getTransform(context, object, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
            ItemTransform firstPersonLeftHand = this.getTransform(context, object, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
            if (firstPersonLeftHand == ItemTransform.NO_TRANSFORM) {
                firstPersonLeftHand = firstPersonRightHand;
            }
            ItemTransform head = this.getTransform(context, object, ItemDisplayContext.HEAD);
            ItemTransform gui = this.getTransform(context, object, ItemDisplayContext.GUI);
            ItemTransform ground = this.getTransform(context, object, ItemDisplayContext.GROUND);
            ItemTransform fixed = this.getTransform(context, object, ItemDisplayContext.FIXED);
            ItemTransform fixedFromBottom = this.getTransform(context, object, ItemDisplayContext.ON_SHELF);
            return new ItemTransforms(thirdPersonLeftHand, thirdPersonRightHand, firstPersonLeftHand, firstPersonRightHand, head, gui, ground, fixed, fixedFromBottom);
        }

        private ItemTransform getTransform(JsonDeserializationContext context, JsonObject object, ItemDisplayContext transform) {
            String name = transform.getSerializedName();
            if (object.has(name)) {
                return (ItemTransform)context.deserialize(object.get(name), ItemTransform.class);
            }
            return ItemTransform.NO_TRANSFORM;
        }
    }
}

