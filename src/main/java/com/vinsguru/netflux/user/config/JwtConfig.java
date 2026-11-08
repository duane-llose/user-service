package com.vinsguru.netflux.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;

@Configuration
public class JwtConfig {

    @Bean
    public JwtEncoder jwtEncoder(RsaKeyProperties properties) {
        var privateKey = properties.privateKey();
        return NimbusJwtEncoder.withKeyPair(derivePublicKey(privateKey), privateKey)
                .build();
    }

    // A standard PKCS#8 RSA private key is an RSAPrivateCrtKey, which carries the modulus and public exponent
    private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) {
        if (!(privateKey instanceof RSAPrivateCrtKey crtKey)) {
            throw new IllegalStateException("RSA private key must be a CRT key (PKCS#8) to derive the public key");
        }
        try {
            var spec = new RSAPublicKeySpec(crtKey.getModulus(), crtKey.getPublicExponent());
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to derive RSA public key", ex);
        }
    }
}
