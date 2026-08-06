/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Sets
 *  com.google.common.collect.Sets$SetView
 *  org.jetbrains.annotations.Contract
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.util.context;

import com.google.common.collect.Sets;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import net.minecraft.util.context.ContextKey;
import net.minecraft.util.context.ContextKeySet;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class ContextMap {
    private final Map<ContextKey<?>, Object> params;

    private ContextMap(Map<ContextKey<?>, Object> params) {
        this.params = params;
    }

    public boolean has(ContextKey<?> key) {
        return this.params.containsKey(key);
    }

    public <T> T getOrThrow(ContextKey<T> key) {
        Object value = this.params.get(key);
        if (value == null) {
            throw new NoSuchElementException(key.name().toString());
        }
        return (T)value;
    }

    public <T> @Nullable T getOptional(ContextKey<T> key) {
        return (T)this.params.get(key);
    }

    @Contract(value="_,!null->!null; _,_->_")
    public <T> @Nullable T getOrDefault(ContextKey<T> param, @Nullable T _default) {
        return (T)this.params.getOrDefault(param, _default);
    }

    public static class Builder {
        private final Map<ContextKey<?>, Object> params = new IdentityHashMap();

        public <T> Builder withParameter(ContextKey<T> param, T value) {
            this.params.put(param, value);
            return this;
        }

        public <T> Builder withOptionalParameter(ContextKey<T> param, @Nullable T value) {
            if (value == null) {
                this.params.remove(param);
            } else {
                this.params.put(param, value);
            }
            return this;
        }

        public <T> T getParameter(ContextKey<T> param) {
            Object value = this.params.get(param);
            if (value == null) {
                throw new NoSuchElementException(param.name().toString());
            }
            return (T)value;
        }

        public <T> @Nullable T getOptionalParameter(ContextKey<T> param) {
            return (T)this.params.get(param);
        }

        public ContextMap create(ContextKeySet paramSet) {
            Sets.SetView notAllowed = Sets.difference(this.params.keySet(), paramSet.allowed());
            if (!notAllowed.isEmpty()) {
                throw new IllegalArgumentException("Parameters not allowed in this parameter set: " + String.valueOf(notAllowed));
            }
            Sets.SetView missingRequired = Sets.difference(paramSet.required(), this.params.keySet());
            if (!missingRequired.isEmpty()) {
                throw new IllegalArgumentException("Missing required parameters: " + String.valueOf(missingRequired));
            }
            return new ContextMap(this.params);
        }
    }
}

