/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.annotations.VisibleForTesting
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonDeserializer
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.model.cuboid;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record CuboidModel(@Nullable UnbakedGeometry geometry, @Nullable UnbakedModel.GuiLight guiLight, @Nullable Boolean ambientOcclusion, @Nullable ItemTransforms transforms, TextureSlots.Data textureSlots, @Nullable Identifier parent) implements UnbakedModel
{
    @VisibleForTesting
    static final Gson GSON = new GsonBuilder().registerTypeAdapter(CuboidModel.class, (Object)new Deserializer()).registerTypeAdapter(CuboidModelElement.class, (Object)new CuboidModelElement.Deserializer()).registerTypeAdapter(CuboidFace.class, (Object)new CuboidFace.Deserializer()).registerTypeAdapter(ItemTransform.class, (Object)new ItemTransform.Deserializer()).registerTypeAdapter(ItemTransforms.class, (Object)new ItemTransforms.Deserializer()).create();

    public static CuboidModel fromStream(Reader reader) {
        return GsonHelper.fromJson(GSON, reader, CuboidModel.class);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Deserializer
    implements JsonDeserializer<CuboidModel> {
        public CuboidModel deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            UnbakedGeometry elements = this.getElements(context, object);
            String parentName = this.getParentName(object);
            TextureSlots.Data textureMap = this.getTextureMap(object);
            Boolean hasAmbientOcclusion = this.getAmbientOcclusion(object);
            ItemTransforms transforms = null;
            if (object.has("display")) {
                JsonObject display = GsonHelper.getAsJsonObject(object, "display");
                transforms = (ItemTransforms)context.deserialize((JsonElement)display, ItemTransforms.class);
            }
            UnbakedModel.GuiLight guiLight = null;
            if (object.has("gui_light")) {
                guiLight = UnbakedModel.GuiLight.getByName(GsonHelper.getAsString(object, "gui_light"));
            }
            Identifier parentLocation = parentName.isEmpty() ? null : Identifier.parse(parentName);
            return new CuboidModel(elements, guiLight, hasAmbientOcclusion, transforms, textureMap, parentLocation);
        }

        private TextureSlots.Data getTextureMap(JsonObject object) {
            if (object.has("textures")) {
                JsonObject texturesObject = GsonHelper.getAsJsonObject(object, "textures");
                return TextureSlots.parseTextureMap(texturesObject);
            }
            return TextureSlots.Data.EMPTY;
        }

        private String getParentName(JsonObject object) {
            return GsonHelper.getAsString(object, "parent", "");
        }

        protected @Nullable Boolean getAmbientOcclusion(JsonObject object) {
            if (object.has("ambientocclusion")) {
                return GsonHelper.getAsBoolean(object, "ambientocclusion");
            }
            return null;
        }

        protected @Nullable UnbakedGeometry getElements(JsonDeserializationContext context, JsonObject object) {
            if (object.has("elements")) {
                ArrayList<CuboidModelElement> elements = new ArrayList<CuboidModelElement>();
                for (JsonElement element : GsonHelper.getAsJsonArray(object, "elements")) {
                    elements.add((CuboidModelElement)context.deserialize(element, CuboidModelElement.class));
                }
                return new UnbakedCuboidGeometry(elements);
            }
            return null;
        }
    }
}

