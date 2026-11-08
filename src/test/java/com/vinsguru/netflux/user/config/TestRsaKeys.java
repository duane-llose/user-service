package com.vinsguru.netflux.user.config;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.security.KeyFactory;
import java.util.Base64;

/**
 * Test helpers for RSA keys.
 */
public final class TestRsaKeys {

    private TestRsaKeys() {
    }

    /** Generates a new 2048-bit RSA private key encoded as PKCS#8 PEM ("-----BEGIN PRIVATE KEY-----"). */
    public static String generatePrivateKeyPem() {
        try {
            var generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var encoded = generator.generateKeyPair().getPrivate().getEncoded();
            var body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(encoded);
            return "-----BEGIN PRIVATE KEY-----\n" + body + "\n-----END PRIVATE KEY-----\n";
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("RSA not available", ex);
        }
    }

    /** Derives the public key from a PKCS#8 (CRT) RSA private key. */
    public static RSAPublicKey publicKeyOf(RSAPrivateKey privateKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalArgumentException("Expected an RSAPrivateCrtKey");
        }
        try {
            var spec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Unable to derive RSA public key", ex);
        }
    }
}
