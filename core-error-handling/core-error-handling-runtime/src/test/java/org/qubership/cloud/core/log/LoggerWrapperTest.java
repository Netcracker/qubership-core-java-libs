package org.qubership.cloud.core.log;

import org.qubership.cloud.utils.DoubleOutputStream;
import org.qubership.cloud.utils.TestLowerException;
import org.qubership.cloud.utils.TestUpperException;
import lombok.CustomLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

@CustomLog
public class LoggerWrapperTest {
    private ByteArrayOutputStream outContent;
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUpStreams() {
        outContent = new ByteArrayOutputStream();
        System.setOut((new PrintStream(new DoubleOutputStream(outContent, originalOut))));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(originalOut);
    }

    @Test
    public void testStackOf2ErrorCodeExceptions() {
        TestLowerException cause = new TestLowerException();
        TestUpperException exception = new TestUpperException(cause);
        log.error("test log message", exception);
        String actualLog = outContent.toString();
        String expectedPrefix = String.format(LoggerWrapper.ERROR_CODE_LOG_PREFIX_TEMPLATE,
                exception.getErrorCode().getCode(), exception.getId());
        Assertions.assertTrue(actualLog.startsWith(expectedPrefix));
    }

    @Test
    public void testStackOf3ErrorCodeExceptions() {
        TestLowerException cause = new TestLowerException();
        RuntimeException middle = new RuntimeException("in the middle exception", cause);
        TestUpperException exception = new TestUpperException(middle);
        log.error("test log message", exception);
        String actualLog = outContent.toString();
        String expectedPrefix = String.format(LoggerWrapper.ERROR_CODE_LOG_PREFIX_TEMPLATE,
                exception.getErrorCode().getCode(), exception.getId());
        Assertions.assertTrue(actualLog.startsWith(expectedPrefix));
    }
}
