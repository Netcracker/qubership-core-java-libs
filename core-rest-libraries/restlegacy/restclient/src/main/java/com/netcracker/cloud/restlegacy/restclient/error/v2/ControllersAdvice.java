package com.netcracker.cloud.restlegacy.restclient.error.v2;

import com.netcracker.cloud.restlegacy.restclient.error.*;
import com.netcracker.cloud.restlegacy.restclient.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

/**
 * @deprecated use exception handling v2 version
 */
@ConditionalOnProperty(name = "error.handler.version", havingValue = "v1", matchIfMissing = true)
@Deprecated
@ControllerAdvice
public class ControllersAdvice {

    private final static Logger LOGGER = LoggerFactory.getLogger(ControllersAdvice.class);

    private final MessageService messageService;

    private final MessageFormat messageFormat = new MessageFormat("{0}");

    @Autowired
    public ControllersAdvice(MessageService messageService) {
        this.messageService = messageService;
    }


    private void logErrorsDescription(ErrorsDescription errorsDescription) {
        LOGGER.error(errorsDescription.toString());
    }

    @ExceptionHandler(ErrorException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleErrorException(HttpServletRequest request, ErrorException e) {
        String[] parameters = Arrays.stream(e.getVars()).map(parameter -> messageFormat.format(new Object[]{parameter})).toArray(String[]::new);
        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                e.getErrorType().getStatusCode(),
                Collections.singletonList(new ErrorsDescription.ErrorDescription(
                        messageService.getMessageCode(e.getErrorType().getErrorCode()),
                        messageService.getMessage(e.getErrorType().getMessageCode(), e.getVars()),
                        parameters
                )),
                e.getCause() != null ? e.getCause().getClass().getName() : e.getClass().getName(),
                e.getCause() != null ? e.getCause().getMessage() : e.getMessage(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, e.getErrorType().getStatusCode());
    }

    @ExceptionHandler(ProxyErrorException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleProxyErrorException(HttpServletRequest request, ProxyErrorException e) {
        ResponseEntity<ErrorsDescription> responseEntity = e.getResponseEntity();
        if (responseEntity.getBody() != null) {
            ErrorsDescription errorsDescription = responseEntity.getBody();
            errorsDescription.setProxy(true);
            logErrorsDescription(errorsDescription);
            return responseEntity;
        } else {
            return responseEntity;
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException e) {

        final ErrorType commonErrorType = ErrorType.UNPROCESSABLE_ENTITY_COMMON;
        final ErrorType errorType = ErrorType.UNPROCESSABLE_ENTITY_VALIDATION_FAILED;

        List<ErrorsDescription.ErrorDescription> validationErrors = e.getBindingResult().getAllErrors().stream().map(o -> {
            if (FieldError.class.isInstance(o)) {
                Object[] objectsParameters = {((FieldError) o).getField(), messageService.getMessage(o)};
                String[] stringParameters = Arrays.stream(objectsParameters).map(parameter -> messageFormat.format(new Object[]{parameter})).toArray(String[]::new);
                return new ErrorsDescription.ErrorDescription(
                        messageService.getMessageCode(errorType.getErrorCode()),
                        messageService.getMessage(errorType.getMessageCode(), objectsParameters),
                        ((FieldError) o).getField(),
                        stringParameters
                );
            } else {
                return new ErrorsDescription.ErrorDescription(
                        messageService.getMessageCode(commonErrorType.getErrorCode()),
                        messageService.getMessage(commonErrorType.getMessageCode(), messageService.getMessage(o)),
                        new String[]{messageService.getMessage(o)}
                );
            }
        }).collect(Collectors.toList());

        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                errorType.getStatusCode(),
                validationErrors,
                e.getClass().getName(),
                e.getMessage(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, errorType.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleMethodArgumentTypeMismatchException(HttpServletRequest request, MethodArgumentTypeMismatchException e) {

        final ErrorType errorType = ErrorType.BAD_REQUEST;

        Object[] objectsParameters = {e.getName(), e.getValue()};
        String[] stringParameters = Arrays.stream(objectsParameters).map(parameter -> messageFormat.format(new Object[]{parameter})).toArray(String[]::new);

        final List<ErrorsDescription.ErrorDescription> errorDescription = Collections.singletonList(new ErrorsDescription.ErrorDescription(
                messageService.getMessageCode(errorType.getErrorCode()),
                messageService.getMessage(errorType.getMessageCode(), objectsParameters),
                stringParameters
        ));
        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                errorType.getStatusCode(),
                errorDescription,
                e.getClass().getName(),
                e.getMessage(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, errorType.getStatusCode());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleHttpMessageNotReadableException(HttpServletRequest request, HttpMessageNotReadableException e) {

        final ErrorType errorType = ErrorType.BAD_REQUEST;

        final List<ErrorsDescription.ErrorDescription> errorDescription = Collections.singletonList(new ErrorsDescription.ErrorDescription(
                messageService.getMessageCode(errorType.getErrorCode()),
                messageService.getMessage(errorType.getMessageCode())
        ));
        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                errorType.getStatusCode(),
                errorDescription,
                e.getClass().getName(),
                "Can not read http request cause: " + ofNullable(e.getCause()).orElse(e).getClass().getName(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, errorType.getStatusCode());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleOptimisticLockingFailureException(HttpServletRequest request, OptimisticLockingFailureException e) {
        final ErrorType errorType = ErrorType.CONFLICT;

        final List<ErrorsDescription.ErrorDescription> errorDescription = Collections.singletonList(new ErrorsDescription.ErrorDescription(
                messageService.getMessageCode(errorType.getErrorCode()),
                messageService.getMessage(errorType.getMessageCode())
        ));

        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                errorType.getStatusCode(),
                errorDescription,
                e.getClass().getName(),
                e.getMessage(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, errorType.getStatusCode());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleIllegalArgumentException(HttpServletRequest request, Exception e) {
        final ErrorType errorType = ErrorType.BAD_REQUEST;

        final List<ErrorsDescription.ErrorDescription> errorDescription = Collections.singletonList(new ErrorsDescription.ErrorDescription(
                messageService.getMessageCode(errorType.getErrorCode()),
                messageService.getMessage(errorType.getMessageCode())
        ));

        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                errorType.getStatusCode(),
                errorDescription,
                e.getClass().getName(),
                e.getMessage(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, errorType.getStatusCode());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleMissingServletRequestParameterException(HttpServletRequest request, MissingServletRequestParameterException e) {
        final ErrorType errorType = ErrorType.BAD_REQUEST;
        String[] stringParameters = {e.getParameterName()};
        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                errorType.getStatusCode(),
                Collections.singletonList(new ErrorsDescription.ErrorDescription(
                        messageService.getMessageCode(errorType.getErrorCode()),
                        e.getMessage(),
                        stringParameters
                )),
                e.getClass().getName(),
                e.getMessage(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, errorType.getStatusCode());
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleEntityNotFoundException(HttpServletRequest request, EntityNotFoundException e) {
        final ErrorType errorType = ErrorType.NOT_FOUND;
        Object[] objectsParameters = {e.getEntityTypeName(), e.getEntityId()};
        String[] stringParameters = Arrays.stream(objectsParameters).map(parameter -> messageFormat.format(new Object[]{parameter})).toArray(String[]::new);
        final String message = messageService.getMessage(errorType.getMessageCode(), objectsParameters);

        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                errorType.getStatusCode(),
                Collections.singletonList(new ErrorsDescription.ErrorDescription(
                        messageService.getMessageCode(errorType.getErrorCode()),
                        message,
                        stringParameters
                )),
                e.getClass().getName(),
                e.getMessage(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, errorType.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    ResponseEntity<ErrorsDescription> handleException(HttpServletRequest request, Exception e) {
        final ErrorType errorType = ErrorType.INTERNAL_SERVER_ERROR;
        final String message = e.getClass().isAnnotationPresent(DisplayedMessageException.class) ?
                e.getLocalizedMessage() : messageService.getMessage(errorType.getMessageCode());

        final ErrorsDescription errorsDescription = new ErrorsDescription(
                new Date(),
                request.getRequestURI(),
                errorType.getStatusCode(),
                Collections.singletonList(new ErrorsDescription.ErrorDescription(
                        messageService.getMessageCode(errorType.getErrorCode()),
                        message
                )),
                e.getClass().getName(),
                e.getMessage(),
                getStackTrace(e));
        logErrorsDescription(errorsDescription);
        return new ResponseEntity<>(errorsDescription, errorType.getStatusCode());
    }

}
