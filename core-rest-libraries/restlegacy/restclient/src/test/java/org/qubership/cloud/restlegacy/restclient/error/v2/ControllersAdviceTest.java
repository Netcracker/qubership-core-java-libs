package org.qubership.cloud.restlegacy.restclient.error.v2;

import jakarta.servlet.http.HttpServletRequest;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.qubership.cloud.restlegacy.restclient.app.TestConfig;
import org.qubership.cloud.restlegacy.restclient.error.*;
import org.qubership.cloud.restlegacy.restclient.service.MessageService;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.MethodParameter;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.retry.RetryStatistics;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.text.MessageFormat;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithV2ExceptionModel.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {TestConfig.class, TestExceptionHandlingConfiguration.class, ControllerWithV2ExceptionModel.class})
class ControllersAdviceTest extends ExceptionHandlerControllersAdviceBase {

    private final MessageFormat messageFormat = new MessageFormat("{0}");
    @Mock
    MessageService messageService;
    @Mock
    HttpServletRequest httpServletRequest;
    private ControllersAdvice baseAdvice;

    @BeforeEach
    void setUp() {
        baseAdvice = new ControllersAdvice(messageService);
    }


    @Test
    void useV1VersionOfErrorHandlingByDefault() {
        assertNotNull(context.getBean(ControllersAdvice.class));
    }

