/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.ImmutableStringReader
 *  com.mojang.brigadier.Message
 *  com.mojang.brigadier.StringReader
 *  com.mojang.brigadier.exceptions.CommandSyntaxException
 *  com.mojang.brigadier.exceptions.SimpleCommandExceptionType
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.DataResult
 *  io.netty.buffer.ByteBuf
 *  org.jspecify.annotations.Nullable
 */
package net.minecraft.resources;

import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.UnaryOperator;
import net.minecraft.IdentifierException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.Nullable;

public final class Identifier
implements Comparable<Identifier> {
    public static final Codec<Identifier> CODEC = Codec.STRING.comapFlatMap(Identifier::read, Identifier::toString).stable();
    public static final StreamCodec<ByteBuf, Identifier> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(Identifier::parse, Identifier::toString);
    public static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType((Message)Component.translatable("argument.id.invalid"));
    public static final char NAMESPACE_SEPARATOR = ':';
    public static final String DEFAULT_NAMESPACE = "minecraft";
    public static final String REALMS_NAMESPACE = "realms";
    public static final String ALLOWED_NAMESPACE_CHARACTERS = "[a-z0-9_.-]";
    private final String namespace;
    private final String path;

    private Identifier(String namespace, String path) {
        assert (Identifier.isValidNamespace(namespace));
        assert (Identifier.isValidPath(path));
        this.namespace = namespace;
        this.path = path;
    }

    private static Identifier createUntrusted(String namespace, String path) {
        return new Identifier(Identifier.assertValidNamespace(namespace, path), Identifier.assertValidPath(namespace, path));
    }

    public static Identifier fromNamespaceAndPath(String namespace, String path) {
        return Identifier.createUntrusted(namespace, path);
    }

    public static Identifier parse(String identifier) {
        return Identifier.bySeparator(identifier, ':');
    }

    public static Identifier withDefaultNamespace(String path) {
        return new Identifier(DEFAULT_NAMESPACE, Identifier.assertValidPath(DEFAULT_NAMESPACE, path));
    }

    public static @Nullable Identifier tryParse(String identifier) {
        return Identifier.tryBySeparator(identifier, ':');
    }

    public static @Nullable Identifier tryBuild(String namespace, String path) {
        if (Identifier.isValidNamespace(namespace) && Identifier.isValidPath(path)) {
            return new Identifier(namespace, path);
        }
        return null;
    }

    public static Identifier bySeparator(String identifier, char separator) {
        int separatorIndex = identifier.indexOf(separator);
        if (separatorIndex >= 0) {
            String path = identifier.substring(separatorIndex + 1);
            if (separatorIndex != 0) {
                String namespace = identifier.substring(0, separatorIndex);
                return Identifier.createUntrusted(namespace, path);
            }
            return Identifier.withDefaultNamespace(path);
        }
        return Identifier.withDefaultNamespace(identifier);
    }

    public static @Nullable Identifier tryBySeparator(String identifier, char separator) {
        int separatorIndex = identifier.indexOf(separator);
        if (separatorIndex >= 0) {
            String path = identifier.substring(separatorIndex + 1);
            if (!Identifier.isValidPath(path)) {
                return null;
            }
            if (separatorIndex != 0) {
                String namespace = identifier.substring(0, separatorIndex);
                return Identifier.isValidNamespace(namespace) ? new Identifier(namespace, path) : null;
            }
            return new Identifier(DEFAULT_NAMESPACE, path);
        }
        return Identifier.isValidPath(identifier) ? new Identifier(DEFAULT_NAMESPACE, identifier) : null;
    }

    public static DataResult<Identifier> read(String input) {
        try {
            return DataResult.success((Object)Identifier.parse(input));
        }
        catch (IdentifierException e) {
            return DataResult.error(() -> "Not a valid resource location: " + input + " " + e.getMessage());
        }
    }

    public String getPath() {
        return this.path;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public Identifier withPath(String newPath) {
        return new Identifier(this.namespace, Identifier.assertValidPath(this.namespace, newPath));
    }

    public Identifier withPath(UnaryOperator<String> modifier) {
        return this.withPath((String)modifier.apply(this.path));
    }

    public Identifier withPrefix(String prefix) {
        return this.withPath(prefix + this.path);
    }

    public Identifier withSuffix(String suffix) {
        return this.withPath(this.path + suffix);
    }

    public String toString() {
        return this.namespace + ":" + this.path;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Identifier) {
            Identifier that = (Identifier)o;
            return this.namespace.equals(that.namespace) && this.path.equals(that.path);
        }
        return false;
    }

    public int hashCode() {
        return 31 * this.namespace.hashCode() + this.path.hashCode();
    }

    @Override
    public int compareTo(Identifier o) {
        int result = this.path.compareTo(o.path);
        if (result == 0) {
            result = this.namespace.compareTo(o.namespace);
        }
        return result;
    }

    public Path resolveAgainst(Path root) {
        Path normalizedRoot;
        Path resultingPath = root.resolve(this.getNamespace(), this.getPath());
        Path normalizedPath = resultingPath.normalize();
        if (!normalizedPath.startsWith(normalizedRoot = root.normalize())) {
            throw new IllegalStateException(String.format(Locale.ROOT, "Identifier \"%s\" tried to access path \"%s\" from root \"%s\"", this, normalizedPath, normalizedRoot));
        }
        return resultingPath;
    }

    public String toDebugFileName() {
        return this.toString().replace('/', '_').replace(':', '_');
    }

    public String toLanguageKey() {
        return this.namespace + "." + this.path;
    }

    public String toShortLanguageKey() {
        return this.namespace.equals(DEFAULT_NAMESPACE) ? this.path : this.toLanguageKey();
    }

    public String toShortString() {
        return this.namespace.equals(DEFAULT_NAMESPACE) ? this.path : this.toString();
    }

    public String toLanguageKey(String prefix) {
        return prefix + "." + this.toLanguageKey();
    }

    public String toLanguageKey(String prefix, String suffix) {
        return prefix + "." + this.toLanguageKey() + "." + suffix;
    }

    private static String readGreedy(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && Identifier.isAllowedInIdentifier(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    public static Identifier read(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String raw = Identifier.readGreedy(reader);
        try {
            return Identifier.parse(raw);
        }
        catch (IdentifierException ex) {
            reader.setCursor(start);
            throw ERROR_INVALID.createWithContext((ImmutableStringReader)reader);
        }
    }

    public static Identifier readNonEmpty(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        String raw = Identifier.readGreedy(reader);
        if (raw.isEmpty()) {
            throw ERROR_INVALID.createWithContext((ImmutableStringReader)reader);
        }
        try {
            return Identifier.parse(raw);
        }
        catch (IdentifierException ex) {
            reader.setCursor(start);
            throw ERROR_INVALID.createWithContext((ImmutableStringReader)reader);
        }
    }

    public static boolean isAllowedInIdentifier(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c == '_' || c == ':' || c == '/' || c == '.' || c == '-';
    }

    public static boolean isValidPath(String path) {
        for (int i = 0; i < path.length(); ++i) {
            if (Identifier.validPathChar(path.charAt(i))) continue;
            return false;
        }
        return true;
    }

    public static boolean isValidNamespace(String namespace) {
        if (namespace.equals("..")) {
            return false;
        }
        for (int i = 0; i < namespace.length(); ++i) {
            if (Identifier.validNamespaceChar(namespace.charAt(i))) continue;
            return false;
        }
        return true;
    }

    private static String assertValidNamespace(String namespace, String path) {
        if (!Identifier.isValidNamespace(namespace)) {
            throw new IdentifierException("Non [a-z0-9_.-] character in namespace of identifier: " + namespace + ":" + path);
        }
        return namespace;
    }

    public static boolean validPathChar(char c) {
        return c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '/' || c == '.';
    }

    private static boolean validNamespaceChar(char c) {
        return c == '_' || c == '-' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '.';
    }

    private static String assertValidPath(String namespace, String path) {
        if (!Identifier.isValidPath(path)) {
            throw new IdentifierException("Non [a-z0-9/._-] character in path of location: " + namespace + ":" + path);
        }
        return path;
    }
}

