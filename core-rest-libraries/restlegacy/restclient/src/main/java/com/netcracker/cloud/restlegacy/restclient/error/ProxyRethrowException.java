package com.netcracker.cloud.restlegacy.restclient.error;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Don't catch this exception and never throw it directly. It will be removed in next major release
 */

/**
 * @deprecated
 */
@Deprecated
@EqualsAndHashCode(callSuper = true)
@ToString
public class ProxyRethrowException extends ProxyErrorException {

    public ProxyRethrowException(ProxyErrorException cause) {
        super(cause);
    }

    /**
     * @deprecated Change to throw RestClientException when {@link ProxyErrorException} will be removed
     */
    @Deprecated
    public static ProxyErrorException buildProxyException(Exception cause, String url) {
        ProxyErrorException e = new ProxyErrorException(cause, url);
        final ResponseEntity<ErrorsDescription> responseEntity = e.getResponseEntity();
        if (responseEntity == null || !responseEntity.hasBody()) {
            return e;
        }
        if (responseEntity.getBody() == null) {
            return e;
        }
        return responseEntity.getBody().isProxy()

                // These statuses don't do retrying.
                || HttpStatus.BAD_REQUEST.equals(responseEntity.getBody().getStatus())
                || HttpStatus.UNPROCESSABLE_ENTITY.equals(responseEntity.getBody().getStatus())
                || HttpStatus.CONFLICT.equals(responseEntity.getBody().getStatus())
                || HttpStatus.NOT_FOUND.equals(responseEntity.getBody().getStatus())
                || HttpStatus.UNAUTHORIZED.equals(responseEntity.getBody().getStatus())
                || HttpStatus.FORBIDDEN.equals(responseEntity.getBody().getStatus())

                ? new ProxyRethrowException(e) : e;
    }

}
