package org.qubership.cloud.restlegacy.restclient.error.v2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ControllerWithCustomException {

    public static final String THROW_CUSTOM_EXEPTION_WITH_CUSTOM_DATA = "/throwCustomExceptionWithCustomData";
    public static final String CUSTOM_DATA_FROM_CUSTOM_EXCEPTION = "customDataValue";

    @GetMapping(value = THROW_CUSTOM_EXEPTION_WITH_CUSTOM_DATA)
    public ResponseEntity throwCustomExceptionWithCustomData() {
        throw new CustomException(CUSTOM_DATA_FROM_CUSTOM_EXCEPTION);
    }
}
