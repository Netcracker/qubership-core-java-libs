package com.netcracker.cloud.restlegacy.restclient.error;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Date;

import static com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1.NamedMessageParameter.date;
import static com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1.NamedMessageParameter.string;
import static java.util.Objects.isNull;

@RestController
public class TestExceptionHandlingRestController {
    public static final String THROW_GENERIC_DISPLAYED_EXCEPTION_METHOD = "/throwGenericDisplayedException";
    public static final String THROW_GENERIC_DISPLAYED_EXCEPTION_V2_1_METHOD = "/throwGenericDisplayedExceptionV2_1";
    public static final String THROW_EXCEPTION_WITH_DATE_PARAMETER_METHOD = "/throwExceptionWithDateParameter";
    public static final String THROW_DISPLAYED_MESSAGE_EXCEPTION_METHOD = "/throwCustomExceptionMarkedBySpecialAnnotation";
    public static final String THROW_NOT_DISPLAYED_MESSAGE_EXCEPTION_METHOD = "/throwCustomExceptionNotMarkedBySpecialAnnotation";
    public static final String THROW_ILLEGAL_STATE_EXCEPTION_METHOD = "/throwIllegalStateException";
    public static final String THROW_ILLEGAL_ARGUMENT_EXCEPTION_METHOD = "/throwIllegalArgumentException";
    public static final String THROW_OPTIMISTIC_LOCKING_EXCEPTION_METHOD = "/throwOptimisticLockingFailureException";
    public static final String THROW_ACCESS_DENIED_EXCEPTION_METHOD = "/throwAccessDeniedException";
    public static final String THROW_UNSUPPORTED_OPERATION_EXCEPTION_METHOD = "/throwUnsupportedOperationException";
    public static final String THROW_ENTITY_NOT_FOUND_METHOD = "/throwEntityNotFound";
    public static final String THROW_ANY_EXCEPTION_METHOD = "/throwAnyException";
    public static final String THROW_LEGACY_ERROR_EXCEPTION_WITH_404 = "/throwLegacyErrorExceptionWith404";
    public static final String THROW_OBJECT_VALIDATION_EXCEPTION = "/throwObjectValidationException";
    public static final String THROW_METHOD_ARGUMENT_TYPE_MISMATCH = "/throwMethodArgumentTypeMismatch";
    public static final String THROW_ENTIRE_OBJECT_VALIDATION_EXCEPTION = "/throwEntireObjectValidationException";

    public static final String REDIRECT_TO_SERVICE_THAT_FAILS_FIRST_TIME = "/redirectToServiceThatFailsSometimes";
    public static final String SERVICE_METHOD_METHOD_THAT_FAILS_FIRST_TIME = "/serviceMethodThatFailsFirstTime";
    public static final String SERVICE_METHOD_METHOD_THAT_FAILS_FIRST_TIME_EXPECTED_RESULT = "SERVICE_METHOD_METHOD_THAT_FAILS_FIRST_TIME_EXPECTED_RESULT";

    public static final String REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_FIRST_TIME = "/redirectToTransitiveServiceThatFailsFirstTime";
    public static final String FIRST_TRANSITIVE_LEVEL_METHOD = "/firstTransitiveLevelMethod";
    public static final String SECOND_TRANSITIVE_LEVEL_METHOD = "/secondTransitiveLevelMethod";
    public static final String SECOND_TRANSITIVE_LEVEL_METHOD_EXPECTED_RESULT = "SECOND_TRANSITIVE_LEVEL_METHOD_EXPECTED_RESULT";

    public static final String REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_ALWAYS = "/redirectToTransitiveServiceThatFailsAlways";
    public static final String FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD = "/firstTransitiveLevelMethodThatRedirectsToAlwaysFailedMethod";
    public static final String SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS = "/secondTransitiveLevelMethodThatFailsAlways";

    public static final String REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_ALWAYS_V2_1 = "/redirectToTransitiveServiceThatFailsAlwaysV2_1";
    public static final String FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD_V2_1 = "/firstTransitiveLevelMethodThatRedirectsToAlwaysFailedMethodV2_1";
    public static final String SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS_V2_1 = "/secondTransitiveLevelMethodThatFailsAlwaysV2_1";

    public static final String REDIRECT_TO_FAILED_WITH_400_METHOD = "/redirectToFailedWith400Method";
    public static final String FAILED_WITH_400_METHOD = "/failedWith400Method";

    public static final String REDIRECT_TO_FAILED_WITH_401_METHOD = "/redirectToFailedWith401Method";
    public static final String FAILED_WITH_401_METHOD = "/failedWith401Method";

