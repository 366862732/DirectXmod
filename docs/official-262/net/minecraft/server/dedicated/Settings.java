/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.MoreObjects
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.server.dedicated;

import com.google.common.base.MoreObjects;
import com.mojang.logging.LogUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.minecraft.core.RegistryAccess;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class Settings<T extends Settings<T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final Properties properties;

    public Settings(Properties properties) {
        this.properties = properties;
    }

    public static Properties loadFromFile(Path file) {
        Properties properties;
        block16: {
            InputStream is = Files.newInputStream(file, new OpenOption[0]);
            try {
                CharsetDecoder reportingUtf8Decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
                Properties properties2 = new Properties();
                properties2.load(new InputStreamReader(is, reportingUtf8Decoder));
                properties = properties2;
                if (is == null) break block16;
            }
            catch (Throwable reportingUtf8Decoder) {
                try {
                    if (is != null) {
                        try {
                            is.close();
                        }
                        catch (Throwable properties2) {
                            reportingUtf8Decoder.addSuppressed(properties2);
                        }
                    }
                    throw reportingUtf8Decoder;
                }
                catch (CharacterCodingException e) {
                    Properties properties3;
                    block17: {
                        LOGGER.info("Failed to load properties as UTF-8 from file {}, trying ISO_8859_1", (Object)file);
                        BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1);
                        try {
                            Properties properties4 = new Properties();
                            properties4.load(reader);
                            properties3 = properties4;
                            if (reader == null) break block17;
                        }
                        catch (Throwable throwable) {
                            try {
                                if (reader != null) {
                                    try {
                                        ((Reader)reader).close();
                                    }
                                    catch (Throwable throwable2) {
                                        throwable.addSuppressed(throwable2);
                                    }
                                }
                                throw throwable;
                            }
                            catch (IOException e2) {
                                LOGGER.error("Failed to load properties from file: {}", (Object)file, (Object)e2);
                                return new Properties();
                            }
                        }
                        ((Reader)reader).close();
                    }
                    return properties3;
                }
            }
            is.close();
        }
        return properties;
    }

    public void store(Path output) {
        try (BufferedWriter os = Files.newBufferedWriter(output, StandardCharsets.UTF_8, new OpenOption[0]);){
            this.properties.store(os, "Minecraft server properties");
        }
        catch (IOException e) {
            LOGGER.error("Failed to store properties to file: {}", (Object)output);
        }
    }

    private static <V extends Number> Function<String, @Nullable V> wrapNumberDeserializer(Function<String, V> inner) {
        return s -> {
            try {
                return (Number)inner.apply((String)s);
            }
            catch (NumberFormatException e) {
                return null;
            }
        };
    }

    protected static <V> Function<String, @Nullable V> dispatchNumberOrString(IntFunction<@Nullable V> intDeserializer, Function<String, @Nullable V> stringDeserializer) {
        return s -> {
            try {
                return intDeserializer.apply(Integer.parseInt(s));
            }
            catch (NumberFormatException e) {
                return stringDeserializer.apply((String)s);
            }
        };
    }

    private @Nullable String getStringRaw(String key) {
        return (String)this.properties.get(key);
    }

    protected <V> @Nullable V getLegacy(String key, Function<String, V> deserializer) {
        String value = this.getStringRaw(key);
        if (value == null) {
            return null;
        }
        this.properties.remove(key);
        return deserializer.apply(value);
    }

    protected <V> V get(String key, Function<String, @Nullable V> deserializer, Function<V, String> serializer, V defaultValue) {
        String value = this.getStringRaw(key);
        Object result = MoreObjects.firstNonNull(value != null ? deserializer.apply(value) : null, defaultValue);
        this.properties.put(key, serializer.apply(result));
        return (V)result;
    }

    protected <V> MutableValue<V> getMutable(String key, Function<String, @Nullable V> deserializer, Function<V, String> serializer, V defaultValue) {
        String value = this.getStringRaw(key);
        Object result = MoreObjects.firstNonNull(value != null ? deserializer.apply(value) : null, defaultValue);
        this.properties.put(key, serializer.apply(result));
        return new MutableValue<Object>(this, key, result, serializer);
    }

    protected <V> V get(String key, Function<String, @Nullable V> deserializer, UnaryOperator<V> validator, Function<V, String> serializer, V defaultValue) {
        return (V)this.get(key, s -> {
            Object result = deserializer.apply((String)s);
            return result != null ? validator.apply(result) : null;
        }, serializer, defaultValue);
    }

    protected <V> V get(String key, Function<String, V> deserializer, V defaultValue) {
        return (V)this.get(key, deserializer, Objects::toString, defaultValue);
    }

    protected <V> MutableValue<V> getMutable(String key, Function<String, V> deserializer, V defaultValue) {
        return this.getMutable(key, deserializer, Objects::toString, defaultValue);
    }

    protected String get(String key, String defaultValue) {
        return this.get(key, Function.identity(), Function.identity(), defaultValue);
    }

    protected @Nullable String getLegacyString(String key) {
        return (String)this.getLegacy(key, Function.identity());
    }

    protected int get(String key, int defaultValue) {
        return this.get(key, Settings.wrapNumberDeserializer(Integer::parseInt), Integer.valueOf(defaultValue));
    }

    protected MutableValue<Integer> getMutable(String key, int defaultValue) {
        return this.getMutable(key, Settings.wrapNumberDeserializer(Integer::parseInt), defaultValue);
    }

    protected MutableValue<String> getMutable(String key, String defaultValue) {
        return this.getMutable(key, String::new, defaultValue);
    }

    protected int get(String key, UnaryOperator<Integer> validator, int defaultValue) {
        return this.get(key, Settings.wrapNumberDeserializer(Integer::parseInt), validator, Objects::toString, defaultValue);
    }

    protected long get(String key, long defaultValue) {
        return this.get(key, Settings.wrapNumberDeserializer(Long::parseLong), defaultValue);
    }

    protected boolean get(String key, boolean defaultValue) {
        return this.get(key, Boolean::valueOf, defaultValue);
    }

    protected MutableValue<Boolean> getMutable(String key, boolean defaultValue) {
        return this.getMutable(key, Boolean::valueOf, defaultValue);
    }

    protected @Nullable Boolean getLegacyBoolean(String key) {
        return this.getLegacy(key, Boolean::valueOf);
    }

    protected Properties cloneProperties() {
        Properties result = new Properties();
        result.putAll((Map<?, ?>)this.properties);
        return result;
    }

    protected abstract T reload(RegistryAccess var1, Properties var2);

    public class MutableValue<V>
    implements Supplier<V> {
        private final String key;
        private final V value;
        private final Function<V, String> serializer;
        final /* synthetic */ Settings this$0;

        private MutableValue(Settings this$0, String key, V value, Function<V, String> serializer) {
            Settings settings = this$0;
            Objects.requireNonNull(settings);
            this.this$0 = settings;
            this.key = key;
            this.value = value;
            this.serializer = serializer;
        }

        @Override
        public V get() {
            return this.value;
        }

        public T update(RegistryAccess registryAccess, V value) {
            Properties properties = this.this$0.cloneProperties();
            properties.put(this.key, this.serializer.apply(value));
            return this.this$0.reload(registryAccess, properties);
        }
    }
}

