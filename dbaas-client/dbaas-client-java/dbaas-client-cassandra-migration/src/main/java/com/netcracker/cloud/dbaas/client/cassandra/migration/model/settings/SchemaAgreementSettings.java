package com.netcracker.cloud.dbaas.client.cassandra.migration.model.settings;

import java.time.Duration;

/**
 * Schema migration schema agreement settings.
 * Use {@link SchemaAgreementSettingsBuilder} for creation.
 *
 * @param awaitRetryDelay retry delay for schema agreement await
 */
public record SchemaAgreementSettings(
        Long awaitRetryDelay
) {
    private static final Duration DEFAULT_AWAIT_RETRY_DELAY = Duration.ofMillis(500);

    public static SchemaAgreementSettingsBuilder builder() {
        return new SchemaAgreementSettingsBuilder();
    }

    public static class SchemaAgreementSettingsBuilder {
        private Long awaitRetryDelay = DEFAULT_AWAIT_RETRY_DELAY.toMillis();

        public SchemaAgreementSettingsBuilder withAwaitRetryDelay(Long awaitRetryDelay) {
            if (awaitRetryDelay != null)
                this.awaitRetryDelay = awaitRetryDelay;
            return this;
        }

        public SchemaAgreementSettings build() {
            return new SchemaAgreementSettings(awaitRetryDelay);
        }
    }
}
