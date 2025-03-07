//package org.qubership.cloud.smartclient.rest.webclient.sample;
//
//import org.qubership.cloud.context.propagation.core.ContextManager;
//import org.qubership.cloud.context.propagation.spring.webclient.interceptor.SpringWebClientInterceptor;
//import com.sun.net.httpserver.HttpServer;
//import org.junit.AfterClass;
//import org.junit.Assert;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
//import org.qubership.cloud.security.common.webclient.SmartWebClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
//import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
//import org.springframework.web.reactive.function.client.WebClient;
//
//import java.lang.reflect.Field;
//import java.net.HttpURLConnection;
//import java.net.InetSocketAddress;
//import java.util.List;
//import java.util.Objects;
//
//import static org.qubership.cloud.framework.contexts.tenant.TenantProvider.TENANT_CONTEXT_NAME;
//
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = {TestConfig.class})
//public class ApplicationTest {
//
//
//    @Autowired
//    SmartWebClient smartWebClient;
//    @Autowired
//    SpringWebClientInterceptor springWebClientInterceptor;
//
//    private static HttpServer httpServer;
//
//    @BeforeClass
//    public static void initServer() throws Exception {
//        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(""));
//        httpServer = HttpServer.create(new InetSocketAddress(8181), 0);
//
//        httpServer.createContext("/test", exchange -> {
//            byte[] response;
//            int responseCode;
//            switch (exchange.getRequestMethod()) {
//                case "OPTIONS":
//                    response = null;
//                    responseCode = HttpURLConnection.HTTP_OK;
//                    exchange.getResponseHeaders().add("Allow", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD");
//                    break;
//                case "HEAD":
//                    response = null;
//                    responseCode = HttpURLConnection.HTTP_NO_CONTENT;
//                    exchange.getResponseHeaders().add("test-header-name", "test-header-value");
//                    break;
//                default:
//                    response = "{\"success\": true}".getBytes();
//                    responseCode = HttpURLConnection.HTTP_OK;
//                    break;
//            }
//            exchange.sendResponseHeaders(responseCode, response == null ? 0 : response.length);
//            if (response != null) {
//                exchange.getResponseBody().write(response);
//            }
//            exchange.close();
//        });
//        httpServer.start();
//    }
//
//    @AfterClass
//    public static void stopHttpServer() {
//        httpServer.stop(0);
//    }
//
//    @Autowired
//    private WebClient userWebClient;
//
//    @Test
//    public void getWebClient() {
//        Assert.assertEquals(HttpStatus.OK, Objects.requireNonNull(userWebClient.get().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
//    }
//
//    @Test
//    public void postWebClient() {
//        Assert.assertEquals(HttpStatus.OK, Objects.requireNonNull(userWebClient.post().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
//    }
//
//    @Test
//    public void patchWebClient() {
//        ResponseEntity<Void> response = userWebClient.patch().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block();
//        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
//    }
//
//    @Test
//    public void deleteWebClient() {
//        Assert.assertEquals(HttpStatus.OK, Objects.requireNonNull(userWebClient.delete().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
//    }
//
//    @Test
//    public void optionWebClient() {
//        Assert.assertEquals(HttpStatus.OK, Objects.requireNonNull(userWebClient.options().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
//    }
//
//    @Test
//    public void headWebClient() {
//        Assert.assertEquals(HttpStatus.NO_CONTENT, Objects.requireNonNull(userWebClient.head().uri("http://localhost:8181/test").retrieve().toBodilessEntity().block()).getStatusCode());
//    }
//
//    @Test
//    public void smartWebClientInterceptorsTest() throws NoSuchFieldException, IllegalAccessException {
//        Field genericInterceptors = smartWebClient.getClass().getDeclaredField("genericInterceptors");
//        genericInterceptors.setAccessible(true);
//        List<ExchangeFilterFunction> interceptors = (List<ExchangeFilterFunction>) genericInterceptors.get(smartWebClient);
//        Assert.assertTrue(interceptors.contains(springWebClientInterceptor));
//    }
//}
