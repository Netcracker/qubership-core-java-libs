package org.qubership.cloud.restlegacy.restclient.error.v2;

import org.hamcrest.*;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.restlegacy.restclient.app.TestConfig;
import org.qubership.cloud.restlegacy.restclient.error.ExceptionHandlerControllersAdviceBase;
import org.qubership.cloud.restlegacy.restclient.error.TestExceptionHandlingConfiguration;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.retry.RetryStatistics;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.qubership.cloud.restlegacy.restclient.error.ErrorMessageCodes.DB_IN_CONFLICT_ERROR_CODE;
import static org.qubership.cloud.restlegacy.restclient.error.TestExceptionHandlingRestController.*;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.ERROR_HANDLER_VERSION_CONDITION_PROPERTY;
import static org.qubership.cloud.restlegacy.restclient.error.v2.Constants.VERSION_2;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithCustomException.CUSTOM_DATA_FROM_CUSTOM_EXCEPTION;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithCustomException.THROW_CUSTOM_EXEPTION_WITH_CUSTOM_DATA;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithV1ExceptionModel.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {TestConfig.class, TestExceptionHandlingConfiguration.class, ControllerWithV1ExceptionModel.class, CustomExceptionHandler.class, ControllerWithCustomException.class},
        properties = {ERROR_HANDLER_VERSION_CONDITION_PROPERTY + "=" + VERSION_2})
@AutoConfigureJsonTesters
class ExceptionHandlerControllersAdviceTest extends ExceptionHandlerControllersAdviceBase {

    @Autowired
    private JacksonTester<PlainTextErrorDescription> plainTextJsonTester;

    @Autowired
    private JacksonTester<ObjectValidationErrorDescription> objectErrorJsonTester;

    private static Matcher<PlainTextErrorDescription> hasErrorMessage(String message) {
        return hasErrorMessage(Matchers.equalTo(message));
    }

    private static Matcher<? super ObjectValidationErrorDescription> hasValidationErrors(Matcher<Iterable<? extends ObjectValidationErrorDescription.FieldValidationErrorDescription>> matcher) {
        return new FeatureMatcher<ObjectValidationErrorDescription, List<ObjectValidationErrorDescription.FieldValidationErrorDescription>>
                (matcher, "has validationErrors", "validationErrors") {
            @Override
            protected List<ObjectValidationErrorDescription.FieldValidationErrorDescription> featureValueOf(ObjectValidationErrorDescription actual) {
                return actual.getValidationErrors();
            }
        };
    }

    private static Matcher<? super ObjectValidationErrorDescription> hasObjectValidationMessage(String expectedMessage) {
        return new FeatureMatcher<>
                (Matchers.equalTo(expectedMessage), "has objectValidationMessage", "objectValidationMessage") {
            @Override
            protected String featureValueOf(ObjectValidationErrorDescription actual) {
                return actual.getObjectValidationMessage();
            }
        };
    }

    private static Matcher<ObjectValidationErrorDescription.FieldValidationErrorDescription> equalTo(String fieldName, String message) {
        return new TypeSafeDiagnosingMatcher<>() {
            @Override
            protected boolean matchesSafely(ObjectValidationErrorDescription.FieldValidationErrorDescription item, Description mismatchDescription) {
                boolean result = true;
                if (!message.equals(item.getErrorMessage())) {
                    result = false;
                    mismatchDescription.appendText(" has different errorMessage ").appendValue(item.getErrorMessage());
                }
                if (!fieldName.equals(item.getFieldName())) {
                    result = false;
                    mismatchDescription.appendText(" has different fieldName ").appendValue(item.getFieldName());
                }
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("has fieldName ").appendText(fieldName).appendText(", has errorMessage ").appendText(message);
            }
        };
    }

    private static Matcher<PlainTextErrorDescription> hasErrorMessage(Matcher<String> messageMatcher) {
        return new FeatureMatcher<>(messageMatcher, "has errorMessage", "errorMessage") {

            @Override
            protected String featureValueOf(PlainTextErrorDescription actual) {
                return actual.getErrorMessage();
            }
        };
    }

