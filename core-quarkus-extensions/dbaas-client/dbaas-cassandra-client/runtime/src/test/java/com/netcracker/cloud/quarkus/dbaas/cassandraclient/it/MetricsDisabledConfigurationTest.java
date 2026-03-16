package com.netcracker.cloud.quarkus.dbaas.cassandraclient.it;

import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.metrics.MetricsProvider;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

@QuarkusTest
@TestProfile(MetricsDisabledConfigurationTest.DisabledMetricsProfile.class)
public class MetricsDisabledConfigurationTest {

    @Inject
    Instance<MetricsProvider<CassandraDatabase>> metricsProvider;

    @Test
    void checkMetricsProviderIsNotCreated() {
        Assertions.assertTrue(metricsProvider.isUnsatisfied());
    }

    @NoArgsConstructor
    protected static final class DisabledMetricsProfile extends CassandraResourceProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("quarkus.dbaas.cassandra.metrics.enabled", "false");
        }
    }
}
