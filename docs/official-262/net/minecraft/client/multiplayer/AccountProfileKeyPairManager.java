/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Strings
 *  com.mojang.authlib.exceptions.MinecraftClientException
 *  com.mojang.authlib.minecraft.InsecurePublicKeyException$MissingException
 *  com.mojang.authlib.minecraft.UserApiService
 *  com.mojang.authlib.yggdrasil.response.KeyPairResponse
 *  com.mojang.authlib.yggdrasil.response.KeyPairResponse$KeyPair
 *  com.mojang.logging.LogUtils
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.client.multiplayer;

import com.google.common.base.Strings;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.InsecurePublicKeyException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.response.KeyPairResponse;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.PublicKey;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.multiplayer.ProfileKeyPairManager;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(value=EnvType.CLIENT)
public class AccountProfileKeyPairManager
implements ProfileKeyPairManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Duration MINIMUM_PROFILE_KEY_REFRESH_INTERVAL = Duration.ofHours(1L);
    private static final Path PROFILE_KEY_PAIR_DIR = Path.of("profilekeys", new String[0]);
    private final UserApiService userApiService;
    private final Path profileKeyPairPath;
    private CompletableFuture<Optional<ProfileKeyPair>> keyPair = CompletableFuture.completedFuture(Optional.empty());
    private Instant nextProfileKeyRefreshTime = Instant.EPOCH;

    public AccountProfileKeyPairManager(UserApiService userApiService, UUID profileId, Path gameDirectory) {
        this.userApiService = userApiService;
        this.profileKeyPairPath = gameDirectory.resolve(PROFILE_KEY_PAIR_DIR).resolve(String.valueOf(profileId) + ".json");
    }

    @Override
    public CompletableFuture<Optional<ProfileKeyPair>> prepareKeyPair() {
        this.nextProfileKeyRefreshTime = Instant.now().plus(MINIMUM_PROFILE_KEY_REFRESH_INTERVAL);
        this.keyPair = this.keyPair.thenCompose(this::readOrFetchProfileKeyPair);
        return this.keyPair;
    }

    @Override
    public boolean shouldRefreshKeyPair() {
        if (this.keyPair.isDone() && Instant.now().isAfter(this.nextProfileKeyRefreshTime)) {
            return this.keyPair.join().map(ProfileKeyPair::dueRefresh).orElse(true);
        }
        return false;
    }

    private CompletableFuture<Optional<ProfileKeyPair>> readOrFetchProfileKeyPair(Optional<ProfileKeyPair> cachedKeyPair) {
        return CompletableFuture.supplyAsync(() -> {
            if (cachedKeyPair.isPresent() && !((ProfileKeyPair)cachedKeyPair.get()).dueRefresh()) {
                if (!SharedConstants.IS_RUNNING_IN_IDE) {
                    this.writeProfileKeyPair(null);
                }
                return cachedKeyPair;
            }
            try {
                ProfileKeyPair fetchedKeyPair = this.fetchProfileKeyPair(this.userApiService);
                this.writeProfileKeyPair(fetchedKeyPair);
                return Optional.ofNullable(fetchedKeyPair);
            }
            catch (MinecraftClientException | IOException | CryptException e) {
                LOGGER.error("Failed to retrieve profile key pair", e);
                this.writeProfileKeyPair(null);
                return cachedKeyPair;
            }
        }, Util.nonCriticalIoPool());
    }

    private Optional<ProfileKeyPair> readProfileKeyPair() {
        Optional optional;
        block9: {
            if (Files.notExists(this.profileKeyPairPath, new LinkOption[0])) {
                return Optional.empty();
            }
            BufferedReader bufferedReader = Files.newBufferedReader(this.profileKeyPairPath);
            try {
                optional = ProfileKeyPair.CODEC.parse((DynamicOps)JsonOps.INSTANCE, (Object)StrictJsonParser.parse(bufferedReader)).result();
                if (bufferedReader == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (bufferedReader != null) {
                        try {
                            bufferedReader.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    LOGGER.error("Failed to read profile key pair file {}", (Object)this.profileKeyPairPath, (Object)e);
                    return Optional.empty();
                }
            }
            bufferedReader.close();
        }
        return optional;
    }

    private void writeProfileKeyPair(@Nullable ProfileKeyPair profileKeyPair) {
        try {
            Files.deleteIfExists(this.profileKeyPairPath);
        }
        catch (IOException e) {
            LOGGER.error("Failed to delete profile key pair file {}", (Object)this.profileKeyPairPath, (Object)e);
        }
        if (profileKeyPair == null) {
            return;
        }
        if (!SharedConstants.IS_RUNNING_IN_IDE) {
            return;
        }
        ProfileKeyPair.CODEC.encodeStart((DynamicOps)JsonOps.INSTANCE, (Object)profileKeyPair).ifSuccess(jsonStr -> {
            try {
                Files.createDirectories(this.profileKeyPairPath.getParent(), new FileAttribute[0]);
                Files.writeString(this.profileKeyPairPath, (CharSequence)jsonStr.toString(), new OpenOption[0]);
            }
            catch (Exception e) {
                LOGGER.error("Failed to write profile key pair file {}", (Object)this.profileKeyPairPath, (Object)e);
            }
        });
    }

    private @Nullable ProfileKeyPair fetchProfileKeyPair(UserApiService userApiService) throws CryptException, IOException {
        KeyPairResponse keyPair = userApiService.getKeyPair();
        if (keyPair != null) {
            ProfilePublicKey.Data publicKeyData = AccountProfileKeyPairManager.parsePublicKey(keyPair);
            return new ProfileKeyPair(Crypt.stringToPemRsaPrivateKey(keyPair.keyPair().privateKey()), new ProfilePublicKey(publicKeyData), Instant.parse(keyPair.refreshedAfter()));
        }
        return null;
    }

    private static ProfilePublicKey.Data parsePublicKey(KeyPairResponse response) throws CryptException {
        KeyPairResponse.KeyPair keyPair = response.keyPair();
        if (keyPair == null || Strings.isNullOrEmpty((String)keyPair.publicKey()) || response.publicKeySignature() == null || response.publicKeySignature().array().length == 0) {
            throw new CryptException((Throwable)new InsecurePublicKeyException.MissingException("Missing public key"));
        }
        try {
            Instant expiresAt = Instant.parse(response.expiresAt());
            PublicKey key = Crypt.stringToRsaPublicKey(keyPair.publicKey());
            ByteBuffer signature = response.publicKeySignature();
            return new ProfilePublicKey.Data(expiresAt, key, signature.array());
        }
        catch (IllegalArgumentException | DateTimeException e) {
            throw new CryptException(e);
        }
    }
}

