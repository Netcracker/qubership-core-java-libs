package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.config.properties.DbaasCassandraMigrationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {TestConfig.class},
        properties = {
                "dbaas.cassandra.migration.enabled=true",
                "dbaas.cassandra.migration.schema-history-table-name=schema-history-test",
                "dbaas.cassandra.migration.version.settings-resource-path=settings-res-path-test",
                "dbaas.cassandra.migration.version.directory-path=version-dir-path-test",
                "dbaas.cassandra.migration.version.resource-name-pattern=res-name-pattern-test",
                "dbaas.cassandra.migration.template.definitions-resource-path=def-res-path-test",
                "dbaas.cassandra.migration.lock.table-name=lock-table-name-test",
                "dbaas.cassandra.migration.lock.retry-delay=111",
                "dbaas.cassandra.migration.lock.lock-lifetime=222",
                "dbaas.cassandra.migration.lock.extension-period=333",
                "dbaas.cassandra.migration.lock.extension-fail-retry-delay=444",
                "dbaas.cassandra.migration.schema-agreement.await-retry-delay=555",
                "dbaas.cassandra.migration.amazon-keyspaces.enabled=true",
                "dbaas.cassandra.migration.amazon-keyspaces.table-status-check.pre-delay=666",
                "dbaas.cassandra.migration.amazon-keyspaces.table-status-check.retry-delay=777"
        })
@EnableDbaasCassandra
public class CassandraMigrationPropertiesTest {

    @Autowired
    private DbaasCassandraMigrationProperties migrationProperties;

    @Test
    public void checkCassandraMigrationProperties() {
        Assertions.assertTrue(migrationProperties.isEnabled());
        Assertions.assertEquals("schema-history-test", migrationProperties.getSchemaHistoryTableName());

        Assertions.assertEquals("settings-res-path-test", migrationProperties.getVersion().getSettingsResourcePath());
        Assertions.assertEquals("version-dir-path-test", migrationProperties.getVersion().getDirectoryPath());
        Assertions.assertEquals("res-name-pattern-test", migrationProperties.getVersion().getResourceNamePattern());

        Assertions.assertEquals("def-res-path-test", migrationProperties.getTemplate().getDefinitionsResourcePath());

        Assertions.assertEquals("lock-table-name-test", migrationProperties.getLock().getTableName());
        Assertions.assertEquals(111L, migrationProperties.getLock().getRetryDelay());
        Assertions.assertEquals(222L, migrationProperties.getLock().getLockLifetime());
        Assertions.assertEquals(333L, migrationProperties.getLock().getExtensionPeriod());
        Assertions.assertEquals(444L, migrationProperties.getLock().getExtensionFailRetryDelay());

        Assertions.assertEquals(555L, migrationProperties.getSchemaAgreement().getAwaitRetryDelay());

        Assertions.assertTrue(migrationProperties.getAmazonKeyspaces().isEnabled());
        Assertions.assertEquals(666L, migrationProperties.getAmazonKeyspaces().getTableStatusCheck().getPreDelay());
        Assertions.assertEquals(777L, migrationProperties.getAmazonKeyspaces().getTableStatusCheck().getRetryDelay());
    }
}
