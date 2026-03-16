package com.netcracker.cloud.restlegacy.restclient.error.v2;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;


/**
 * If you want to register custom exception handler, you should:
 * <ul>
 * <li>Create class annotated by {@link org.springframework.web.bind.annotation.ControllerAdvice}</li>
 * <li>Add method annotated by {@link org.springframework.web.bind.annotation.ExceptionHandler} with required exception</li>
 * <li>Annotate this method by {@link org.springframework.web.bind.annotation.ResponseBody}</li>
 * <li>Instantiate response body - POJO. The POJO should be serializable by Jackson.
 * You can use:
 * <ul>
 * <li>      {@link PlainTextErrorDescription}</li>
 * <li>      {@link ObjectValidationErrorDescription}</li>
 * <li>      or create your custom POJO. This POJO should extend {@link HasDebugInfo} with generic T ({@link DebugInfo} or custom inheritor)
 * See unit test with example</li>
 * </ul>
 * </li>
 * <li>Write {@link DebugInfo} in POJO</li>
 * <li>Log POJO with error logging level</li>
 * <li>Create and return value: ResponseEntity with POJO. <b>Use {@link #createResponse} methods to create ResponseEntity</b>
 * If root cause of your exception is in related microservice, you can use {@link #createTransitiveErrorResponse(Object, HttpStatusCode)} to prevent retry calls in your service
 * </li>
 * </ul>
 *
 * <b>Example:</b>
 * <pre>
 * public class CustomException extends RuntimeException {
 * private final String customData;
 *
 * public CustomException(String message,  String customData) {
 *   super(message);
 *   this.customData = customData;
 * }
 *
 * public String getCustomData() {
 *    return customData;
 *  }
 * }
 *
 *
 * &#064;RequiredArgsConstructor
 * class CustomExceptionResponse implements HasDebugInfo&lt;DebugInfo&gt; {
 * &#064;Getter
 * private final String message;
 *
 * &#064;Getter
 * private final String customData;
 *
 * &#064;Setter
 * &#064;Getter
 * public DebugInfo debugInfo;
 * }
 *
 *
 *
 * &#064;Slf4j
 * &#064;ControllerAdvice
 * class CustomExceptionHandler {
 *
 * &#064;ExceptionHandler(CustomException.class)
 * &#064;ResponseBody
 * private ResponseEntity&lt;CustomExceptionResponse&gt; handleCustomException(HttpServletRequest request, CustomException e) {
 *   final CustomExceptionResponse customExceptionResponse = new CustomExceptionResponse(e.getMessage(), e.getCustomData());
 *   ResponseAdviceExceptionHelper.addDebugInfo(request, e, customExceptionResponse);
 *   log.error(customExceptionResponse.toString());
 *   return ResponseAdviceExceptionHelper.createResponse(customExceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
 *  }
 * }
 *
 * </pre>
 */
public class ResponseAdviceExceptionHelper {
    private static final String NC_ERROR_HEADER = "advice.ncError";
    private static final String ADVICE_NC_ERROR_TRANSITIVE_HEADER = "advice.ncError.transitive";

    public static void addDebugInfo(HttpServletRequest request, Throwable e, HasDebugInfo<DebugInfo> errorResponse) {
        errorResponse.setDebugInfo(new DebugInfo(new Date(), request.getRequestURI(), getStackTrace(e)));
    }

    private static String getStackTrace(final Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    public static <T> ResponseEntity<T> createResponse(T responseBody, HttpStatus httpStatus) {
        return createResponse(responseBody, null, httpStatus);
    }

    public static <T> ResponseEntity<T> createResponse(T responseBody, HttpHeaders headers, HttpStatusCode httpStatus) {
        return new ResponseEntity<>(responseBody, addCommonNcHeaders(headers), httpStatus);
    }

    public static <T> ResponseEntity<T> createTransitiveErrorResponse(T responseBody, HttpHeaders headers, HttpStatusCode httpStatus) {
        return createResponse(responseBody, addTransitiveMarkerHeader(headers), httpStatus);
    }

    public static <T> ResponseEntity<T> createTransitiveErrorResponse(T responseBody, HttpStatusCode httpStatus) {
        return createTransitiveErrorResponse(responseBody, null, httpStatus);
    }

    public static boolean isKnownResponseFormat(RestClientResponseException ex) {
        final HttpHeaders responseHeaders = ex.getResponseHeaders();
        return responseHeaders != null && responseHeaders.containsKey(NC_ERROR_HEADER);
    }

    private static HttpHeaders addTransitiveMarkerHeader(HttpHeaders headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            httpHeaders.putAll(headers);
        }
        httpHeaders.put(ADVICE_NC_ERROR_TRANSITIVE_HEADER, Collections.singletonList("true"));
        return httpHeaders;
    }

    public static boolean isTransitiveError(Throwable e) {
        if (!(e instanceof RestClientResponseException)) {
            return false;
        }
        final RestClientResponseException restClientResponseException = (RestClientResponseException) e;
        final HttpHeaders responseHeaders = restClientResponseException.getResponseHeaders();
        return responseHeaders != null && responseHeaders.containsKey(ADVICE_NC_ERROR_TRANSITIVE_HEADER);
    }

    private static HttpHeaders addCommonNcHeaders(HttpHeaders headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null) {
            httpHeaders.putAll(headers);
        }
        httpHeaders.set(NC_ERROR_HEADER, "true");
        return httpHeaders;
    }
}
