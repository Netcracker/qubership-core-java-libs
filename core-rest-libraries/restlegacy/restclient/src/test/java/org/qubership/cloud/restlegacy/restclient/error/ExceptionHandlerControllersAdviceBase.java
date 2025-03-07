package org.qubership.cloud.restlegacy.restclient.error;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.qubership.cloud.restlegacy.restclient.service.MessageService;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.retry.RetryStatistics;
import org.springframework.retry.stats.StatisticsRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Iterator;

import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;
import static org.qubership.cloud.restlegacy.restclient.error.RetryStatisticsMatcher.*;
import static org.qubership.cloud.restlegacy.restclient.error.TestExceptionHandlingRestController.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// @Ignore is added to prevent test failing if you will execute tests directly on this class from IDE.
// It is norm behaviour, because IDE test runner starts all tests annotated by @Test.
// But maven surefire plugin (is used to execute test during maven build) will not start test for this class never, because class name does not contain 'Test'.
// See details here http://maven.apache.org/surefire/maven-surefire-plugin/examples/inclusion-exclusion.html
@Ignore
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

    @Before
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
    }

    @Test
    public void keepStatusCodeInResponseIfThrowLegacyErrorException() throws Exception {
        mockMvc.perform(get(THROW_LEGACY_ERROR_EXCEPTION_WITH_404)).andExpect(status().isNotFound());
    }

    @Test
    public void badRequestStatusCodeInResponseIfThrowIllegalArgumentException() throws Exception {
        mockMvc.perform(get(THROW_ILLEGAL_ARGUMENT_EXCEPTION_METHOD)).andExpect(status().isBadRequest());
    }

    @Test
    public void notFoundStatusCodeInResponseIfThrowEntityNotFoundException() throws Exception {
        mockMvc.perform(get(THROW_ENTITY_NOT_FOUND_METHOD)).andExpect(status().isNotFound());
    }

    @Test
    public void propogateSpecialUserMessageWithEntityNameAndIdToResponseIfObjectNotFoundWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ENTITY_NOT_FOUND_METHOD))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(NOT_FOUNDED_ENTITY_ID)))
                .andExpect(content().string(containsString(ENTITY_TYPE)));
    }

    @Test
    public void internalErrorCodeInResponseIfStandardJavaExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_ILLEGAL_STATE_EXCEPTION_METHOD))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void conflictErrorMessageToResponseIfOptimisticLockingFailureExceptionWasThrown() throws Exception {
        mockMvc.perform(get(THROW_OPTIMISTIC_LOCKING_EXCEPTION_METHOD))
                .andExpect(status().isConflict());
    }

    @Test
    public void unprocessableEntityCodeInResponseIfValidationFailed() throws Exception {

        mockMvc.perform(get(THROW_OBJECT_VALIDATION_EXCEPTION).content("{}").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void badRequestCodeInResponseIfRequestContainsNotReadableMessage() throws Exception {

        mockMvc.perform(get(THROW_OBJECT_VALIDATION_EXCEPTION).content("{").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void badRequestCodeInResponseIfRequestDoesNotContainsRequiredParameter() throws Exception {

        mockMvc.perform(get(THROW_METHOD_ARGUMENT_TYPE_MISMATCH).param(INTEGER_REQUEST_PARAMETER_NAME, "notInteger").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void dontRetryAlwaysFailedTransitiveRestRequest() throws Exception {

        mockMvc.perform(get(REDIRECT_TO_TRANSITIVE_SERVICE_THAT_FAILS_ALWAYS))
                .andExpect(status().isInternalServerError());

        final RetryStatistics statisticForFirstLevelService = getStatisticForRequest(FIRST_TRANSITIVE_LEVEL_METHOD_THAT_REDIRECTS_TO_ALWAYS_FAILED_METHOD);
        assertThat(statisticForFirstLevelService, abortedAfterFirstError());

        final RetryStatistics statisticForSecondLevelService = getStatisticForRequest(SECOND_TRANSITIVE_LEVEL_METHOD_THAT_FAILS_ALWAYS);
        assertThat(statisticForSecondLevelService, abortedAfterSeveralAttempts());
    }

    @Test
    public void dontRetryDirectFailedRestRequestIfBadRequest() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_400_METHOD)).andExpect(status().isBadRequest());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_400_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    public void dontRetryDirectFailedRestRequestIfUnprocessableEntityError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_422_METHOD)).andExpect(status().isUnprocessableEntity());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_422_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    public void dontRetryDirectFailedRestRequestIfNotFoundError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_404_METHOD)).andExpect(status().isNotFound());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_404_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    public void dontRetryDirectFailedRestRequestIfConflictError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_409_METHOD)).andExpect(status().isConflict());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_409_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    public void dontRetryDirectFailedRestRequestIfUnauthorizedError() throws Exception {
        mockMvc.perform(get(REDIRECT_TO_FAILED_WITH_401_METHOD)).andExpect(status().isUnauthorized());

        final RetryStatistics statisticForRequest = getStatisticForRequest(FAILED_WITH_401_METHOD);
        assertThat(statisticForRequest, abortedAfterFirstError());
    }

    @Test
    public void dontRetryDirectFailedRestRequestIfForbiddenError() throws Exception {
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