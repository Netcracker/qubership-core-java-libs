package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.management.classifier.ServiceDbaaSClassifierBuilder;
import org.qubership.cloud.dbaas.client.service.flyway.FlywayRunner;
import org.qubership.cloud.dbaas.client.testconfiguration.PostgresTestContainerConfiguration;
import org.qubership.cloud.dbaas.client.testconfiguration.TestPostgresWithDatasourceConfig;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import javax.sql.DataSource;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = {TestPostgresWithDatasourceConfig.class, PostgresTestContainerConfiguration.class})
public class PostgresDatasourceCreatorFlywayTest {

    @Autowired
    private DbaasPostgresqlDatasourceBuilder datasourceBuilder;

    @Test
    public void testFlywayRunner() {
        DataSource dataSource = datasourceBuilder
                .newBuilder(new ServiceDbaaSClassifierBuilder())
                .withFlyway(getFlywayRunner())
                .build();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List<String> name = jdbcTemplate.queryForList("SELECT name FROM city", String.class);
        Assertions.assertEquals(1, name.size());
        Assertions.assertEquals("Moscow", name.get(0));
    }

    @NotNull
    private static FlywayRunner getFlywayRunner() {
        return context -> {
            Flyway flyway = Flyway.configure()
                    .dataSource(context.getDataSource())
                    .baselineOnMigrate(true)
                    .locations("classpath:db/migration/postgresql")
                    .load();
            flyway.migrate();
        };
    }
}