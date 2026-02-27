package com.netcracker.cloud.restlegacy.restclient.error;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import com.netcracker.cloud.restlegacy.restclient.service.MessageService;
import org.hamcrest.Matcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.MediaType;
import org.springframework.retry.RetryStatistics;
import org.springframework.retry.stats.StatisticsRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;
import static com.netcracker.cloud.restlegacy.restclient.error.RetryStatisticsMatcher.*;
import static com.netcracker.cloud.restlegacy.restclient.error.TestExceptionHandlingRestController.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureTestRestTemplate
public class ExceptionHandlerControllersAdviceBase {

    @Autowired
    protected WebApplicationContext context;

    @Autowired
    protected MessageService messageService;

    @Autowired
    protected TestExceptionHandlingConfiguration.TestRestClient restClient;

    protected MockMvc mockMvc;

    @Autowired
    private StatisticsRepository statisticsRepository;

    protected static Matcher<RetryStatistics> abortedAfterSeveralAttempts() {
        return allOf(hasStarted(greaterThanOrEqualTo(1)), hasCompleted(0), hasErrors(greaterThanOrEqualTo(1)), hasAborted(1));
    }

    protected static Matcher<RetryStatistics> abortedAfterFirstError() {
        return allOf(hasStarted(1), hasCompleted(0), hasErrors(1), hasAborted(1));
    }

    protected static Matcher<RetryStatistics> completedAfterFirstAttempt() {
        return allOf(hasStarted(1), hasCompleted(1), hasErrors(0));
    }

    protected static Matcher<RetryStatistics> completedAfterOneError() {
        return allOf(hasStarted(2), hasCompleted(1), hasErrors(1));
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
    }

    @Test
    void keepStatusCodeInResponseIfThrowLegacyErrorException() throws Exception {
        mockMvc.perform(get(THROW_LEGACY_ERROR_EXCEPTION_WITH_404)).andExpect(status().isNotFound());
    }

    @Test
    void badRequestStatusCodeInResponseIfThrowIllegalArgumentException() throws Exception {
        mockMvc.perform(get(THROW_ILLEGAL_ARGUMENT_EXCEPTION_METHOD)).andExpect(status().isBadRequest());
    }

    @Test
    void notFoundStatusCodeInResponseIfThrowEntityNotFoundException() throws Exception {
        mockMvc.perform(get(THROW_ENTITY_NOT_FOUND_METHOD)).andExpect(status().isNotFound());
    }

    @Test
    void propogateSpecialUserMessageWithEntityNameAndIdToResponseIfObjectNotFoundWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ENTITY_NOT_FOUND_METHOD))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(NOT_FOUNDED_ENTITY_ID)))
                .andExpect(content().string(containsString(ENTITY_TYPE)));
    }

    @Test
    void internalErrorCodeInResponseIfStandardJavaExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ILLEGAL_STATE_EXCEPTION_METHOD))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void conflictErrorMessageToResponseIfOptimisticLockingFailureExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_OPTIMISTIC_LOCKING_EXCEPTION_METHOD))
                .andExpect(status().isConflict());
    }

    @Test
    void unprocessableEntityCodeInResponseIfValidationFailed() throws Exception {

        mockMvc.perform(get(THROW_OBJECT_VALIDATION_EXCEPTION).content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void badRequestCodeInResponseIfRequestContainsNotReadableMessage() throws Exception {

        mockMvc.perform(get(THROW_OBJECT_VALIDATION_EXCEPTION).content("{").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void badRequestCodeInResponseIfRequestDoesNotContainsRequiredParameter() throws Exception {

        mockMvc.perform(get(THROW_METHOD_ARGUMENT_TYPE_MISMATCH).param(INTEGER_REQUEST_PARAMETER_NAME, "notInteger").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dontRetryAlwaysFailedTransitiveRestRequest() throws Exception {

        mockMvc.perform(get(REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_ALWAYS))
                .andExpect(status().isInternalServerError());

        final RetryStatistics statisticForFirstLevelService = getStatisticForRequest(FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD);
        assertThat(statisticForFirstLevelService, abortedAfterFirstError());

        final RetryStatistics statisticForSecondLevelService = getStatisticForRequest(SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS);
        assertThat(statisticForSecondLevelService, abortedAfterSeveralAttempts());
    }

    @Test
    void dontRetryDirectFailedRestRequestIfBadRequest() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_400_METHOD)).andExpect(status().isBadRequest());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_400_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    void dontRetryDirectFailedRestRequestIfUnprocessableEntityError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_422_METHOD)).andExpect(status().isUnprocessableEntity());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_422_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    void dontRetryDirectFailedRestRequestIfNotFoundError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_404_METHOD)).andExpect(status().isNotFound());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_404_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    void dontRetryDirectFailedRestRequestIfConflictError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_409_METHOD)).andExpect(status().isConflict());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_409_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    void dontRetryDirectFailedRestRequestIfUnauthorizedError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_401_METHOD)).andExpect(status().isUnauthorized());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_401_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    void dontRetryDirectFailedRestRequestIfForbiddenError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_403_METHOD)).andExpect(status().isForbidden());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_403_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    protected String getInternalErrorMessage() {
        return messageService.getMessage(ErrorMessageCodes.INTERNAL_SERVER_ERROR_CODE);
    }

    protected String getEntityNotFoundMessage(String entityName, String entityId) {
        return messageService.getMessage(ErrorMessageCodes.ENTITY_NOT_FOUND_ERROR_CODE, entityName, entityId);
    }

    @SuppressWarnings("StaticPseudoFunctionalStyleMethod")
    protected RetryStatistics getStatisticForRequest(final String request) {
        RetryStatistics expectedRetryStatistic = findRetryStatistics(request);
        assertThat(expectedRetryStatistic, notNullValue(RetryStatistics.class));
        return expectedRetryStatistic;
    }

    private RetryStatistics findRetryStatistics(String request) {
        Iterator<RetryStatistics> iterator = statisticsRepository.findAll().iterator();
        RetryStatistics expectedRetryStatistic = null;
        while (iterator.hasNext()) {
            RetryStatistics retryStatistic = iterator.next();
            if (retryStatistic.getName() != null && retryStatistic.getName().endsWith(request)) {
                expectedRetryStatistic = retryStatistic;
                break;
            }
        }
        return expectedRetryStatistic;
    }
}
