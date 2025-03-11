package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import org.qubership.cloud.dbaas.client.cassandra.service.DbaasCqlSessionBuilderCustomizer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ApplicationScoped
public class CassandraClientTestConfiguration {

    @Produces
    public DbaasCqlSessionBuilderCustomizer getDbaasCqlSessionBuilderCustomizer1() {
        return new Test1DbaasCqlSessionBuilderCustomizer();
    }

    @Produces
    public DbaasCqlSessionBuilderCustomizer getDbaasCqlSessionBuilderCustomizer2() {
        return new Test2DbaasCqlSessionBuilderCustomizer();
    }

    @Priority(3)
    static public class Test1DbaasCqlSessionBuilderCustomizer implements DbaasCqlSessionBuilderCustomizer {

        @Override
        public void customize(CqlSessionBuilder cqlSessionBuilder) {

        }

        @Override
        public void customize(ProgrammaticDriverConfigLoaderBuilder programmaticDriverConfigLoaderBuilder) {
            assertEquals(Duration.ofMinutes(1), programmaticDriverConfigLoaderBuilder.build()
                    .getInitialConfig().getDefaultProfile()
                    .getDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT));

            programmaticDriverConfigLoaderBuilder.withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofMinutes(5));
        }
    }

    @Priority(5)
    static public class Test2DbaasCqlSessionBuilderCustomizer implements DbaasCqlSessionBuilderCustomizer {

        @Override
        public void customize(CqlSessionBuilder cqlSessionBuilder) {

        }

        @Override
        public void customize(ProgrammaticDriverConfigLoaderBuilder programmaticDriverConfigLoaderBuilder) {
            assertEquals(Duration.ofMinutes(5), programmaticDriverConfigLoaderBuilder.build()
                    .getInitialConfig().getDefaultProfile()
                    .getDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT));

            programmaticDriverConfigLoaderBuilder.withDuration(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, Duration.ofMinutes(50));
        }
    }
}