    public static final String REDIRECT_TO_FAILED_WITH_403_METHOD = "/redirectToFailedWith403Method";
    public static final String FAILED_WITH_403_METHOD = "/failedWith403Method";

    public static final String REDIRECT_TO_FAILED_WITH_404_METHOD = "/redirectToFailedWith404Method";
    public static final String FAILED_WITH_404_METHOD = "/failedWith404Method";

    public static final String REDIRECT_TO_FAILED_WITH_409_METHOD = "/redirectToFailedWith409Method";
    public static final String FAILED_WITH_409_METHOD = "/failedWith409Method";

    public static final String REDIRECT_TO_FAILED_WITH_422_METHOD = "/redirectToFailedWith422Method";
    public static final String FAILED_WITH_422_METHOD = "/failedWith422Method";

    public static final String TRANSITIVE_LEVEL_METHOD_ERROR_MESSAGE = "Temporary Error";
    public static final String NOT_FOUNDED_ENTITY_ID = "3f2cc790-b812-4a8d-a57e-ded284343dba";
    public static final String ENTITY_TYPE = "myCustomEntity";
    public static final String CUSTOM_USER_MESSAGE = "Custom User message";
    public static final String FAILED_FIELD = "failedField";
    public static final String FIELD_VALIDATION_MESSAGE = "fieldValidationMessage";
    public static final String OBJECT_VALIDATION_MESSAGE = "objectValidationMessage";
    public static final String INTEGER_REQUEST_PARAMETER_NAME = "integerParam";

    public static final String GENERIC_DISPLAYED_EXCEPTION_V2_1_MESSAGE_CODE = "Some message with {{messageParameterName}} parameter";
    public static final String GENERIC_DISPLAYED_EXCEPTION_V2_1_STRING_MESSAGE_PARAMETER_NAME = "messageParameterName";
    public static final String GENERIC_DISPLAYED_EXCEPTION_V2_1_STRING_MESSAGE_PARAMETER_VALUE = "messageParameterValue";

    public static final String MESSAGE_DATE_PARAMETER_NAME = "messageDateParameterName";
    public static final Date MESSAGE_DATE_PARAMETER_VALUE = new Date();

    public static final String TRANSITIVE_LEVEL_METHOD_MESSAGE_CODE = "message code with {{param name}} placeholders";
    public static final String TRANSITIVE_LEVEL_METHOD_MESSAGE_PARAM_NAME = "param name";
    public static final String TRANSITIVE_LEVEL_METHOD_MESSAGE_PARAM_VALUE = "param value";
    public static final String VALIDATION_FIELD_FAILED = "Validation field failed";
    public static final String VALIDATION_OBJECT_FAILED = "Validation object failed";

    @Autowired
    private TestExceptionHandlingConfiguration.TestRestClient restClient;

    private boolean shouldFailForServiceMethodThatFailsFirstTime = true;
    private boolean shouldFailForSecondTransitiveLevelMethod = true;

    @GetMapping(value = THROW_ENTITY_NOT_FOUND_METHOD)
    public void throwOEntityNotFound() {
        throw new EntityNotFoundException(ENTITY_TYPE, NOT_FOUNDED_ENTITY_ID);
    }

    @GetMapping(value = THROW_GENERIC_DISPLAYED_EXCEPTION_METHOD)
    public void throwGenericDisplayedException() {
        throw new GenericDisplayedException(CUSTOM_USER_MESSAGE);
    }

    @GetMapping(value = THROW_EXCEPTION_WITH_DATE_PARAMETER_METHOD)
    public void throwExceptionWithDateParameter() {
        throw new com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1.GenericDisplayedException(
                GENERIC_DISPLAYED_EXCEPTION_V2_1_MESSAGE_CODE,
                date(MESSAGE_DATE_PARAMETER_NAME, MESSAGE_DATE_PARAMETER_VALUE));
    }

    @GetMapping(value = THROW_GENERIC_DISPLAYED_EXCEPTION_V2_1_METHOD)
    public void throwGenericDisplayedExceptionV2_1() {
        throw new com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1.GenericDisplayedException(
                GENERIC_DISPLAYED_EXCEPTION_V2_1_MESSAGE_CODE,
                string(GENERIC_DISPLAYED_EXCEPTION_V2_1_STRING_MESSAGE_PARAMETER_NAME,
                        GENERIC_DISPLAYED_EXCEPTION_V2_1_STRING_MESSAGE_PARAMETER_VALUE));
    }

    @GetMapping(value = THROW_DISPLAYED_MESSAGE_EXCEPTION_METHOD)
    public void throwCustomExceptionMarkedBySpecialAnnotation() {
        throw new MyCustomAnnotatedException(CUSTOM_USER_MESSAGE);
    }

