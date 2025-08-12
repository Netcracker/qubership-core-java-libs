package com.netcracker.cloud.restlegacy.restclient.error.v2;

import com.netcracker.cloud.restlegacy.restclient.error.ErrorsDescription;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Date;

import static com.netcracker.cloud.restlegacy.restclient.error.v2.ControllerWithV1ExceptionModel.SYSTEM_WITH_V1_MODEL;

@RequestMapping(SYSTEM_WITH_V1_MODEL)
@RestController
public class ControllerWithV1ExceptionModel {

    public static final String SYSTEM_WITH_V1_MODEL = "/systemWithV1Model";
    public static final String TRANSITIVE_FAILED_REQUEST = "/transitiveFailedRequest";
    public static final String DIRECTLY_FAILED_REQUEST = "/directlyFailedRequest";

    @GetMapping(value = TRANSITIVE_FAILED_REQUEST)
    public ResponseEntity transitiveFailedRequest() {
        return new ResponseEntity<>(emulateErrorToResponseConversion(true), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping(value = DIRECTLY_FAILED_REQUEST)
    public ResponseEntity directlyFailedRequest() {
        return new ResponseEntity<>(emulateErrorToResponseConversion(false), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ErrorsDescription emulateErrorToResponseConversion(boolean isTransitiveFailedError) {
        final ErrorsDescription errorsDescription = new ErrorsDescription(new Date(),
                "serviceUrl",
                HttpStatus.INTERNAL_SERVER_ERROR,
                Collections.emptyList(),
                "exception", "originalException", "stackTrace");
        errorsDescription.setProxy(isTransitiveFailedError);
        return errorsDescription;
    }
}
