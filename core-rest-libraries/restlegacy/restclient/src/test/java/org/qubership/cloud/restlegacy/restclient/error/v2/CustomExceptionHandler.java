package org.qubership.cloud.restlegacy.restclient.error.v2;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


import static org.qubership.cloud.restlegacy.restclient.error.v2.ResponseAdviceExceptionHelper.addDebugInfo;
import static org.qubership.cloud.restlegacy.restclient.error.v2.ResponseAdviceExceptionHelper.createResponse;

@Slf4j
@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(CustomException.class)
    @ResponseBody
    private ResponseEntity<CustomExceptionResponse> handleCustomException(HttpServletRequest request, CustomException e) {
        final CustomExceptionResponse customExceptionResponse = new CustomExceptionResponse(e.getMessage(), e.getCustomData());
        addDebugInfo(request, e, customExceptionResponse);
        log.error(customExceptionResponse.toString());
        return createResponse(customExceptionResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
