package org.qubership.cloud.microserviceframework.config;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.qubership.cloud.microserviceframework.testconfig.TestConstants.*;

@SpringBootTest(classes = MicroserviceFrameworkConfiguration.class)
class MicroserviceFrameworkConfigurationTest {

    @Autowired
    private ReloadableResourceBundleMessageSource reloadableResourceBundleMessageSource;

    @Test
    void testLocale() {
        String actualMessage = reloadableResourceBundleMessageSource.getMessage(TEST_ERROR_CODE, null, Locale.US);
        assertEquals(TEST_ERROR_MESSAGE, actualMessage);
        actualMessage = reloadableResourceBundleMessageSource.getMessage(TEST_CODE, null, Locale.US);
        assertEquals(TEST_MESSAGE, actualMessage);
    }

    @Test
    void testLocaleCodeNotFound() {
        assertThrows(NoSuchMessageException.class, () -> reloadableResourceBundleMessageSource.getMessage(WRONG_TEST_CODE, null, Locale.US));
    }

}