    @Test
    void dontUseV2VersionOfErrorHandlingByDefault() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(ExceptionHandlingV2MainConfiguration.class));
    }

    @Test
    void dontRetryRequestToSystemThatUsesV2ModelIfFailWasInTransitiveService() {
        restClient.safelySendRequest(SYSTEM_WITH_V2_MODEL + TRANSITIVE_FAILED_REQUEST);

        final RetryStatistics statisticForRequest = getStatisticForRequest(SYSTEM_WITH_V2_MODEL + TRANSITIVE_FAILED_REQUEST);
        MatcherAssert.assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    void retryRequestToSystemThatUsesV2ModelIfFailWasInDirectService() {
        restClient.safelySendRequest(SYSTEM_WITH_V2_MODEL + DIRECTLY_FAILED_REQUEST);

        final RetryStatistics statisticForRequest = getStatisticForRequest(SYSTEM_WITH_V2_MODEL + DIRECTLY_FAILED_REQUEST);
        MatcherAssert.assertThat(statisticForRequest, abortedAfterSeveralAttempts());
    }

    @Test
    void handleErrorExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("errorExceptionTest");
        Object[] parameters = {"test characteristic"};
        String templateMessage = "Parameters: {0}";
        String message = "Parameters: test characteristic";
        ErrorType errorType = ErrorType.BAD_REQUEST;

        setMessageServiceResponse(errorType, parameters, message, templateMessage);
        ErrorsDescription.ErrorDescription error = getError(baseAdvice.handleErrorException(
                httpServletRequest, new ErrorException(null, errorType, parameters)
        ));

        defaultAssert(errorType, error, parameters, message);
    }

    @Test
    void handleMethodArgumentNotValidExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("handleMethodArgumentNotValidExceptionTest");
        BindingResult bindingResult = new BindException(new Object(), "");
        FieldError fieldError = new FieldError("objectName", "field", "defaultMessage");
        bindingResult.addError(fieldError);

        String fieldMessage = "FieldMessage";
        Object[] parameters = {fieldError.getField(), fieldMessage};
        String templateMessage = "Parameters: {0}";
        String message = "Parameters: " + Arrays.toString(parameters);
        ErrorType commonErrorType = ErrorType.UNPROCESSABLE_ENTITY_VALIDATION_FAILED;

        when(messageService.getMessageCode(commonErrorType.getErrorCode())).thenReturn(commonErrorType.getErrorCode());
        when(messageService.getMessage(fieldError)).thenReturn(fieldMessage);
        when(messageService.getMessage(commonErrorType.getMessageCode())).thenReturn(templateMessage);
        when(messageService.getMessage(commonErrorType.getMessageCode(), fieldError.getField(), messageService.getMessage(fieldError))).thenReturn(message);

        ErrorsDescription.ErrorDescription error = getError(baseAdvice.handleMethodArgumentNotValidException(
                httpServletRequest, new MethodArgumentNotValidExceptionExt(null, bindingResult, message)
        ));

        defaultAssert(commonErrorType, error, parameters, message);
    }

    @Test
    void handleMethodArgumentTypeMismatchExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("handleMethodArgumentTypeMismatchExceptionTest");
        String[] parameters = {"objectName", "value"};
        String templateMessage = "Parameters: {0}, {1}";
        String message = "Parameters: objectName, value";
        ErrorType errorType = ErrorType.BAD_REQUEST;

        setMessageServiceResponse(errorType, parameters, message, templateMessage);
        ErrorsDescription.ErrorDescription error = getError(baseAdvice.handleMethodArgumentTypeMismatchException(
                httpServletRequest, new MethodArgumentTypeMismatchException(parameters[1], Object.class, parameters[0], null, null)
        ));

        defaultAssert(errorType, error, parameters, message);
    }

    @Test
    void handleHttpMessageNotReadableExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("handleHttpMessageNotReadableExceptionTest");
        String templateMessage = "Parameters: objectName, value";
        ErrorType errorType = ErrorType.BAD_REQUEST;

        setMessageServiceResponse(errorType, null, null, templateMessage);
        ErrorsDescription.ErrorDescription error = getError(baseAdvice.handleHttpMessageNotReadableException(
                httpServletRequest, new HttpMessageNotReadableException(templateMessage)
        ));

        defaultAssert(errorType, error, null, templateMessage);
    }

    @Test
    void handleOptimisticLockingFailureExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("handleOptimisticLockingFailureExceptionTest");
        String templateMessage = "Parameters: objectName, value";
        ErrorType errorType = ErrorType.CONFLICT;

        setMessageServiceResponse(errorType, null, null, templateMessage);
        ErrorsDescription.ErrorDescription error = getError(baseAdvice.handleOptimisticLockingFailureException(
                httpServletRequest, new OptimisticLockingFailureException(templateMessage)
        ));

        defaultAssert(errorType, error, null, templateMessage);
    }

    @Test
    void handleIllegalArgumentExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("handleIllegalArgumentExceptionTest");
        String templateMessage = "Parameters: objectName, value";
        ErrorType errorType = ErrorType.BAD_REQUEST;

        setMessageServiceResponse(errorType, null, null, templateMessage);
        ErrorsDescription.ErrorDescription error = getError(baseAdvice.handleIllegalArgumentException(
                httpServletRequest, new IllegalArgumentException(templateMessage)
        ));

        defaultAssert(errorType, error, null, templateMessage);
    }

    @Test
    void handleMissingServletRequestParameterExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("handleMissingServletRequestParameterExceptionTest");
        ErrorType errorType = ErrorType.BAD_REQUEST;

        String paramName = "microserviceName";
        String[] parameters = {paramName};
        String message = "Required request parameter 'microserviceName' for method parameter type String is not present";

        setMessageServiceResponse(errorType, parameters, message, message);
        ErrorsDescription.ErrorDescription error = getError(
                baseAdvice.handleMissingServletRequestParameterException(
                        httpServletRequest,
                        new MissingServletRequestParameterException(paramName, "String")
                )
        );
        defaultAssert(errorType, error, parameters, message);
    }

    @Test
    void handleEntityNotFoundExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("handleEntityNotFoundExceptionTest");
        String message = "Parameters: entityTypeName, \"550e8400-e29b-41d4-a716-446655440000\"";
        Object[] parameters = {"entityTypeName", "550e8400-e29b-41d4-a716-446655440000"};
        String[] stringParameters = Arrays.stream(parameters).map(parameter -> messageFormat.format(new Object[]{parameter})).toArray(String[]::new);
        String templateMessage = "Parameters: {0}, {1}";

        ErrorType errorType = ErrorType.NOT_FOUND;
        setMessageServiceResponse(errorType, parameters, message, templateMessage);
        ErrorsDescription.ErrorDescription error = getError(
                baseAdvice.handleEntityNotFoundException(
                        httpServletRequest,
                        new EntityNotFoundException((String) parameters[0], (String) parameters[1])
                )
        );

        defaultAssert(errorType, error, stringParameters, message);
    }

    @Test
    void handleExceptionTest() {
        when(httpServletRequest.getRequestURI()).thenReturn("handleExceptionTest");
        String templateMessage = "templateMessage";
        ErrorType errorType = ErrorType.INTERNAL_SERVER_ERROR;

        setMessageServiceResponse(errorType, null, null, templateMessage);
        ErrorsDescription.ErrorDescription error = getError(baseAdvice.handleException(httpServletRequest, new Exception(templateMessage, null)));

        defaultAssert(errorType, error, null, templateMessage);
    }

    private void defaultAssert(ErrorType errorType, ErrorsDescription.ErrorDescription error, Object[] parameters, String messageWithParams) {
        assertEquals(error.getErrorMessage(), messageWithParams);
        if (parameters != null)
            assertArrayEquals(error.getParameters(), parameters);
        assertEquals(error.getErrorCode(), errorType.getErrorCode());
    }

    private void setMessageServiceResponse(ErrorType errorType, Object[] parameters, String messageWithParams, String templateMessage) {
        when(messageService.getMessageCode(errorType.getErrorCode())).thenReturn(errorType.getErrorCode());
        when(messageService.getMessage(errorType.getMessageCode(), parameters)).thenReturn(messageWithParams);
        when(messageService.getMessage(errorType.getMessageCode())).thenReturn(templateMessage);
    }

    private ErrorsDescription.ErrorDescription getError(ResponseEntity<ErrorsDescription> resp) {
        assertNotNull(resp);
        ErrorsDescription errorsDescription = resp.getBody();
        return errorsDescription.getErrors().get(0);
    }

    private class MethodArgumentNotValidExceptionExt extends MethodArgumentNotValidException {
        private final String message;

        public MethodArgumentNotValidExceptionExt(MethodParameter parameter, BindingResult bindingResult, String message) {
            super(parameter, bindingResult);
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }
    }
}
