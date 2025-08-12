package com.netcracker.cloud.log.manager.spring;

import com.netcracker.cloud.log.manager.common.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.core.env.Environment;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LoggingUpdaterTest {

    @Mock
    private Environment environment;

    @InjectMocks
    private LoggingUpdater loggingUpdater;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testOnApplicationEvent_LogLevelChangedSuccessfully() {
        Set<String> changedKeys = Set.of("logging.level.com.example");
        EnvironmentChangeEvent event = new EnvironmentChangeEvent(this, changedKeys);
        when(environment.getProperty("logging.level.com.example")).thenReturn("INFO");

        loggingUpdater.onApplicationEvent(event);
        verify(environment, times(1)).getProperty("logging.level.com.example");
        assertEquals(LogManager.getLogLevel().get("com.example"), "INFO");
    }

    @Test
    void testOnApplicationEvent_LogLevelResetToDefault() {
        Set<String> changedKeys = Set.of("logging.level.com.example");
        EnvironmentChangeEvent event = new EnvironmentChangeEvent(this, changedKeys);
        when(environment.getProperty("logging.level.com.example")).thenReturn(null);

        loggingUpdater.onApplicationEvent(event);

        verify(environment, times(1)).getProperty("logging.level.com.example");
        assertEquals(LogManager.getLogLevel().get("com.example"), "INFO");
    }
}