    @Test
    void dontUseV1VersionOfErrorHandlingIfItIsV2SpecifiedDirectly() {
        assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(ControllersAdvice.class));
    }

    @Test
    void useV2VersionOfErrorHandlingIfItIsSpecifiedDirectly() {
        assertNotNull(context.getBean(ExceptionHandlingV2MainConfiguration.class));
        assertNotNull(context.getBean(ExceptionHandlingV2_0Configuration.class));
    }

    @Test
    void propagateUserMessageFromErrorTypeToResponseIfThrowLegacyErrorException() throws Exception {
        mockMvc.perform(get(THROW_LEGACY_ERROR_EXCEPTION_WITH_404))
                .andExpect(body().isPlainTextError(hasErrorMessage(getEntityNotFoundMessage(ENTITY_TYPE, NOT_FOUNDED_ENTITY_ID))));
    }

    @Test
    void propagateUserMessageToResponseIfErrorClassHasSpecialAnnotation() throws Exception {
        mockMvc.perform(get(THROW_DISPLAYED_MESSAGE_EXCEPTION_METHOD))
                .andExpect(status().isInternalServerError())
                .andExpect(body().isPlainTextError(hasErrorMessage(CUSTOM_USER_MESSAGE)));
    }

    @Test
    void propagateUserMessageToResponseIfGenericDisplayedExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_GENERIC_DISPLAYED_EXCEPTION_METHOD))
                .andExpect(status().isInternalServerError())
                .andExpect(body().isPlainTextError(hasErrorMessage(CUSTOM_USER_MESSAGE)));
    }

    @Test
    void propagateSpecialUserMessageWithEntityNameAndIdToResponseIfObjectNotFoundWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ENTITY_NOT_FOUND_METHOD))
                .andExpect(content().string(containsString(NOT_FOUNDED_ENTITY_ID)))
                .andExpect(content().string(containsString(ENTITY_TYPE)));
    }

    @Test
    void propagateGeneralInternalErrorMessageToResponseIfErrorClassDoesNotHaveSpecialAnnotation() throws Exception {
        mockMvc.perform(get(THROW_NOT_DISPLAYED_MESSAGE_EXCEPTION_METHOD))
                .andExpect(status().isInternalServerError())
                .andExpect(body().isPlainTextError(hasErrorMessage(getInternalErrorMessage())));
    }

    @Test
    void propagateGeneralInternalErrorMessageToResponseIfStandardJavaExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ILLEGAL_STATE_EXCEPTION_METHOD))
                .andExpect(body().isPlainTextError(hasErrorMessage(getInternalErrorMessage())));
    }

    @Test
    void propagateConflictErrorMessageToResponseIfOptimisticLockingFailureExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_OPTIMISTIC_LOCKING_EXCEPTION_METHOD))
                .andExpect(body().isPlainTextError(hasErrorMessage(messageService.getMessage(DB_IN_CONFLICT_ERROR_CODE))));
    }

    @Test
    void addFieldValidationDetailsInResponseIfValidationFailed() throws Exception {
        mockMvc.perform(get(THROW_OBJECT_VALIDATION_EXCEPTION).content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(body().isObjectValidationError(hasValidationErrors(contains(equalTo(FAILED_FIELD, FIELD_VALIDATION_MESSAGE)))));
    }

    @Test
    void addObjectValidationMessageInResponseIfEntireObjectWasRejected() throws Exception {
        mockMvc.perform(get(THROW_ENTIRE_OBJECT_VALIDATION_EXCEPTION).content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(body().isObjectValidationError(hasObjectValidationMessage(OBJECT_VALIDATION_MESSAGE)));
    }

    @Test
    void dontRetryRequestToSystemThatUsesV1ModelIfFailWasInTransitiveService() {
        restClient.safelySendRequest(SYSTEM_WITH_V1_MODEL + TRANSITIVE_FAILED_REQUEST);

        final RetryStatistics statisticForRequest = getStatisticForRequest(SYSTEM_WITH_V1_MODEL + TRANSITIVE_FAILED_REQUEST);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    void retryRequestToSystemThatUsesV1ModelIfFailWasInDirectService() {
        restClient.safelySendRequest(SYSTEM_WITH_V1_MODEL + DIRECTLY_FAILED_REQUEST);

        final RetryStatistics statisticForRequest = getStatisticForRequest(SYSTEM_WITH_V1_MODEL + DIRECTLY_FAILED_REQUEST);
        assertThat(statisticForRequest, abortedAfterSeveralAttempts());
    }

    @Test
    void dontRetryAlwaysFailedTransitiveRestRequest() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_ALWAYS))
                .andExpect(status().isInternalServerError())
                .andExpect(body().isPlainTextError(hasErrorMessage(TRANSITIVE_LEVEL_METHOD_ERROR_MESSAGE)));

        final RetryStatistics statisticForFirstLevelService = getStatisticForRequest(FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD);
        assertThat(statisticForFirstLevelService, abortedAfterFirstError());

        final RetryStatistics statisticForSecondLevelService = getStatisticForRequest(SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS);
        assertThat(statisticForSecondLevelService, abortedAfterSeveralAttempts());
    }

    @Test
    void thereIsAbilityToCreateCustomExceptionHandlerAndSendCustomDataInResponse() throws Exception {
        mockMvc.perform(get(THROW_CUSTOM_EXEPTION_WITH_CUSTOM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString(CUSTOM_DATA_FROM_CUSTOM_EXCEPTION)));
    }

    private BodyMatcherBuilder body() {
        return new BodyMatcherBuilder(plainTextJsonTester, objectErrorJsonTester);
    }

    static class BodyMatcherBuilder {
        private final JacksonTester<PlainTextErrorDescription> plainTextJsonTester;
        private final JacksonTester<ObjectValidationErrorDescription> formErrorJsonTester;

        BodyMatcherBuilder(JacksonTester<PlainTextErrorDescription> plainTextJsonTester, JacksonTester<ObjectValidationErrorDescription> formErrorJsonTester) {
            this.plainTextJsonTester = plainTextJsonTester;
            this.formErrorJsonTester = formErrorJsonTester;
        }

        ResultMatcher isPlainTextError(Matcher<PlainTextErrorDescription> matcher) {
            return result -> {
                String contentAsString = result.getResponse().getContentAsString();
                PlainTextErrorDescription actual = plainTextJsonTester.parseObject(contentAsString);

                assertThat(actual, matcher);
            };
        }

        ResultMatcher isObjectValidationError(Matcher<? super ObjectValidationErrorDescription> matcher) {
            return result -> {
                String contentAsString = result.getResponse().getContentAsString();
                ObjectValidationErrorDescription actual = formErrorJsonTester.parseObject(contentAsString);

                assertThat(actual, matcher);
            };
        }
    }
}
