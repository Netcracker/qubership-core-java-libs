package com.netcracker.cloud.disableapi.spring;

import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponse;
import com.netcracker.cloud.disableapi.DeprecatedApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

@Slf4j
public class ErrorHandler {
    public ResponseEntity<TmfErrorResponse> buildErrorResponse(HttpServletRequest req, Set<String> httpMethods, String pathPattern) {
        String requestURI = req.getRequestURI();
        DeprecatedApiException e = new DeprecatedApiException(req.getMethod(), requestURI, httpMethods, pathPattern);
        log.warn(e.getDetail());
        HttpStatus httpStatus = HttpStatus.NOT_FOUND;
        return new ResponseEntity<>(TmfErrorResponse.builder(e).status(String.valueOf(httpStatus.value())).build(), httpStatus);
    }
}
