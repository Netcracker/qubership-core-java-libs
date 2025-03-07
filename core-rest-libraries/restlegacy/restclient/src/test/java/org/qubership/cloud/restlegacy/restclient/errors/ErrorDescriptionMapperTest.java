package org.qubership.cloud.restlegacy.restclient.errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.restlegacy.restclient.error.ErrorsDescription;
import org.qubership.cloud.restlegacy.restclient.error.ProxyErrorException;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.util.UUID;

import static org.junit.Assert.*;

public class ErrorDescriptionMapperTest {

    private final String ERROR_CODE = "errorCode";
    private final String ERROR_MESSAGE = "errorMessage";
    private final String FIELD_NAME = "fieldName";
    private final String TEMPLATE_MESSAGE = "templateMessage";
    private final Object[] PARAMETERS = {"one parameter"};
    private final String errorDescriptionWithUnknowField = "{\n" +
            "\t\"errorId\": \"" + UUID.randomUUID() + "\",\n" +
            "\t\"date\": \"2017-12-06 16:26:22 PM UTC\",\n" +
            "\t\"service\": \"/api/v2/subscription-manager/customers/current/purchased-offering-instances/a7bec5c1-c134-41be-a7ce-b622b6eaa862/characteristics/newValues\",\n" +
            "\t\"status\": \"BAD_REQUEST\",\n" +
            "\t\"errors\": [{\n" +
            "\t\t\"errorCode\": \"" + ERROR_CODE + "\",\n" +
            "\t\t\"errorMessage\": \"" + ERROR_MESSAGE + "\",\n" +
            "\t\t\"fieldName\": \"" + FIELD_NAME + "\",\n" +
            "\t\t\"parameters\": [\"one parameter\"],\n" +
            "\t\t\"templateMessage\": \"" + TEMPLATE_MESSAGE + "\",\n" +
            "\t\t\"unknownField\": \"unknow message\"\n" +
            "\t}],\n" +
            "\t\"originalMessage\": null,\n" +
            "\t\"errorMessage\": null,\n" +
            "\t\"proxy\": true\n" +
            "}";

    @Test
    public void testUnknownField() throws JsonProcessingException {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, errorDescriptionWithUnknowField.getBytes(), null);
        ProxyErrorException ex = new ProxyErrorException(cause, "");

        assertNotNull(ex.getResponseEntity().getBody());
        ErrorsDescription errorsDescription = ex.getResponseEntity().getBody();

        assertNotNull(errorsDescription.getErrors().get(0));
        ErrorsDescription.ErrorDescription error = errorsDescription.getErrors().get(0);

        assertEquals(ERROR_MESSAGE, error.getErrorMessage());
        assertEquals(PARAMETERS, error.getParameters());
        assertEquals(ERROR_CODE, error.getErrorCode());
        assertEquals(FIELD_NAME, error.getFieldName());
    }

    @Test
    public void testErrorsDescriptionFieldsInResponse() throws JsonProcessingException {
        final HttpClientErrorException cause = new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE, null, errorDescriptionWithUnknowField.getBytes(), null);
        ProxyErrorException ex = new ProxyErrorException(cause, "");
        assertNotNull(ex.getResponseEntity().getBody());
        ErrorsDescription errorsDescription = ex.getResponseEntity().getBody();
        assertNotNull(errorsDescription.getErrorId());
        assertNotNull(errorsDescription.getService());

        String responseEntityJson = new ObjectMapper().writeValueAsString(new ResponseEntity<>(errorsDescription, HttpStatus.BAD_REQUEST).getBody());
        assertNotEquals(-1, responseEntityJson.indexOf("\"errorId\":"));
        assertEquals(-1, responseEntityJson.indexOf("\"service\":"));
        assertEquals(-1, responseEntityJson.indexOf("\"originalMessage\":"));
        assertEquals(-1, responseEntityJson.indexOf("\"stackTrace\":"));
    }
}
