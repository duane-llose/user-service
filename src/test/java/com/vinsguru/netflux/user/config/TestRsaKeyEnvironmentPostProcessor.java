package com.vinsguru.netflux.user.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Generates a throwaway RSA key pair for tests and points {@code rsa.private-key} at its PKCS#8 PEM file.
 */
public class TestRsaKeyEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Path KEY_PATH = Path.of("target", "test-keys", "private.pem");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        writePrivateKeyPem();
        var location = "file:" + KEY_PATH.toAbsolutePath();
        environment.getPropertySources()
                .addFirst(new MapPropertySource("testRsaKey", Map.of("rsa.private-key", location)));
    }

    private void writePrivateKeyPem() {
        try {
            Files.createDirectories(KEY_PATH.getParent());
            Files.writeString(KEY_PATH, TestRsaKeys.generatePrivateKeyPem(), StandardCharsets.US_ASCII);
        } catch (IOException ex) {
            throw new UncheckedIOException("Unable to write test RSA key", ex);
        }
    }
}
