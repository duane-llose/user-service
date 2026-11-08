package com.vinsguru.netflux.user.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.crypto.RsaKeyConversionServicePostProcessor;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Startup behaviour of the RSA signing key: the application must fail fast when the key is missing or invalid.
 * <p>
 * Uses {@link ApplicationContextRunner} because the full application context always receives the generated
 * test key from {@link TestRsaKeyEnvironmentPostProcessor}.
 */
class RsaKeyConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RsaKeyTestConfiguration.class);

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Should create the JWT encoder when a valid PKCS#8 RSA key is configured")
    void shouldStartWithValidKey() throws IOException {
        var keyFile = writeKeyFile(TestRsaKeys.generatePrivateKeyPem());

        contextRunner.withPropertyValues("rsa.private-key=file:" + keyFile)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JwtEncoder.class);
                });
    }

    @Test
    @DisplayName("Should fail startup when rsa.private-key is not configured")
    void shouldFailWhenKeyMissing() {
        contextRunner.run(context -> assertThat(context).hasFailed()
                .getFailure()
                .hasRootCauseInstanceOf(BindValidationException.class)
                .hasStackTraceContaining("field 'privateKey': rejected value [null]"));
    }

    @Test
    @DisplayName("Should fail startup when the key file does not exist")
    void shouldFailWhenKeyFileMissing() {
        var missingFile = tempDir.resolve("missing.pem");

        contextRunner.withPropertyValues("rsa.private-key=file:" + missingFile)
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .hasRootCauseInstanceOf(FileNotFoundException.class)
                        .hasStackTraceContaining("missing.pem"));
    }

    @Test
    @DisplayName("Should fail startup when the key file is not a PEM private key")
    void shouldFailWhenKeyFileInvalid() throws IOException {
        var keyFile = writeKeyFile("not a private key");

        contextRunner.withPropertyValues("rsa.private-key=file:" + keyFile)
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .hasRootCauseInstanceOf(IllegalArgumentException.class)
                        .hasRootCauseMessage("Key is not in PEM-encoded PKCS#8 format, "
                                + "please check that the header begins with -----BEGIN PRIVATE KEY-----"));
    }

    private Path writeKeyFile(String content) throws IOException {
        return Files.writeString(tempDir.resolve("private.pem"), content, StandardCharsets.US_ASCII);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(RsaKeyProperties.class)
    @Import(JwtConfig.class)
    static class RsaKeyTestConfiguration {

        // In the application this is registered by Spring Security's web configuration
        @Bean
        static RsaKeyConversionServicePostProcessor rsaKeyConversionServicePostProcessor() {
            return new RsaKeyConversionServicePostProcessor();
        }
    }
}
