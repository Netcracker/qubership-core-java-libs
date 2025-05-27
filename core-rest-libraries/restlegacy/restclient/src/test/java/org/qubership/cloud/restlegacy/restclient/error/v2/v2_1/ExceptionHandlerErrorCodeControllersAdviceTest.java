package org.qubership.cloud.restlegacy.restclient.error.v2.v2_1;

import org.hamcrest.*;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restlegacy.restclient.app.TestConfig;
import org.qubership.cloud.restlegacy.restclient.error.ExceptionHandlerControllersAdviceBase;
import org.qubership.cloud.restlegacy.restclient.error.TestExceptionHandlingConfiguration;
import org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithCustomException;
import org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithV1ExceptionModel;
import org.qubership.cloud.restlegacy.restclient.error.v2.CustomExceptionHandler;
import org.qubership.cloud.restlegacy.restclient.error.v2.ExceptionHandlingV2MainConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.retry.RetryStatistics;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.qubership.cloud.restlegacy.restclient.error.TestExceptionHandlingRestController.*;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2_1;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithCustomException.CUSTOM_DATA_FROM_CUSTOM_EXCEPTION;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithCustomException.THROW_CUSTOM_EXEPTION_WITH_CUSTOM_DATA;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithV1ExceptionModel.*;
import static org.qubership.cloud.restlegacy.restclient.error.v2.v2_1.ErrorMessageCodes.*;
import static org.qubership.cloud.restlegacy.restclient.error.v2.v2_1.MessageParameterTypes.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {TestConfig.class,
                TestExceptionHandlingConfiguration.class,
                ControllerWithV1ExceptionModel.class,
                CustomExceptionHandler.class,
                ControllerWithCustomException.class},
        properties = {ERROR_HANDLER_VERSION_CONDITION_PROPERTY + "=" + VERSION_2_1})
@AutoConfigureJsonTesters
class ExceptionHandlerErrorCodeControllersAdviceTest extends ExceptionHandlerControllersAdviceBase {

    @Autowired
    private JacksonTester<SimpleErrorMessageResponse> simpleMessageJsonTester;

    @Autowired
    private JacksonTester<ObjectValidationErrorResponse> objectValidationJsonTester;

    private static Matcher<SimpleErrorMessageResponse> hasMessageCode(final String messageCode) {
        return hasErrorMessage(equalTo(messageCode), null);
    }

