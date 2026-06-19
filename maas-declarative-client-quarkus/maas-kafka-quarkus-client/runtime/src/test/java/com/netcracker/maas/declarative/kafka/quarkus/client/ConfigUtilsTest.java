package com.netcracker.maas.declarative.kafka.quarkus.client;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConfigUtilsTest {

  @Test
  void putIfPresentUsesFirstMatchingTypeOnly() {
    Config config = mock(Config.class);
    String key = "maas.kafka.client.consumer.property.max.poll.records";
    when(config.getPropertyNames()).thenReturn(Set.of(key));
    when(config.getOptionalValue(key, Integer.class)).thenReturn(Optional.of(2));
    when(config.getOptionalValue(key, Long.class)).thenReturn(Optional.of(2L));
    when(config.getOptionalValue(key, Double.class)).thenReturn(Optional.empty());
    when(config.getOptionalValue(key, Boolean.class)).thenReturn(Optional.of(false));
    when(config.getOptionalValue(key, String.class)).thenReturn(Optional.empty());

    Map<String, Object> map = ConfigUtils.configAsMap(config, "maas.kafka.client.consumer.property.");

    assertThat(map).containsEntry("max.poll.records", 2);
    verify(config, never()).getOptionalValue(eq(key), eq(Boolean.class));
  }

  @Test
  void configAsMapParsesStringBooleans() {
    Config config = mock(Config.class);
    String key = "maas.kafka.client.consumer.property.enable.auto.commit";
    when(config.getPropertyNames()).thenReturn(Set.of(key));
    when(config.getOptionalValue(key, Integer.class)).thenReturn(Optional.empty());
    when(config.getOptionalValue(key, Long.class)).thenReturn(Optional.empty());
    when(config.getOptionalValue(key, Double.class)).thenReturn(Optional.empty());
    when(config.getOptionalValue(key, Boolean.class)).thenReturn(Optional.empty());
    when(config.getOptionalValue(key, String.class)).thenReturn(Optional.of("true"));

    Map<String, Object> map = ConfigUtils.configAsMap(config, "maas.kafka.client.consumer.property.");

    assertThat(map).containsEntry("enable.auto.commit", true);
  }
}
