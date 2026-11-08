package com.vinsguru.netflux.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(@DefaultValue("1h") Duration expiration) {
}
