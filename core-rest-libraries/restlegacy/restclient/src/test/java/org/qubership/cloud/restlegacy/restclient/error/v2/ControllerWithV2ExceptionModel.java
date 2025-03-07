package org.qubership.cloud.restlegacy.restclient.error.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.qubership.cloud.restlegacy.restclient.error.v2.ControllerWithV2ExceptionModel.SYSTEM_WITH_V2_MODEL;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RequestMapping(SYSTEM_WITH_V2_MODEL)
@RestController
public class ControllerWithV2ExceptionModel {

    static final String SYSTEM_WITH_V2_MODEL = "/systemWithV2Model";
    static final String TRANSITIVE_FAILED_REQUEST = "/transitiveFailedRequest";
    static final String DIRECTLY_FAILED_REQUEST = "/directlyFailedRequest";
    private static final String RESPONSE_BODY = "{errorKey: \"errorValue\"}";

    @GetMapping(value = TRANSITIVE_FAILED_REQUEST)
    public ResponseEntity transitiveFailedRequest() {
        return emulateErrorToResponseConversion(true);
    }

    @GetMapping(value = DIRECTLY_FAILED_REQUEST)
    public ResponseEntity directlyFailedRequest() {
        return emulateErrorToResponseConversion(false);
    }

    private ResponseEntity emulateErrorToResponseConversion(boolean isTransitiveFailedError) {
        if (isTransitiveFailedError) {
            return ResponseAdviceExceptionHelper.createTransitiveErrorResponse(RESPONSE_BODY, INTERNAL_SERVER_ERROR);
        }
        return ResponseAdviceExceptionHelper.createResponse(RESPONSE_BODY, INTERNAL_SERVER_ERROR);
    }
}
