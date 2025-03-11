package org.qubership.cloud.context.propagation.quarkus.runtime.filter;

import org.qubership.cloud.context.propagation.quarkus.it.reactive.ReactiveClient;
import org.qubership.cloud.headerstracking.filters.context.AcceptLanguageContext;
import org.qubership.cloud.headerstracking.filters.context.RequestIdContext;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.qubership.cloud.context.propagation.quarkus.it.reactive.RestController.TEST_ENDPOINT;
import static org.hamcrest.core.Is.is;

@QuarkusTest
public class QuarkusContextInitializationReactiveTest {

    @RestClient
    ReactiveClient postmanClient;

    @Test
    void testContextInitialization() {
        String xRequestId = "X-Request-Id";
        String xRequestIdValue = "123";
        RequestIdContext.set(xRequestIdValue);
        String acceptLanguage = "Accept-Language";
        String acceptLanguageValue = "ru;kz";
        RestAssured.given()
                .header(xRequestId, xRequestIdValue)
                .header(acceptLanguage, acceptLanguageValue)
                .get(TEST_ENDPOINT)
                .then()
                .statusCode(200)
                .body(xRequestId, is(xRequestIdValue))
                .body(acceptLanguage, is(acceptLanguageValue));
    }

    @Test
    void testContextInitializationReactive() {
        String xRequestId = "X-Request-Id";
        String xRequestIdValue = "696";
        RequestIdContext.set(xRequestIdValue);
        String acceptLanguage = "Accept-Language";
        String acceptLanguageValue = "ru;fr";
        RestAssured.given()
                .header(xRequestId, xRequestIdValue)
                .header(acceptLanguage, acceptLanguageValue)
                .get(TEST_ENDPOINT + "/reactive")
                .then()
                .statusCode(200)
                .body(xRequestId, is(xRequestIdValue))
                .body(acceptLanguage, is(acceptLanguageValue));
    }

    @Test
    void testContextPropagatedViaRestClient() {
        RequestIdContext.set("007");
        AcceptLanguageContext.set("ru;kz");
        Response response = postmanClient.getHeaders();
        Assertions.assertEquals(200, response.getStatus());
        Map<String, List<String>> entity = response.readEntity(new GenericType<>() {
        });
        Assertions.assertEquals("007", entity.get("X-Request-Id").get(0));
        Assertions.assertEquals("ru;kz", entity.get("Accept-Language").get(0));
    }

    @Test
    void testContextPropagatedViaReactiveRestClient() {
        RequestIdContext.set("001");
        AcceptLanguageContext.set("ru;us");
        Response response = postmanClient.getReactiveHeaders().await().indefinitely();
        Assertions.assertEquals(200, response.getStatus());
        Map<String, List<String>> entity = response.readEntity(new GenericType<>() {
        });
        Assertions.assertEquals("001", entity.get("X-Request-Id").get(0));
        Assertions.assertEquals("ru;us", entity.get("Accept-Language").get(0));
    }
}
