package org.qubership.cloud.core.error.rest.tmf;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.core.error.rest.exception.RemoteCodeException;
import org.qubership.cloud.core.error.rest.exceptions.DbaasAggregatorValidationException;
import org.qubership.cloud.core.error.rest.exceptions.DbaasClientLibException;
import org.qubership.cloud.core.error.rest.exceptions.ProjectClientLibException;
import org.qubership.cloud.core.error.rest.exceptions.ProjectMsInternalException;
import org.qubership.cloud.core.error.rest.tmf.model.Source;
import org.qubership.cloud.core.error.rest.utils.DoubleOutputStream;
import org.qubership.cloud.core.error.runtime.MultiCauseException;
import org.qubership.cloud.core.log.LoggerWrapper;
import lombok.CustomLog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CustomLog
public class TmfErrorCodeResponseTest {
    private ByteArrayOutputStream outContent;
    private final PrintStream originalOut = System.out;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    public void testErrorCodeResponseSerialization() throws Exception {
        DbaasAggregatorValidationException validationErr1 = new DbaasAggregatorValidationException(
                ErrorCodes.DBAAS_VALIDATION_4001,
                "detailed message", Source.builder().pointer("/attr1").build());
        DbaasAggregatorValidationException validationErr2 = new DbaasAggregatorValidationException(
                ErrorCodes.DBAAS_VALIDATION_4002,
                "detailed message", Source.builder().pointer("/attr2").build());
        MultiCauseException dbaasAgentErr = new MultiCauseException(
                Stream.of(validationErr1, validationErr2).collect(Collectors.toList()));
        // imitate exception was serialized into REST response and
        // deserialized on the client side into RemoteCodeException
        TmfErrorResponse response = TmfErrorResponse.builder(dbaasAgentErr)
                .referenceError("https://errors-portal.com/error/" + dbaasAgentErr.getErrorCode().getCode())
                .errors(dbaasAgentErr.getCauseExceptions().stream()
                        .map(cause -> TmfError.builder(cause)
                                .referenceError("https://errors-portal.com/error/" +
                                        cause.getErrorCode().getCode())
                                .build()
                        ).collect(Collectors.toList())
                ).build();
        TmfErrorResponseConverter errorResponseConverter = new DefaultTmfErrorResponseConverter();
        RemoteCodeException remoteErrorCodeException = errorResponseConverter.buildErrorCodeException(response);

        DbaasClientLibException dbaasClientLibErr = new DbaasClientLibException(
                "detailed message", remoteErrorCodeException);
        ProjectClientLibException projectLibErr = new ProjectClientLibException(
                "detailed message", dbaasClientLibErr);
        Exception checkedException = new Exception("test checked exception", projectLibErr);
        ProjectMsInternalException projectMsErr = new ProjectMsInternalException(
                "detailed message", UUID.randomUUID(), checkedException);
        log.error("Failed to execute business operation", projectMsErr);
        String actualLog = outContent.toString();
        String expectedPrefix = String.format(LoggerWrapper.ERROR_CODE_LOG_PREFIX_TEMPLATE,
                projectMsErr.getErrorCode().getCode(), projectMsErr.getId());
        Assertions.assertTrue(actualLog.startsWith(expectedPrefix));

        TmfErrorResponse projectMsErrResponse = TmfErrorResponse.builder(projectMsErr)
                .referenceError("https://errors-portal.com/error/" + projectMsErr.getErrorCode().getCode())
                .build();
        originalOut.println(objectMapper.writerWithDefaultPrettyPrinter().

                writeValueAsString(projectMsErrResponse));
    }
}
