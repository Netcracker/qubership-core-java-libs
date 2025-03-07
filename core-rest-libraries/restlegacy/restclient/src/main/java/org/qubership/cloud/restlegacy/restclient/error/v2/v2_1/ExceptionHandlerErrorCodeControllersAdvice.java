package org.qubership.cloud.restlegacy.restclient.error.v2.v2_1;

import org.qubership.cloud.restlegacy.restclient.error.EntityNotFoundException;
import org.qubership.cloud.restlegacy.restclient.error.ErrorException;
import org.qubership.cloud.restlegacy.restclient.error.ProxyErrorException;
import org.qubership.cloud.restlegacy.restclient.error.v2.DebugInfo;
import org.qubership.cloud.restlegacy.restclient.error.v2.HasDebugInfo;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2_1;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ResponseAdviceExceptionHelper.*;
import static org.qubership.cloud.restlegacy.restclient.error.v2.v2_1.ErrorMessageCodes.*;
import static org.qubership.cloud.restlegacy.restclient.error.v2.v2_1.MessageParameterTypes.LOCALIZED_STRING;
import static org.qubership.cloud.restlegacy.restclient.error.v2.v2_1.MessageParameterTypes.STRING;


@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@ConditionalOnProperty(name = ERROR_HANDLER_VERSION_CONDITION_PROPERTY, havingValue = VERSION_2_1)
class ExceptionHandlerErrorCodeControllersAdvice {

    private static String calculateObjectValidationErrorMessage(final List<ObjectError> allErrors) {
        for (ObjectError error : allErrors) {
            if (!(error instanceof FieldError)) {
                return error.getCode();
            }
        }
        return OPERATION_VALIDATION_ERROR_CODE;
    }

    private static void generalErrorHandling(HttpServletRequest request, Exception e, HasDebugInfo<DebugInfo> errorDescription) {
        addDebugInfo(request, e, errorDescription);
        log.error(errorDescription.toString());
    }

