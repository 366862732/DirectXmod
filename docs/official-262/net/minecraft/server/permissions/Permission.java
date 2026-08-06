/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.datafixers.util.Either
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.permissions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.PermissionLevel;

public interface Permission {
    public static final Codec<Permission> FULL_CODEC = BuiltInRegistries.PERMISSION_TYPE.byNameCodec().dispatch(Permission::codec, c -> c);
    public static final Codec<Permission> CODEC = Codec.either(FULL_CODEC, Identifier.CODEC).xmap(e -> (Permission)e.map(permission -> permission, Atom::create), permission -> {
        Either either;
        if (permission instanceof Atom) {
            Atom atom = (Atom)permission;
            either = Either.right((Object)atom.id());
        } else {
            either = Either.left((Object)permission);
        }
        return either;
    });

    public MapCodec<? extends Permission> codec();

    public record Atom(Identifier id) implements Permission
    {
        public static final MapCodec<Atom> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Identifier.CODEC.fieldOf("id").forGetter(Atom::id)).apply((Applicative)i, Atom::new));

        public MapCodec<Atom> codec() {
            return MAP_CODEC;
        }

        public static Atom create(String name) {
            return Atom.create(Identifier.withDefaultNamespace(name));
        }

        public static Atom create(Identifier id) {
            return new Atom(id);
        }
    }

    public record HasCommandLevel(PermissionLevel level) implements Permission
    {
        public static final MapCodec<HasCommandLevel> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)PermissionLevel.CODEC.fieldOf("level").forGetter(HasCommandLevel::level)).apply((Applicative)i, HasCommandLevel::new));

        public MapCodec<HasCommandLevel> codec() {
            return MAP_CODEC;
        }
    }
}

