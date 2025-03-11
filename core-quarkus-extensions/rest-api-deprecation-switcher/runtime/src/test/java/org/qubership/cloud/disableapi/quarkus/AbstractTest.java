package org.qubership.cloud.disableapi.quarkus;

import io.restassured.http.Method;
import org.hamcrest.Matcher;

import static io.restassured.RestAssured.given;

public abstract class AbstractTest {
    public static String MANDATORY_RESPONSE_STRING = "is declined with 404 Not Found, because the following deprecated";

    public void test(String uri, Method method, int expectedStatus, Matcher<String> bodyMatcher) {
        given().when().request(method, uri).then().statusCode(expectedStatus).body(bodyMatcher);
    }
}
