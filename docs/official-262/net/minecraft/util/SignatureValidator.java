/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.yggdrasil.ServicesKeySet
 *  com.mojang.authlib.yggdrasil.ServicesKeyType
 *  com.mojang.logging.LogUtils
 *  org.jspecify.annotations.Nullable
 *  org.slf4j.Logger
 */
package net.minecraft.util;

import com.mojang.authlib.yggdrasil.ServicesKeySet;
import com.mojang.authlib.yggdrasil.ServicesKeyType;
import com.mojang.logging.LogUtils;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Collection;
import net.minecraft.util.SignatureUpdater;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public interface SignatureValidator {
    public static final SignatureValidator NO_VALIDATION = (payload, signature) -> true;
    public static final Logger LOGGER = LogUtils.getLogger();

    public boolean validate(SignatureUpdater var1, byte[] var2);

    default public boolean validate(byte[] payload, byte[] signature) {
        return this.validate(output -> output.update(payload), signature);
    }

    private static boolean verifySignature(SignatureUpdater updater, byte[] signature, Signature verifier) throws SignatureException {
        updater.update(verifier::update);
        return verifier.verify(signature);
    }

    public static SignatureValidator from(PublicKey publicKey, String algorithm) {
        return (updater, signature) -> {
            try {
                Signature verifier = Signature.getInstance(algorithm);
                verifier.initVerify(publicKey);
                return SignatureValidator.verifySignature(updater, signature, verifier);
            }
            catch (Exception e) {
                LOGGER.error("Failed to verify signature", (Throwable)e);
                return false;
            }
        };
    }

    public static @Nullable SignatureValidator from(ServicesKeySet keySet, ServicesKeyType type) {
        Collection keys = keySet.keys(type);
        if (keys.isEmpty()) {
            return null;
        }
        return (updater, signature) -> keys.stream().anyMatch(key -> {
            Signature verifier = key.signature();
            try {
                return SignatureValidator.verifySignature(updater, signature, verifier);
            }
            catch (SignatureException e) {
                LOGGER.error("Failed to verify Services signature", (Throwable)e);
                return false;
            }
        });
    }
}

