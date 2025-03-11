package org.qubership.cloud.disableapi.quarkus.annotations;

import org.qubership.cloud.disableapi.quarkus.annotations.profiles.DisabledProfile;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.Method;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DisabledProfile.class)
class TestDisabled extends AbstractAnnotationsTest {
    
    @Test
    void v1Test() {
        test("/api/v1/test", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v1/test", Method.POST, 200, CoreMatchers.containsString("ok"));
        test("/api/v1/test/inner", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v1/test/inner/wildcard/param1", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v1/test/inner/extension/example.html", Method.GET, 200, CoreMatchers.containsString("ok"));
    }
    @Test
    void v2Test() {
        test("/api/v2/test", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v2/test", Method.POST, 200, CoreMatchers.containsString("ok"));
        test("/api/v2/test/inner", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v2/test/inner/wildcard/param1", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v2/test/inner/extension/example.html", Method.GET, 200, CoreMatchers.containsString("ok"));
    }

    @Test
    void v3Test() {
        test("/api/v3/test", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v3/test", Method.POST, 200, CoreMatchers.containsString("ok"));
        test("/api/v3/test/inner", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v3/test/inner/wildcard/param1", Method.GET, 200, CoreMatchers.containsString("ok"));
        test("/api/v3/test/inner/extension/example.html", Method.GET, 200, CoreMatchers.containsString("ok"));
    }
}
