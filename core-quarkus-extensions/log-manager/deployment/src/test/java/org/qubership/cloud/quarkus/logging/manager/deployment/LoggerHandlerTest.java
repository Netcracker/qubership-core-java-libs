package org.qubership.cloud.quarkus.logging.manager.deployment;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;


@QuarkusTest
public class LoggerHandlerTest {

    @Test
    public void testGetLoggersEndpoint() {
        Map response =
                given()
                        .when().get("/api/logging/v1/levels")
                        .then()
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .extract().as(Map.class);

        Assertions.assertNotNull(response);
    }
}
