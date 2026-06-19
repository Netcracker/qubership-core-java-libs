package com.netcracker.cloud.podsecrets.quarkus.runtime;

import com.netcracker.cloud.podsecrets.PodSecretsLoaderConfig;
import io.smallrye.config.ConfigSourceContext;
import io.smallrye.config.ConfigValue;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PodSecretsConfigSourceFactoryTest {

    private static ConfigValue cvOf(String value) {
        return ConfigValue.builder().withName("key").withValue(value).build();
    }

    @Test
    void disabledViaContext_returnsEmpty() {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_ENABLED)).thenReturn(cvOf("false"));

        var sources = new PodSecretsConfigSourceFactory().getConfigSources(ctx);

        assertThat(sources).isEmpty();
        verify(ctx, never()).getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR);
        verify(ctx, never()).getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL);
    }

    @Test
    void enabledExplicitly_returnsOneSource(@TempDir Path dir) {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_ENABLED)).thenReturn(cvOf("true"));
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR)).thenReturn(cvOf(dir.toString()));
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL)).thenReturn(cvOf("PT1S"));

        var sources = new PodSecretsConfigSourceFactory().getConfigSources(ctx);

        assertThat(sources).hasSize(1);
        assertThat(sources.iterator().next())
                .isInstanceOf(PodSecretsConfigSource.class)
                .extracting(ConfigSource::getName).isEqualTo("pod-secrets-config-source");
    }

    @Test
    void enabledDefaultsToTrue(@TempDir Path dir) {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_ENABLED)).thenReturn(null);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR)).thenReturn(cvOf(dir.toString()));
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL)).thenReturn(null);

        var sources = new PodSecretsConfigSourceFactory().getConfigSources(ctx);

        assertThat(sources).hasSize(1);
    }

    @Test
    void dirOverrideIsApplied(@TempDir Path dir) {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR)).thenReturn(cvOf(dir.toString()));
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL)).thenReturn(null);

        var config = PodSecretsConfigSourceFactory.fromContext(ctx);

        assertThat(config.getBaseDir()).isEqualTo(dir);
    }

    @Test
    void ttlOverrideIsParsed() {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR)).thenReturn(null);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL)).thenReturn(cvOf("PT30S"));

        var config = PodSecretsConfigSourceFactory.fromContext(ctx);

        assertThat(config.getTtl()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void bothMissing_useDefaultConfig() {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_DIR)).thenReturn(null);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL)).thenReturn(null);

        var config = PodSecretsConfigSourceFactory.fromContext(ctx);

        assertThat(config.getBaseDir()).isEqualTo(PodSecretsLoaderConfig.fromSystem().getBaseDir());
        assertThat(config.getTtl()).isEqualTo(PodSecretsLoaderConfig.fromSystem().getTtl());
    }

    @Test
    void getValue_nullConfigValue_returnsDefault() {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue("any.key")).thenReturn(null);

        String result = PodSecretsConfigSourceFactory.getValue(ctx, "any.key", "fallback", Function.identity());

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void getValue_configValueWithNull_returnsDefault() {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue("any.key")).thenReturn(cvOf(null));

        String result = PodSecretsConfigSourceFactory.getValue(ctx, "any.key", "fallback", Function.identity());

        assertThat(result).isEqualTo("fallback");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void getValue_blank_returnsDefault(String blank) {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue("any.key")).thenReturn(cvOf(blank));

        String result = PodSecretsConfigSourceFactory.getValue(ctx, "any.key", "fallback", Function.identity());

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void getValue_valid_appliesConverter() {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue("any.key")).thenReturn(cvOf("42"));

        Integer result = PodSecretsConfigSourceFactory.getValue(ctx, "any.key", 0, Integer::valueOf);

        assertThat(result).isEqualTo(42);
    }

    @Test
    void getValue_converterThrows_propagatesException() {
        var ctx = mock(ConfigSourceContext.class);
        when(ctx.getValue(PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL)).thenReturn(cvOf("not-a-duration"));

        assertThatThrownBy(() ->
                PodSecretsConfigSourceFactory.getValue(ctx, PodSecretsLoaderConfig.PROP_POD_SECRETS_TTL, Duration.ZERO, Duration::parse)
        ).isInstanceOf(DateTimeParseException.class);
    }
}
