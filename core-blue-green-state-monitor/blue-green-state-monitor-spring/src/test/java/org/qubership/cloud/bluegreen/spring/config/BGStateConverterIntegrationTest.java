package org.qubership.cloud.bluegreen.spring.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {AbstractConsulTest.TestTokenStorageConfig.class, BlueGreenStatePublisherConfiguration.class, BGStateConfiguration.class},
        properties = {"cloud.microservice.namespace=test-namespace-1"})
class BGStateConverterIntegrationTest extends AbstractConsulTest {

    @Autowired
    BlueGreenStatePublisher blueGreenStatePublisher;

    @Test
    void logContainsNamespaceStatus() {
        Logger logger = (Logger) LoggerFactory.getLogger("test.logger");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        PatternLayout layout = new PatternLayout();
        layout.setContext(logger.getLoggerContext());
        layout.setPattern("%d{HH:mm:ss.SSS} %-5level [%thread] %logger bg_state:%bg_state - %msg%n");
        layout.start();


        logger.info("Test message");


        boolean found = appender.list.stream()
                .map(layout::doLayout)
                .anyMatch(msg -> msg.contains("bg_state:active"));
        assertThat(found).isTrue();
    }
}
