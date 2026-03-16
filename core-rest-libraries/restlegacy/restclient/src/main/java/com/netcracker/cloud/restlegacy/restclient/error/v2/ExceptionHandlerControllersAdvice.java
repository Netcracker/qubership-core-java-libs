package com.netcracker.cloud.restlegacy.restclient.error.v2;

import com.netcracker.cloud.restlegacy.restclient.error.DisplayedMessageException;
import com.netcracker.cloud.restlegacy.restclient.error.EntityNotFoundException;
import com.netcracker.cloud.restlegacy.restclient.error.ErrorException;
import com.netcracker.cloud.restlegacy.restclient.error.ProxyErrorException;
import com.netcracker.cloud.restlegacy.restclient.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.stream.Collectors;

import static com.netcracker.cloud.restlegacy.restclient.error.ErrorMessageCodes.*;
import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static com.netcracker.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2;
import static com.netcracker.cloud.restlegacy.restclient.error.v2.ResponseAdviceExceptionHelper.*;

@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@ConditionalOnProperty(name = ERROR_HANDLER_VERSION_CONDITION_PROPERTY, havingValue = VERSION_2)
class ExceptionHandlerControllersAdvice {

    @Autowired
    private final MessageService messageService;

    /**
     * @deprecated Only for legacy usages. We need remove all places where we create {@link ErrorException}
     */
    @SuppressWarnings("unused")
    @Deprecated
    @ExceptionHandler(ErrorException.class)
    @ResponseBody
    private ResponseEntity<PlainTextErrorDescription> handleLegacyErrorException(HttpServletRequest request, ErrorException e) {
        PlainTextErrorDescription errorDescription = new PlainTextErrorDescription(messageService.getMessage(e.getErrorType().getMessageCode(), e.getVars()));
        generalErrorHandling(request, e, errorDescription);
        return createResponse(errorDescription, e.getErrorType().getStatusCode());
    }


    @SuppressWarnings("unused")
    @ExceptionHandler(ProxyErrorException.class)
    @ResponseBody
    private ResponseEntity<?> handleLegacyProxyErrorException(HttpServletRequest request, ProxyErrorException proxyErr) {
        final ResponseEntity<PlainTextErrorDescription> genericResponse = handleException(request, proxyErr);
        return createTransitiveErrorResponse(genericResponse.getBody(), proxyErr.getHttpStatus());
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
            //If you want to show userMessage from 3-rd party service, catch RestClientException when you sent request
            // and throw exception annotated by DisplayedMessageException with required message
            final ResponseEntity<PlainTextErrorDescription> genericResponse = handleException(request, restClientException);
            return createTransitiveErrorResponse(genericResponse.getBody(), statusException.getStatusCode());
        }

        final ResponseEntity<PlainTextErrorDescription> genericResponse = handleException(request, restClientException);
        return createTransitiveErrorResponse(genericResponse.getBody(), genericResponse.getStatusCode());
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    private ResponseEntity<PlainTextErrorDescription> handleEntityNotFoundException(HttpServletRequest request, EntityNotFoundException e) {
        final String message = messageService.getMessage(ENTITY_NOT_FOUND_ERROR_CODE, e.getEntityTypeName(), e.getEntityId());
        final PlainTextErrorDescription errorDescription = new PlainTextErrorDescription(message);
        generalErrorHandling(request, e, errorDescription);
        return createResponse(errorDescription, HttpStatus.NOT_FOUND);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    private ResponseEntity<ObjectValidationErrorDescription> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {

        final List<ObjectError> allErrors = e.getBindingResult().getAllErrors();
        final List<ObjectValidationErrorDescription.FieldValidationErrorDescription> fieldValidationErrors = allErrors.stream()
                .filter(FieldError.class::isInstance)
                .map(error -> new ObjectValidationErrorDescription.FieldValidationErrorDescription(((FieldError) error).getField(), messageService.getMessage(error)))
                .collect(Collectors.toList());

        final ObjectValidationErrorDescription errorDescription = new ObjectValidationErrorDescription(fieldValidationErrors);
        allErrors.stream()
                .filter(error -> !FieldError.class.isInstance(error))
                .findFirst()
                .ifPresent(objectError -> errorDescription.setObjectValidationMessage(messageService.getMessage(objectError)));

        generalErrorHandling(request, e, errorDescription);
        return createResponse(errorDescription, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    private ResponseEntity<PlainTextErrorDescription> handleMethodArgumentTypeMismatchException(HttpServletRequest request, MethodArgumentTypeMismatchException e) {

        final PlainTextErrorDescription errorDescription = new PlainTextErrorDescription(getInternalServerErrorMessage());
        generalErrorHandling(request, e, errorDescription);
        return createResponse(errorDescription, HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    private ResponseEntity<PlainTextErrorDescription> handleHttpMessageNotReadableException(HttpServletRequest request, HttpMessageNotReadableException e) {
        final PlainTextErrorDescription errorDescription = new PlainTextErrorDescription(getInternalServerErrorMessage());
        generalErrorHandling(request, e, errorDescription);
        return createResponse(errorDescription, HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    private ResponseEntity<PlainTextErrorDescription> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException e) {
        final PlainTextErrorDescription errorDescription = new PlainTextErrorDescription(getInternalServerErrorMessage());
        generalErrorHandling(request, e, errorDescription);
        return createResponse(errorDescription, HttpStatus.BAD_REQUEST);
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseBody
    private ResponseEntity<PlainTextErrorDescription> handleOptimisticLockingFailureException(HttpServletRequest request, OptimisticLockingFailureException e) {
        final PlainTextErrorDescription errorDescription = new PlainTextErrorDescription(messageService.getMessageCode(DB_IN_CONFLICT_ERROR_CODE));
        generalErrorHandling(request, e, errorDescription);
        return createResponse(errorDescription, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    private ResponseEntity<PlainTextErrorDescription> handleException(HttpServletRequest request, Exception e) {

        final String message = e.getClass().isAnnotationPresent(DisplayedMessageException.class) ? e.getLocalizedMessage() : getInternalServerErrorMessage();
        final PlainTextErrorDescription errorDescription = new PlainTextErrorDescription(message);
        generalErrorHandling(request, e, errorDescription);
        return createResponse(errorDescription, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static void generalErrorHandling(HttpServletRequest request, Exception e, HasDebugInfo<DebugInfo> errorDescription) {
        addDebugInfo(request, e, errorDescription);
        log.error(errorDescription.toString());
    }

    private String getInternalServerErrorMessage() {
        return messageService.getMessageCode(INTERNAL_SERVER_ERROR_CODE);
    }
}
