/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Maps
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonDeserializationContext
 *  com.google.gson.JsonDeserializer
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParseException
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.resources.model.cuboid;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public record CuboidModelElement(Vector3fc from, Vector3fc to, Map<Direction, CuboidFace> faces, @Nullable CuboidRotation rotation, boolean shade, int lightEmission) {
    private static final boolean DEFAULT_RESCALE = false;
    private static final float MIN_EXTENT = -16.0f;
    private static final float MAX_EXTENT = 32.0f;

    public CuboidModelElement(Vector3fc from, Vector3fc to, Map<Direction, CuboidFace> faces) {
        this(from, to, faces, null, true, 0);
    }

    @Environment(value=EnvType.CLIENT)
    protected static class Deserializer
    implements JsonDeserializer<CuboidModelElement> {
        private static final boolean DEFAULT_SHADE = true;
        private static final int DEFAULT_LIGHT_EMISSION = 0;
        private static final String FIELD_SHADE = "shade";
        private static final String FIELD_LIGHT_EMISSION = "light_emission";
        private static final String FIELD_ROTATION = "rotation";
        private static final String FIELD_ORIGIN = "origin";
        private static final String FIELD_ANGLE = "angle";
        private static final String FIELD_X = "x";
        private static final String FIELD_Y = "y";
        private static final String FIELD_Z = "z";
        private static final String FIELD_AXIS = "axis";
        private static final String FIELD_RESCALE = "rescale";
        private static final String FIELD_FACES = "faces";
        private static final String FIELD_TO = "to";
        private static final String FIELD_FROM = "from";

        protected Deserializer() {
        }

        public CuboidModelElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = json.getAsJsonObject();
            Vector3f from = Deserializer.getPosition(object, FIELD_FROM);
            Vector3f to = Deserializer.getPosition(object, FIELD_TO);
            CuboidRotation rotation = this.getRotation(object);
            Map<Direction, CuboidFace> faces = this.getFaces(context, object);
            if (object.has(FIELD_SHADE) && !GsonHelper.isBooleanValue(object, FIELD_SHADE)) {
                throw new JsonParseException("Expected 'shade' to be a Boolean");
            }
            boolean shade = GsonHelper.getAsBoolean(object, FIELD_SHADE, true);
            int lightEmission = 0;
            if (object.has(FIELD_LIGHT_EMISSION)) {
                boolean isNumber = GsonHelper.isNumberValue(object, FIELD_LIGHT_EMISSION);
                if (isNumber) {
                    lightEmission = GsonHelper.getAsInt(object, FIELD_LIGHT_EMISSION);
                }
                if (!isNumber || lightEmission < 0 || lightEmission > 15) {
                    throw new JsonParseException("Expected 'light_emission' to be an Integer between (inclusive) 0 and 15");
                }
            }
            return new CuboidModelElement((Vector3fc)from, (Vector3fc)to, faces, rotation, shade, lightEmission);
        }

        private @Nullable CuboidRotation getRotation(JsonObject object) {
            if (object.has(FIELD_ROTATION)) {
                Record rotationValue;
                JsonObject rotationObject = GsonHelper.getAsJsonObject(object, FIELD_ROTATION);
                Vector3f origin = Deserializer.getVector3f(rotationObject, FIELD_ORIGIN);
                origin.mul(0.0625f);
                if (rotationObject.has(FIELD_AXIS) || rotationObject.has(FIELD_ANGLE)) {
                    Direction.Axis axis = this.getAxis(rotationObject);
                    float angle = GsonHelper.getAsFloat(rotationObject, FIELD_ANGLE);
                    rotationValue = new CuboidRotation.SingleAxisRotation(axis, angle);
                } else if (rotationObject.has(FIELD_X) || rotationObject.has(FIELD_Y) || rotationObject.has(FIELD_Z)) {
                    float x = GsonHelper.getAsFloat(rotationObject, FIELD_X, 0.0f);
                    float y = GsonHelper.getAsFloat(rotationObject, FIELD_Y, 0.0f);
                    float z = GsonHelper.getAsFloat(rotationObject, FIELD_Z, 0.0f);
                    rotationValue = new CuboidRotation.EulerXYZRotation(x, y, z);
                } else {
                    throw new JsonParseException("Missing rotation value, expected either 'axis' and 'angle' or 'x', 'y' and 'z'");
                }
                boolean rescale = GsonHelper.getAsBoolean(rotationObject, FIELD_RESCALE, false);
                return new CuboidRotation((Vector3fc)origin, (CuboidRotation.RotationValue)((Object)rotationValue), rescale);
            }
            return null;
        }

        private Direction.Axis getAxis(JsonObject object) {
            String axisName = GsonHelper.getAsString(object, FIELD_AXIS);
            Direction.Axis axis = Direction.Axis.byName(axisName.toLowerCase(Locale.ROOT));
            if (axis == null) {
                throw new JsonParseException("Invalid rotation axis: " + axisName);
            }
            return axis;
        }

        private Map<Direction, CuboidFace> getFaces(JsonDeserializationContext context, JsonObject object) {
            Map<Direction, CuboidFace> faces = this.filterNullFromFaces(context, object);
            if (faces.isEmpty()) {
                throw new JsonParseException("Expected between 1 and 6 unique faces, got 0");
            }
            return faces;
        }

        private Map<Direction, CuboidFace> filterNullFromFaces(JsonDeserializationContext context, JsonObject object) {
            EnumMap result = Maps.newEnumMap(Direction.class);
            JsonObject faceObjects = GsonHelper.getAsJsonObject(object, FIELD_FACES);
            for (Map.Entry entry : faceObjects.entrySet()) {
                Direction direction = this.getFacing((String)entry.getKey());
                result.put(direction, (CuboidFace)context.deserialize((JsonElement)entry.getValue(), CuboidFace.class));
            }
            return result;
        }

        private Direction getFacing(String name) {
            Direction direction = Direction.byName(name);
            if (direction == null) {
                throw new JsonParseException("Unknown facing: " + name);
            }
            return direction;
        }

        private static Vector3f getPosition(JsonObject object, String key) {
            Vector3f from = Deserializer.getVector3f(object, key);
            if (from.x() < -16.0f || from.y() < -16.0f || from.z() < -16.0f || from.x() > 32.0f || from.y() > 32.0f || from.z() > 32.0f) {
                throw new JsonParseException("'" + key + "' specifier exceeds the allowed boundaries: " + String.valueOf(from));
            }
            return from;
        }

        private static Vector3f getVector3f(JsonObject object, String key) {
            JsonArray vecArray = GsonHelper.getAsJsonArray(object, key);
            if (vecArray.size() != 3) {
                throw new JsonParseException("Expected 3 " + key + " values, found: " + vecArray.size());
            }
            float[] elements = new float[3];
            for (int i = 0; i < elements.length; ++i) {
                elements[i] = GsonHelper.convertToFloat(vecArray.get(i), key + "[" + i + "]");
            }
            return new Vector3f(elements[0], elements[1], elements[2]);
        }
    }
}

