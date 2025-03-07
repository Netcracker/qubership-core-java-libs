package org.qubership.cloud.restlegacy.restclient.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Date;

import static org.qubership.cloud.restlegacy.restclient.error.v2.ResponseAdviceExceptionHelper.isTransitiveError;


/**
 * Don't catch this exception and never throw it directly.
 * Catch {@link org.springframework.web.client.RestClientException}.
 * This exception will be removed in next major release
 */
/**
 * @deprecated
 */
@Deprecated
@EqualsAndHashCode(callSuper = true)
@ToString
public class ProxyErrorException extends RestClientException {
    @Getter
    private final ResponseEntity<ErrorsDescription> responseEntity;
    @Getter
    private HttpStatus httpStatus;

    public ProxyErrorException(ProxyErrorException cause) {
        super("Exception while communicating with microservices", cause);
        this.responseEntity = cause.responseEntity;
        this.httpStatus = cause.httpStatus;
    }

    public ProxyErrorException(Exception cause, String url) {
        super("Exception while communicating with " + url, cause);
        ErrorsDescription errorsDescription;
        this.httpStatus = HttpStatus.EXPECTATION_FAILED;
        final String stackTrace = ExceptionUtils.getStackTrace(cause);
        if (cause instanceof HttpStatusCodeException) {
            HttpStatusCodeException e = (HttpStatusCodeException) cause;
            this.httpStatus = (HttpStatus) e.getStatusCode();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                errorsDescription = objectMapper.readValue(e.getResponseBodyAsString(), ErrorsDescription.class);
            } catch (Exception ex) {
                errorsDescription = new ErrorsDescription(
                        new Date(),
                        url,
                        (HttpStatus) e.getStatusCode(),
                        Collections.singletonList(
                                new ErrorsDescription.ErrorDescription(
                                        e.getStatusText(),
                                        e.getMessage()
                                )
                        ),
                        e.toString(),
                        e.getResponseBodyAsString(),
                        stackTrace
                );
            }

        } else {
            if (cause instanceof ResourceAccessException
                    && ((ResourceAccessException) cause).getRootCause() instanceof SocketTimeoutException) {
                this.httpStatus = HttpStatus.GATEWAY_TIMEOUT;
            }
            errorsDescription = new ErrorsDescription(
                    new Date(),
                    url,
                    this.httpStatus,
                    Collections.singletonList(
                            new ErrorsDescription.ErrorDescription(
                                    "", cause.getMessage()
                            )
                    ),
                    cause.toString(),
                    cause.getMessage(),
                    stackTrace
            );
        }
        if (isTransitiveError(cause)) {
            errorsDescription.setProxy(true);
        }
        responseEntity = new ResponseEntity<>(errorsDescription, this.httpStatus);
    }
}
