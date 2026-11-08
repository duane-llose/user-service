package com.vinsguru.netflux.user.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.security.interfaces.RSAPrivateKey;

@Validated
@ConfigurationProperties(prefix = "rsa")
public record RsaKeyProperties(@NotNull RSAPrivateKey privateKey) {
}