    private static Matcher<ErrorMessage> hasErrorMessageCode(final String messageCode) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText(", has messageCode ").appendText(messageCode);
            }

            @Override
            protected boolean matchesSafely(final ErrorMessage item, final Description mismatchDescription) {
                boolean result = true;

                if (!messageCode.equals(item.getMessageCode())) {
                    result = false;
                    mismatchDescription.appendText(" has different messageCode ").appendValue(item.getMessageCode());
                }
                return result;
            }
        };
    }

    private static Matcher<ObjectValidationErrorResponse> hasValidationErrors(final Matcher<Iterable<? extends FieldValidationMessage>> matcher) {
        return new FeatureMatcher<ObjectValidationErrorResponse, Iterable<FieldValidationMessage>>
                (matcher, "has validationErrors", "validationErrors") {
            @Override
            protected List<FieldValidationMessage> featureValueOf(final ObjectValidationErrorResponse actual) {
                return Arrays.asList(actual.getFieldValidationMessages());
            }
        };
    }

    private static Matcher<ObjectValidationErrorResponse> hasObjectValidationMessage(Matcher<ErrorMessage> matcher) {
        return new FeatureMatcher<>
                (matcher, "has validationErrors", "validationErrors") {
            @Override
            protected ErrorMessage featureValueOf(final ObjectValidationErrorResponse actual) {
                return actual.getErrorMessage();
            }
        };
    }

    private static Matcher<FieldValidationMessage> equalToMessageCode(final String failedField, final String messageCode) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(final FieldValidationMessage item, final Description mismatchDescription) {
                boolean result = true;

                if (!failedField.equals(item.getFieldName())) {
                    result = false;
                    mismatchDescription.appendText(" has different fieldName ").appendValue(item.getFieldName());
                }

                if (!messageCode.equals(item.getValidationMessage().getMessageCode())) {
                    result = false;
                    mismatchDescription.appendText(" has different messageCode ").appendValue(item.getValidationMessage().getMessageCode());
                }
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has fieldName ").appendText(failedField).appendText(", has messageCode ").appendText(messageCode);
            }
        };
    }

    private static Matcher<SimpleErrorMessageResponse> hasErrorMessage(final Matcher<String> messageCodeMatcher,
                                                                       final Matcher<Map<? extends String, ? extends MessageParameter>> messageParametersMatcher) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(final SimpleErrorMessageResponse simpleErrorMessageResponse, final Description description) {
                final ErrorMessage errorMessage = simpleErrorMessageResponse.getErrorMessage();
                boolean result = true;
                if (messageCodeMatcher != null && !messageCodeMatcher.matches(errorMessage.getMessageCode())) {
                    description.appendText(" has different message code: ").appendText(errorMessage.getMessageCode());
                    result = false;
                }
                if (messageParametersMatcher != null && !messageParametersMatcher.matches(errorMessage.getParameters())) {
                    description.appendText(" has different message parameters: ").appendValue(errorMessage.getParameters());
                    result = false;
                }
                return result;
            }

            @Override
            public void describeTo(final Description description) {
                if (messageCodeMatcher != null) {
                    description.appendText(" messageCode ").appendDescriptionOf(messageCodeMatcher);
                }
                if (messageParametersMatcher != null) {
                    description.appendText(" message parameters ").appendDescriptionOf(messageParametersMatcher);
                }
            }
        };
    }

    private static Matcher<SimpleErrorMessageResponse> hasMessageParameter(final String name, final Object value, final String type) {
        return hasErrorMessage(null, Matchers.hasEntry(equalTo(name), messageParameter(equalTo(type), equalTo(value))));
    }

    private static Matcher<MessageParameter> messageParameter(final Matcher<String> typeMatcher, final Matcher<Object> valueMatcher) {
        return new TypeSafeDiagnosingMatcher<MessageParameter>() {
            @Override
            protected boolean matchesSafely(final MessageParameter messageParameter, final Description description) {

                boolean result = true;
                if (typeMatcher != null && !typeMatcher.matches(messageParameter.getType())) {
                    description.appendText(" has different type: ").appendText(messageParameter.getType());
                    result = false;
                }
                if (valueMatcher != null && !valueMatcher.matches(messageParameter.getValue())) {
                    description.appendText(" has different value: ").appendValue(messageParameter.getValue());
                    result = false;
                }
                return result;
            }

            @Override
            public void describeTo(final Description description) {
                if (typeMatcher != null) {
                    description.appendText(" type ").appendDescriptionOf(typeMatcher);
                }
                if (valueMatcher != null) {
                    description.appendText(" value ").appendDescriptionOf(valueMatcher);
                }
            }
        };
    }

    @Test
    void useV2VersionOfErrorHandlingIfItIsSpecifiedDirectly() {
        assertNotNull(context.getBean(ExceptionHandlingV2MainConfiguration.class));
        assertNotNull(context.getBean(ExceptionHandlingV2_1Configuration.class));
    }

    @Test
    void wePropagateOnlyHttCodeFromErrorTypeToResponseIfThrowLegacyErrorException() throws Exception {
        mockMvc.perform(get(THROW_LEGACY_ERROR_EXCEPTION_WITH_404))
                .andExpect(body().isSimpleMessageError(hasMessageCode(INTERNAL_SERVER_ERROR_CODE)))
                .andExpect(status().isNotFound());
    }

    @Test
    void propagateUserMessageCodeWithParamsToResponseIfGenericDisplayedExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_GENERIC_DISPLAYED_EXCEPTION_V2_1_METHOD))
                .andExpect(status().isInternalServerError())
                .andExpect(body().isSimpleMessageError(allOf(
                        hasMessageCode(GENERIC_DISPLAYED_EXCEPTION_V2_1_MESSAGE_CODE),
                        hasMessageParameter(
                                GENERIC_DISPLAYED_EXCEPTION_V2_1_STRING_MESSAGE_PARAMETER_NAME,
                                GENERIC_DISPLAYED_EXCEPTION_V2_1_STRING_MESSAGE_PARAMETER_VALUE,
                                STRING))));
    }

    @Test
    void weSendDateMessageParameterInRFC1123() throws Exception {
        String expectedDateValue = MESSAGE_DATE_PARAMETER_VALUE.toInstant().atZone(ZoneId.systemDefault()).format(RFC_1123_DATE_TIME);
        mockMvc.perform(get(THROW_EXCEPTION_WITH_DATE_PARAMETER_METHOD))
                .andExpect(status().isInternalServerError())
                .andExpect(body().isSimpleMessageError(hasMessageParameter(MESSAGE_DATE_PARAMETER_NAME, expectedDateValue, DATE)));
    }

    @Test
    void propagateSpecialUserMessageWithEntityNameAndIdToResponseIfObjectNotFoundWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ENTITY_NOT_FOUND_METHOD))
                .andExpect(body().isSimpleMessageError(allOf(
                        hasMessageCode(ENTITY_NOT_FOUND_ERROR_CODE),
                        hasMessageParameter(ENTITY_NOT_FOUND_ID_PARAM, NOT_FOUNDED_ENTITY_ID, STRING),
                        hasMessageParameter(ENTITY_NOT_FOUND_TYPE_PARAM, ENTITY_TYPE, LOCALIZED_STRING))));
    }

    @Test
    void propagateGeneralInternalErrorMessageToResponseIfErrorClassDoesNotExtendsGenericDisplayedException() throws Exception {
        mockMvc.perform(get(THROW_NOT_DISPLAYED_MESSAGE_EXCEPTION_METHOD))
                .andExpect(status().isInternalServerError())
                .andExpect(body().isSimpleMessageError(hasMessageCode(INTERNAL_SERVER_ERROR_CODE)));
    }

    @Test
    void propagateGeneralInternalErrorMessageToResponseIfStandardJavaExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ILLEGAL_STATE_EXCEPTION_METHOD))
                .andExpect(body().isSimpleMessageError(hasMessageCode(INTERNAL_SERVER_ERROR_CODE)));
    }

    @Test
    void propagateConflictErrorMessageToResponseIfOptimisticLockingFailureExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_OPTIMISTIC_LOCKING_EXCEPTION_METHOD))
                .andExpect(body().isSimpleMessageError(hasMessageCode(OPTIMISTIC_LOCKING_ERROR_CODE)));
    }

    @Test
    void propagateAccessDeniedErrorMessageToResponseIfAccessDeniedExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ACCESS_DENIED_EXCEPTION_METHOD))
                .andExpect(status().isForbidden())
                .andExpect(body().isSimpleMessageError(hasMessageCode(ACCESS_DENIED_ERROR_CODE)));
    }

    @Test
    void sendInternalErrorMessageToResponseIfUnsupportedOperationExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_UNSUPPORTED_OPERATION_EXCEPTION_METHOD))
                .andExpect(status().isBadRequest())
                .andExpect(body().isSimpleMessageError(hasMessageCode(INTERNAL_SERVER_ERROR_CODE)));
    }

    @Test
    void propagateDefaultOperationValidationErrorMessageToResponseIfOnlyHaveFieldValidationErrors() throws Exception {
        mockMvc.perform(get(THROW_OBJECT_VALIDATION_EXCEPTION).content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(body().isSimpleMessageError(hasMessageCode(OPERATION_VALIDATION_ERROR_CODE)));
    }

    @Test
    void addFieldValidationDetailsInResponseIfValidationFailed() throws Exception {
        mockMvc.perform(get(THROW_OBJECT_VALIDATION_EXCEPTION).content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(body().isObjectValidationError(hasValidationErrors(contains(equalToMessageCode(FAILED_FIELD, VALIDATION_FIELD_FAILED)))));
    }

    @Test
    void addObjectValidationMessageInResponseIfEntireObjectWasRejected() throws Exception {
        mockMvc.perform(get(THROW_ENTIRE_OBJECT_VALIDATION_EXCEPTION).content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(body().isObjectValidationError(hasObjectValidationMessage(hasErrorMessageCode(VALIDATION_OBJECT_FAILED))));
    }

    @Test
    void dontRetryRequestToSystemThatUsesV1ModelIfFailWasInTransitiveService() {
        restClient.safelySendRequest(SYSTEM_WITH_V1_MODEL + TRANSITIVE_FAILED_REQUEST);

        final RetryStatistics statisticForRequest = getStatisticForRequest(SYSTEM_WITH_V1_MODEL + TRANSITIVE_FAILED_REQUEST);
        MatcherAssert.assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    void retryRequestToSystemThatUsesV1ModelIfFailWasInDirectService() {
        restClient.safelySendRequest(SYSTEM_WITH_V1_MODEL + DIRECTLY_FAILED_REQUEST);

        final RetryStatistics statisticForRequest = getStatisticForRequest(SYSTEM_WITH_V1_MODEL + DIRECTLY_FAILED_REQUEST);
        MatcherAssert.assertThat(statisticForRequest, abortedAfterSeveralAttempts());
    }

    @Test
    void populateTransitiveMessageCodeWithParamsFromAlwaysFailedTransitiveRestRequest() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_ALWAYS_V2_1))
                .andExpect(status().isInternalServerError())
                .andExpect(body().isSimpleMessageError(allOf(
                        hasMessageCode(TRANSITIVE_LEVEL_METHOD_MESSAGE_CODE),
                        hasMessageParameter(TRANSITIVE_LEVEL_METHOD_MESSAGE_PARAM_NAME, TRANSITIVE_LEVEL_METHOD_MESSAGE_PARAM_VALUE, STRING))));

        final RetryStatistics statisticForFirstLevelService = getStatisticForRequest(FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD_V2_1);
        MatcherAssert.assertThat(statisticForFirstLevelService, abortedAfterFirstError());

        final RetryStatistics statisticForSecondLevelService = getStatisticForRequest(SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS_V2_1);
        MatcherAssert.assertThat(statisticForSecondLevelService, abortedAfterSeveralAttempts());
    }

    @Test
    void thereIsAbilityToCreateCustomExceptionHandlerAndSendCustomDataInResponse() throws Exception {
        mockMvc.perform(get(THROW_CUSTOM_EXEPTION_WITH_CUSTOM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString(CUSTOM_DATA_FROM_CUSTOM_EXCEPTION)));
    }

    private BodyMatcherBuilder body() {
        return new BodyMatcherBuilder(simpleMessageJsonTester, objectValidationJsonTester);
    }

    static class BodyMatcherBuilder {
        private final JacksonTester<SimpleErrorMessageResponse> simpleMessageJsonTester;
        private final JacksonTester<ObjectValidationErrorResponse> objectValidationJsonTester;

        BodyMatcherBuilder(final JacksonTester<SimpleErrorMessageResponse> simpleMessageJsonTester,
                           final JacksonTester<ObjectValidationErrorResponse> objectValidationJsonTester) {
            this.simpleMessageJsonTester = simpleMessageJsonTester;
            this.objectValidationJsonTester = objectValidationJsonTester;
        }

        ResultMatcher isSimpleMessageError(final Matcher<SimpleErrorMessageResponse> matcher) {
            return result -> {
                String contentAsString = result.getResponse().getContentAsString();
                SimpleErrorMessageResponse actual = simpleMessageJsonTester.parseObject(contentAsString);

                MatcherAssert.assertThat(actual, matcher);
            };
        }

        ResultMatcher isObjectValidationError(final Matcher<? super ObjectValidationErrorResponse> matcher) {
            return result -> {
                String contentAsString = result.getResponse().getContentAsString();
                ObjectValidationErrorResponse actual = objectValidationJsonTester.parseObject(contentAsString);

                MatcherAssert.assertThat(actual, matcher);
            };
        }

    }
}