    @GetMapping(value = THROW_NOT_DISPLAYED_MESSAGE_EXCEPTION_METHOD)
    public void throwCustomExceptionNotMarkedBySpecialAnnotation() {
        throw new MyCustomNotAnnotatedException(CUSTOM_USER_MESSAGE);
    }

    @GetMapping(value = THROW_OPTIMISTIC_LOCKING_EXCEPTION_METHOD)
    public void throwOptimisticLockingFailureException() {
        throw new OptimisticLockingFailureException("user message");
    }

    @GetMapping(value = THROW_ILLEGAL_STATE_EXCEPTION_METHOD)
    public void throwIllegalStateException() {
        throw new IllegalStateException(CUSTOM_USER_MESSAGE);
    }

    @GetMapping(value = THROW_ILLEGAL_ARGUMENT_EXCEPTION_METHOD)
    public void throwIllegalArgumentException() {
        throw new IllegalArgumentException();
    }

    @GetMapping(value = THROW_ANY_EXCEPTION_METHOD)
    public void throwAnyException() {
        throw new NullPointerException();
    }

    @GetMapping(value = THROW_LEGACY_ERROR_EXCEPTION_WITH_404)
    public void throwLegacyErrorExceptionWith404() throws ErrorException {
        throw new ErrorException(new RuntimeException(), ErrorType.NOT_FOUND, ENTITY_TYPE, NOT_FOUNDED_ENTITY_ID);
    }

    @GetMapping(value = THROW_OBJECT_VALIDATION_EXCEPTION)
    public void throwObjectValidationException(@RequestBody @Valid TestBody body) {
    }

    @GetMapping(value = THROW_ENTIRE_OBJECT_VALIDATION_EXCEPTION)
    public void throwEntireObjectValidationException(@RequestBody @Valid TestBody2 body) {
    }

    @GetMapping(value = THROW_METHOD_ARGUMENT_TYPE_MISMATCH)
    public void throwMethodArgumentTypeMismatch(@RequestParam(INTEGER_REQUEST_PARAMETER_NAME) int param) {
        System.out.println("here");
    }

    @GetMapping(value = THROW_ACCESS_DENIED_EXCEPTION_METHOD)
    public void throwAccessDeniedExceptionMethodException() {
        throw new AccessDeniedException("user message");
    }

    @GetMapping(value = THROW_UNSUPPORTED_OPERATION_EXCEPTION_METHOD)
    public void throwUnsupportedOperationMethodException() {
        throw new UnsupportedOperationException("user message");
    }

    @GetMapping(value = REDIRECT_TO_SERVICE_THAT_FAILS_FIRST_TIME)
    public String redirectToMethodThatFailsFirstTime() {
        return restClient.sendRequest(SERVICE_METHOD_METHOD_THAT_FAILS_FIRST_TIME);
    }

    @GetMapping(value = SERVICE_METHOD_METHOD_THAT_FAILS_FIRST_TIME)
    public String serviceMethodThatFailsFirstTime() {
        if (shouldFailForServiceMethodThatFailsFirstTime) {
            shouldFailForServiceMethodThatFailsFirstTime = false;
            throw new IllegalStateException();
        }
        return SERVICE_METHOD_METHOD_THAT_FAILS_FIRST_TIME_EXPECTED_RESULT;
    }

    @GetMapping(value = REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_FIRST_TIME)
    public String redirectToTransitiveServiceThatFailsFirstTime() {
        return restClient.sendRequest(FIRST_TRANSITIVE_LEVEL_METHOD);
    }

    @GetMapping(value = FIRST_TRANSITIVE_LEVEL_METHOD)
    public String firstTransitiveLevelMethod() {
        return restClient.sendRequest(SECOND_TRANSITIVE_LEVEL_METHOD);
    }

    @GetMapping(value = SECOND_TRANSITIVE_LEVEL_METHOD)
    public String secondTransitiveLevelMethod() {
        if (shouldFailForSecondTransitiveLevelMethod) {
            shouldFailForSecondTransitiveLevelMethod = false;
            throw new IllegalStateException(TRANSITIVE_LEVEL_METHOD_ERROR_MESSAGE);
        }
        return SECOND_TRANSITIVE_LEVEL_METHOD_EXPECTED_RESULT;
    }

    @GetMapping(value = REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_ALWAYS)
    public String redirectToTransitiveServiceThatFailsAlways() {
        return restClient.sendRequest(FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD);
    }

    @GetMapping(value = FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD)
    public String firstTransitiveLevelMethodThatRedirectsToAlwaysFailedMethod() {
        return restClient.sendRequest(SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS);
    }

