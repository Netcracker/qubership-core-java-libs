package com.netcracker.cloud.dbaas.client.opensearch.metrics;

import io.micrometer.common.KeyValue;
import io.micrometer.common.util.StringUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.httpcomponents.hc5.ApacheHttpClientContext;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@AllArgsConstructor
public class OpensearchClientRequestsSecondsObservationHandler implements ObservationHandler<ApacheHttpClientContext> {

    public static final String START_TIME_IN_MILLIS = "startTimeInMillis";
    public static final String METHOD_TAG_NAME = "method";
    public static final String OUTCOME_TAG_NAME = "outcome";
    public static final String STATUS_TAG_NAME = "status";
    public static final String OPERATION_TAG_NAME = "operation";
    public static final String RESOURCE_PREFIX_TAG_NAME = "resource-prefix";
    public static final String DEFAULT_OPERATION_TAG_VALUE = "index";
    public static final String UNKNOWN_TAG_VALUE = "UNKNOWN";

    private final Map<String, String> baseMetricTags;
    private final MeterRegistry meterRegistry;
    private final Timer.Builder requestsSecondsBaseMetricBuilder;

    @Override
    public boolean supportsContext(Observation.Context context) {
        return context instanceof ApacheHttpClientContext;
    }

    @Override
    public void onStart(ApacheHttpClientContext context) {
        context.put(START_TIME_IN_MILLIS, System.currentTimeMillis());
    }

    @Override
    public void onStop(ApacheHttpClientContext context) {
        var endTimeInMillis = System.currentTimeMillis();
        var startTimeInMillis = (Long) context.get(START_TIME_IN_MILLIS);

        if (startTimeInMillis != null) {
            var tags = collectMetricTags(context);
            var requestDurationInMillis = endTimeInMillis - startTimeInMillis;

            requestsSecondsBaseMetricBuilder.tags(tags)
                .register(meterRegistry)
                .record(Duration.ofMillis(requestDurationInMillis));

            if (log.isDebugEnabled()) {
                log.debug("Recorded {} millis to metric {} with tags {}",
                    requestDurationInMillis, OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME, tags
                );
            }
        } else {
            if (log.isDebugEnabled()) {
                var tags = collectMetricTags(context);

                log.debug("Not recorded millis to metric {} with tags {} because request execution start time is not saved",
                    OpensearchMetricsProvider.REQUESTS_SECONDS_METRIC_NAME, tags
                );
            }
        }
    }

    protected List<Tag> collectMetricTags(ApacheHttpClientContext context) {
        var method = context.getLowCardinalityKeyValue(METHOD_TAG_NAME);
        var outcome = context.getLowCardinalityKeyValue(OUTCOME_TAG_NAME);
        var status = context.getLowCardinalityKeyValue(STATUS_TAG_NAME);

        var requestSpecificMetricTags = Stream.of(method, outcome, status)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(KeyValue::getKey, KeyValue::getValue));

        var operationTagValue = createOperationTagValue(context);

        requestSpecificMetricTags.put(OPERATION_TAG_NAME, operationTagValue);

        return Stream.of(baseMetricTags, requestSpecificMetricTags)
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .map(this::handleTagValue)
            .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
            .toList();
    }

    protected String createOperationTagValue(ApacheHttpClientContext context) {
        String operationTagValue = null;

        var pathWithQueryParams = Optional.ofNullable(context.getCarrier())
            .map(HttpRequest::getPath)
            .orElse(null);

        if (StringUtils.isNotBlank(pathWithQueryParams)) {
            try {
                var pathWithoutQueryParams = new URI(pathWithQueryParams).getPath();

                operationTagValue = convertOpensearchRequestPathToOperationTagValue(pathWithoutQueryParams);
            } catch (URISyntaxException e) {
                log.error("Error happened during parsing URI for string value '{}'", pathWithQueryParams, e);
            }
        }

        return operationTagValue;
    }

    protected String convertOpensearchRequestPathToOperationTagValue(String opensearchRequestPath) {
        String operationTagValue = null;

        if (StringUtils.isNotBlank(opensearchRequestPath)) {

            operationTagValue = Arrays.stream(opensearchRequestPath.split("/"))
                .filter(StringUtils::isNotBlank)
                .filter(str -> str.startsWith("_"))
                .map(str -> str.substring(1))
                .collect(Collectors.joining("-"));

            if (StringUtils.isBlank(operationTagValue)) {
                operationTagValue = DEFAULT_OPERATION_TAG_VALUE;
            }
        }

        return operationTagValue;
    }

    protected Map.Entry<String, String> handleTagValue(Map.Entry<String, String> entry) {
        if (StringUtils.isBlank(entry.getValue())) {
            entry.setValue(UNKNOWN_TAG_VALUE);
        }

        return entry;
    }
}
