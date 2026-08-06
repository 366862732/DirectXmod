/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.serialization.Codec
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package net.minecraft.client.gui.font;

import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.StringRepresentable;

@Environment(value=EnvType.CLIENT)
public enum FontOption implements StringRepresentable
{
    UNIFORM("uniform"),
    JAPANESE_VARIANTS("jp");

    public static final Codec<FontOption> CODEC;
    private final String name;

    private FontOption(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static {
        CODEC = StringRepresentable.fromEnum(FontOption::values);
    }

    @Environment(value=EnvType.CLIENT)
    public static class Filter {
        private final Map<FontOption, Boolean> values;
        public static final Codec<Filter> CODEC = Codec.unboundedMap(CODEC, (Codec)Codec.BOOL).xmap(Filter::new, p -> p.values);
        public static final Filter ALWAYS_PASS = new Filter(Map.of());

        public Filter(Map<FontOption, Boolean> values) {
            this.values = values;
        }

        public boolean apply(Set<FontOption> options) {
            for (Map.Entry<FontOption, Boolean> e : this.values.entrySet()) {
                if (options.contains(e.getKey()) == e.getValue().booleanValue()) continue;
                return false;
            }
            return true;
        }

        public Filter merge(Filter other) {
            HashMap<FontOption, Boolean> options = new HashMap<FontOption, Boolean>(other.values);
            options.putAll(this.values);
            return new Filter(Map.copyOf(options));
        }
    }
}