    @GetMapping(value = SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS)
    public String secondTransitiveLevelMethodThatFailsAlways() {
        throw new GenericDisplayedException(TRANSITIVE_LEVEL_METHOD_ERROR_MESSAGE);
    }

    @GetMapping(value = REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_ALWAYS_V2_1)
    public String redirectToTransitiveServiceThatFailsAlwaysV2_1() {
        return restClient.sendRequest(FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD_V2_1);
    }

    @GetMapping(value = FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD_V2_1)
    public String firstTransitiveLevelMethodThatRedirectsToAlwaysFailedMethodV2_1() {
        return restClient.sendRequest(SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS_V2_1);
    }

    @GetMapping(value = SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS_V2_1)
    public String secondTransitiveLevelMethodThatFailsAlwaysV2_1() {
        throw new com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1.GenericDisplayedException(TRANSITIVE_LEVEL_METHOD_MESSAGE_CODE,
                string(TRANSITIVE_LEVEL_METHOD_MESSAGE_PARAM_NAME, TRANSITIVE_LEVEL_METHOD_MESSAGE_PARAM_VALUE));
    }

    @GetMapping(value = REDIRECT_TO_FAILED_WITH_400_METHOD)
    public String redirectToFailedWith400Method() {
        return restClient.sendRequest(FAILED_WITH_400_METHOD);
    }

    @GetMapping(value = FAILED_WITH_400_METHOD)
    public ResponseEntity<String> failedWith400Method() {
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @GetMapping(value = REDIRECT_TO_FAILED_WITH_401_METHOD)
    public String redirectToFailedWith401Method() {
        return restClient.sendRequest(FAILED_WITH_401_METHOD);
    }

    @GetMapping(value = FAILED_WITH_401_METHOD)
    public ResponseEntity<String> failedWith401Method() {
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping(value = REDIRECT_TO_FAILED_WITH_403_METHOD)
    public String redirectToFailedWith403Method() {
        return restClient.sendRequest(FAILED_WITH_403_METHOD);
    }

    @GetMapping(value = FAILED_WITH_403_METHOD)
    public ResponseEntity<String> failedWith403Method() {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @GetMapping(value = REDIRECT_TO_FAILED_WITH_404_METHOD)
    public String redirectToFailedWith404Method() {
        return restClient.sendRequest(FAILED_WITH_404_METHOD);
    }

    @GetMapping(value = FAILED_WITH_404_METHOD)
    public ResponseEntity<String> failedWith404Method() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @GetMapping(value = REDIRECT_TO_FAILED_WITH_409_METHOD)
    public String redirectToFailedWith409Method() {
        return restClient.sendRequest(FAILED_WITH_409_METHOD);
    }

    @GetMapping(value = FAILED_WITH_409_METHOD)
    public ResponseEntity<String> failedWith409Method() {
        return new ResponseEntity<>(HttpStatus.CONFLICT);
    }

    @GetMapping(value = REDIRECT_TO_FAILED_WITH_422_METHOD)
    public String redirectToFailedWith422Method() {
        return restClient.sendRequest(FAILED_WITH_422_METHOD);
    }

    @GetMapping(value = FAILED_WITH_422_METHOD)
    public ResponseEntity<String> failedWith422Method() {
        return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @DisplayedMessageException
    private static class MyCustomAnnotatedException extends RuntimeException {

        MyCustomAnnotatedException(String userMessage) {
            super(userMessage);
        }
    }

    private static class MyCustomNotAnnotatedException extends RuntimeException {

        MyCustomNotAnnotatedException(String userMessage) {
            super(userMessage);
        }
    }

    @Data
    static class TestBody {
        String failedField;
    }

    @Data
    static class TestBody2 {
    }

    @InitBinder
    public void dataBindings(final WebDataBinder binder,
                             final HttpServletRequest request) throws Exception {
        final Object target = binder.getTarget();
        if (!isNull(target) && TestBody.class.equals(target.getClass())) {
            binder.addValidators(new Validator() {
                @Override
                public boolean supports(Class<?> clazz) {
                    return TestBody.class.equals(clazz);
                }

                @Override
                public void validate(Object target, Errors errors) {
                    errors.rejectValue(FAILED_FIELD, VALIDATION_FIELD_FAILED, FIELD_VALIDATION_MESSAGE);
                }
            });
        } else {
            binder.addValidators(new Validator() {
                @Override
                public boolean supports(Class<?> clazz) {
                    return TestBody2.class.equals(clazz);
                }

                @Override
                public void validate(Object target, Errors errors) {
                    errors.reject(VALIDATION_OBJECT_FAILED, OBJECT_VALIDATION_MESSAGE);
                }
            });
        }
    }
}