    @SuppressWarnings("unused")
    @Deprecated
    @ExceptionHandler(ErrorException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleLegacyErrorException(HttpServletRequest request, ErrorException e) {
        final ErrorMessage errorMessage = new ErrorMessage(INTERNAL_SERVER_ERROR_CODE);
        final SimpleErrorMessageResponse errorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        generalErrorHandling(request, e, errorMessageResponse);
        //we cannot create response based on content of ErrorException#errorType and ErrorException#vars because we don't have names of message parameters in these fields
        log.warn("Legacy ErrorException was thrown, message will not be displayed to User, please refactor a place where exception was thrown");
        log.error(errorMessageResponse.toString());
        return createResponse(errorMessageResponse, e.getErrorType().getStatusCode());
    }

    @SuppressWarnings("unused")
    @Deprecated
    @ExceptionHandler(ProxyErrorException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleLegacyProxyErrorException(HttpServletRequest request, ProxyErrorException proxyError) {
        final ResponseEntity<SimpleErrorMessageResponse> genericResponse = handleException(request, proxyError);
        log.error(proxyError.toString());
        return createTransitiveErrorResponse(genericResponse.getBody(), proxyError.getHttpStatus());
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(GenericDisplayedException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleGenericDisplayedException(HttpServletRequest request, GenericDisplayedException e) {
        final Map<String, MessageParameter> parameters = Arrays.asList(e.getNamedMessageParameters()).stream().collect(Collectors.toMap(NamedMessageParameter::getParameterName,
                param -> new MessageParameter(param.getParameterType(), param.getParameterValue())));
        final SimpleErrorMessageResponse errorMessageResponse = new SimpleErrorMessageResponse(new ErrorMessage(e.getMessageCode(), parameters));
        generalErrorHandling(request, e, errorMessageResponse);
        log.error(errorMessageResponse.toString());
        return createResponse(errorMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleAccessDeniedException(HttpServletRequest request, AccessDeniedException accessDeniedException) {
        ErrorMessage errorMessage = new ErrorMessage(ACCESS_DENIED_ERROR_CODE);
        SimpleErrorMessageResponse errorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        addDebugInfo(request, accessDeniedException, errorMessageResponse);
        log.error(errorMessageResponse.toString());
        return createResponse(errorMessageResponse, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleException(HttpServletRequest request, Exception e) {
        final ErrorMessage errorMessage = new ErrorMessage(INTERNAL_SERVER_ERROR_CODE);
        final SimpleErrorMessageResponse errorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        generalErrorHandling(request, e, errorMessageResponse);
        log.error(errorMessageResponse.toString());
        return createResponse(errorMessageResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleEntityNotFoundException(HttpServletRequest request, EntityNotFoundException e) {
        Map<String, MessageParameter> parameters = new HashMap<>();
        parameters.put(ENTITY_NOT_FOUND_ID_PARAM, new MessageParameter(STRING, e.getEntityId()));
        parameters.put(ENTITY_NOT_FOUND_TYPE_PARAM, new MessageParameter(LOCALIZED_STRING, e.getEntityTypeName()));
        ErrorMessage errorMessage = new ErrorMessage(ENTITY_NOT_FOUND_ERROR_CODE, parameters);
        SimpleErrorMessageResponse errorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        generalErrorHandling(request, e, errorMessageResponse);
        log.error(errorMessageResponse.toString());
        return createResponse(errorMessageResponse, HttpStatus.NOT_FOUND);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(RestClientException.class)
    @ResponseBody
    private ResponseEntity<?> handleRestClientException(HttpServletRequest request, RestClientException restClientException) {
        if (restClientException instanceof HttpStatusCodeException) {
            HttpStatusCodeException statusException = (HttpStatusCodeException) restClientException;
            if (isKnownResponseFormat(statusException)) {
                final String responseBodyAsString = statusException.getResponseBodyAsString();
                log.error(responseBodyAsString);
                return createTransitiveErrorResponse(responseBodyAsString, statusException.getStatusCode());
            }
            final ResponseEntity<SimpleErrorMessageResponse> genericResponse = handleException(request, restClientException);
            return createTransitiveErrorResponse(genericResponse.getBody(), statusException.getStatusCode());
        }

        final ResponseEntity<SimpleErrorMessageResponse> genericResponse = handleException(request, restClientException);
        return createTransitiveErrorResponse(genericResponse.getBody(), genericResponse.getStatusCode());
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    private ResponseEntity<ObjectValidationErrorResponse> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {

        final List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        final List<FieldValidationMessage> fieldValidationErrors = allErrors.stream()
                .filter(FieldError.class::isInstance)
                .map(error -> new FieldValidationMessage(((FieldError) error).getField(), new ErrorMessage(error.getCode())))
                .collect(Collectors.toList());
        final FieldValidationMessage[] fieldValidationMessages = fieldValidationErrors.stream().toArray(FieldValidationMessage[]::new);

        final ErrorMessage errorMessage = new ErrorMessage(calculateObjectValidationErrorMessage(allErrors));
        final ObjectValidationErrorResponse validationErrorResponse = new ObjectValidationErrorResponse(errorMessage, fieldValidationMessages);
        generalErrorHandling(request, e, validationErrorResponse);
        log.error(validationErrorResponse.toString());
        return createResponse(validationErrorResponse, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleMethodArgumentTypeMismatchException(HttpServletRequest request, MethodArgumentTypeMismatchException e) {
        ErrorMessage errorMessage = new ErrorMessage(INTERNAL_SERVER_ERROR_CODE);
        SimpleErrorMessageResponse simpleErrorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        generalErrorHandling(request, e, simpleErrorMessageResponse);
        log.error(simpleErrorMessageResponse.toString());
        return createResponse(simpleErrorMessageResponse, HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleHttpMessageNotReadableException(HttpServletRequest request, HttpMessageNotReadableException e) {
        ErrorMessage errorMessage = new ErrorMessage(INTERNAL_SERVER_ERROR_CODE);
        SimpleErrorMessageResponse simpleErrorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        generalErrorHandling(request, e, simpleErrorMessageResponse);
        log.error(simpleErrorMessageResponse.toString());
        return createResponse(simpleErrorMessageResponse, HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        ErrorMessage errorMessage = new ErrorMessage(INTERNAL_SERVER_ERROR_CODE);
        SimpleErrorMessageResponse simpleErrorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        generalErrorHandling(request, e, simpleErrorMessageResponse);
        log.error(simpleErrorMessageResponse.toString());
        return createResponse(simpleErrorMessageResponse, HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleOptimisticLockingFailureException(HttpServletRequest request, OptimisticLockingFailureException e) {
        ErrorMessage errorMessage = new ErrorMessage(OPTIMISTIC_LOCKING_ERROR_CODE);
        SimpleErrorMessageResponse simpleErrorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        generalErrorHandling(request, e, simpleErrorMessageResponse);
        log.error(simpleErrorMessageResponse.toString());
        return createResponse(simpleErrorMessageResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseBody
    private ResponseEntity<SimpleErrorMessageResponse> handleUnsupportedOperationException(final HttpServletRequest request,
                                                                                           final UnsupportedOperationException unsupportedOperationException) {
        ErrorMessage errorMessage = new ErrorMessage(INTERNAL_SERVER_ERROR_CODE);
        SimpleErrorMessageResponse errorMessageResponse = new SimpleErrorMessageResponse(errorMessage);
        generalErrorHandling(request, unsupportedOperationException, errorMessageResponse);
        log.error(errorMessageResponse.toString());
        return createResponse(errorMessageResponse, HttpStatus.BAD_REQUEST);
    }
}
