/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Splitter
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.client.renderer.block.dispatch;

import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

@Environment(value=EnvType.CLIENT)
public class VariantSelector {
    private static final Splitter COMMA_SPLITTER = Splitter.on((char)',');
    private static final Splitter EQUAL_SPLITTER = Splitter.on((char)'=').limit(2);

    public static <O, S extends StateHolder<O, S>> Predicate<StateHolder<O, S>> predicate(StateDefinition<O, S> stateDefinition, String properties) {
        HashMap map = new HashMap();
        for (String keyValue : COMMA_SPLITTER.split((CharSequence)properties)) {
            Iterator iterator = EQUAL_SPLITTER.split((CharSequence)keyValue).iterator();
            if (!iterator.hasNext()) continue;
            String propertyName = (String)iterator.next();
            Property<?> property = stateDefinition.getProperty(propertyName);
            if (property != null && iterator.hasNext()) {
                String propertyValue = (String)iterator.next();
                Object value = VariantSelector.getValueHelper(property, propertyValue);
                if (value != null) {
                    map.put(property, value);
                    continue;
                }
                throw new RuntimeException("Unknown value: '" + propertyValue + "' for blockstate property: '" + propertyName + "' " + String.valueOf(property.getPossibleValues()));
            }
            if (propertyName.isEmpty()) continue;
            throw new RuntimeException("Unknown blockstate property: '" + propertyName + "'");
        }
        return input -> {
            for (Map.Entry entry : map.entrySet()) {
                if (Objects.equals(input.getValue((Property)entry.getKey()), entry.getValue())) continue;
                return false;
            }
            return true;
        };
    }

    private static <T extends Comparable<T>> @Nullable T getValueHelper(Property<T> property, String next) {
        return (T)((Comparable)property.getValue(next).orElse(null));
    }
}

