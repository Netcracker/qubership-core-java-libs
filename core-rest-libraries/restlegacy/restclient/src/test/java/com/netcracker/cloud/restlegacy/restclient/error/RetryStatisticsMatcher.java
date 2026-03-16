package com.netcracker.cloud.restlegacy.restclient.error;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.springframework.retry.RetryStatistics;

import static org.hamcrest.Matchers.equalTo;

public class RetryStatisticsMatcher extends TypeSafeDiagnosingMatcher<RetryStatistics> {

    private final Matcher<Integer> errorMatcher;
    private final Matcher<Integer> completedMatcher;
    private final Matcher<Integer> startedMatcher;
    private final Matcher<Integer> abortedMatcher;

    private RetryStatisticsMatcher(Matcher<Integer> errorMatcher, Matcher<Integer> completedMatcher, Matcher<Integer> startedMatcher, Matcher<Integer> abortedMatcher) {
        this.errorMatcher = errorMatcher;
        this.completedMatcher = completedMatcher;
        this.startedMatcher = startedMatcher;
        this.abortedMatcher = abortedMatcher;
    }

    @Override
    protected boolean matchesSafely(RetryStatistics item, Description mismatchDescription) {
        boolean result = true;
        if (errorMatcher != null && !errorMatcher.matches(item.getErrorCount())) {
            result = false;
            mismatchDescription.appendText("has different error count: ").appendValue(item.getErrorCount());
        }
        if (completedMatcher != null && !completedMatcher.matches(item.getCompleteCount())) {
            result = false;
            mismatchDescription.appendText("has different complete count: ").appendValue(item.getCompleteCount());
        }
        if (startedMatcher != null && !startedMatcher.matches(item.getStartedCount())) {
            result = false;
            mismatchDescription.appendText("has different started count: ").appendValue(item.getStartedCount());
        }
        if (abortedMatcher != null && !abortedMatcher.matches(item.getAbortCount())) {
            result = false;
            mismatchDescription.appendText("has different aborted count: ").appendValue(item.getAbortCount());
        }
        return result;
    }

    @Override
    public void describeTo(Description description) {
        if (errorMatcher != null) {
            description.appendText("has error count: ").appendDescriptionOf(errorMatcher);
        }
        if (completedMatcher != null) {
            description.appendText("has completed count: ").appendDescriptionOf(completedMatcher);
        }
        if (startedMatcher != null) {
            description.appendText("has started count: ").appendDescriptionOf(startedMatcher);
        }
        if (abortedMatcher != null) {
            description.appendText("has aborted count: ").appendDescriptionOf(abortedMatcher);
        }
    }

    public static Matcher<RetryStatistics> hasErrors(Matcher<Integer> errorCount) {
        return new RetryStatisticsMatcher(errorCount, null, null, null);
    }

    public static Matcher<RetryStatistics> hasErrors(int errorCount) {
        return hasErrors(equalTo(errorCount));
    }

    public static Matcher<RetryStatistics> hasAborted(int abortedCount) {
        return new RetryStatisticsMatcher(null, null, null, equalTo(abortedCount));
    }

    public static Matcher<RetryStatistics> hasCompleted(int completeCount) {
        return new RetryStatisticsMatcher(null, equalTo(completeCount), null, null);
    }

    public static Matcher<RetryStatistics> hasStarted(Matcher<Integer> startedCount) {
        return new RetryStatisticsMatcher(null, null, startedCount, null);
    }

    public static Matcher<RetryStatistics> hasStarted(int startedCount) {
        return hasStarted(equalTo(startedCount));
    }
}
