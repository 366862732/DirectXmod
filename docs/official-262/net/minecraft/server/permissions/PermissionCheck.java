/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.datafixers.kinds.App
 *  com.mojang.datafixers.kinds.Applicative
 *  com.mojang.serialization.Codec
 *  com.mojang.serialization.MapCodec
 *  com.mojang.serialization.codecs.RecordCodecBuilder
 */
package net.minecraft.server.permissions;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionSet;

public interface PermissionCheck {
    public static final Codec<PermissionCheck> CODEC = BuiltInRegistries.PERMISSION_CHECK_TYPE.byNameCodec().dispatch(PermissionCheck::codec, c -> c);

    public boolean check(PermissionSet var1);

    public MapCodec<? extends PermissionCheck> codec();

    public record Require(Permission permission) implements PermissionCheck
    {
        public static final MapCodec<Require> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group((App)Permission.CODEC.fieldOf("permission").forGetter(Require::permission)).apply((Applicative)i, Require::new));

        public MapCodec<Require> codec() {
            return MAP_CODEC;
        }

        @Override
        public boolean check(PermissionSet source) {
            return source.hasPermission(this.permission);
        }
    }

    public static class AlwaysPass
    implements PermissionCheck {
        public static final AlwaysPass INSTANCE = new AlwaysPass();
        public static final MapCodec<AlwaysPass> MAP_CODEC = MapCodec.unit((Object)INSTANCE);

        private AlwaysPass() {
        }

        @Override
        public boolean check(PermissionSet source) {
            return true;
        }

        public MapCodec<AlwaysPass> codec() {
            return MAP_CODEC;
        }
    }
}

