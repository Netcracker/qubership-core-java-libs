package org.qubership.cloud.springcloud.config.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.qubership.cloud.quarkus.security.auth.M2MManager;
import org.qubership.cloud.security.core.auth.Token;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;
import okhttp3.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

public class ConfigServerClientImpl implements ConfigServerClient {

    SimpleDateFormat formatter = new SimpleDateFormat("[yyyy-MM-dd'T'HH:mm:ss.SSS]");

    private final int MAX_TRIES = 25;
    private final int DELAY = 500;

    private OkHttpClient client;
    private ObjectMapper mapper;
    private URL url;

    public ConfigServerClientImpl(String csUrl) throws MalformedURLException {
        client = new OkHttpClient.Builder()
                .connectionSpecs(Collections.singletonList(
                        csUrl.startsWith("https") ? ConnectionSpec.COMPATIBLE_TLS : ConnectionSpec.CLEARTEXT)
                )
                .sslSocketFactory(TlsUtils.getSslContext().getSocketFactory(), TlsUtils.getTrustManager())
                .build();
        final Config cfg = ConfigProvider.getConfig();
        String appName = cfg.getValue("cloud.microservice.name", String.class);
        url = new URL(csUrl + "/" + appName + "/default");
        mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    }

    @Override
    public CloudEnv getProperties() {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            String body = processRequest(request);
            CloudEnv env = mapper.readValue(body, CloudEnv.class);
            return env;
        } catch (IOException e) {
            logError("Error during loading config properties " + e.getMessage());
            return ConfigServerClientInitStub.EMPTY_CLOUD_ENV;
        }
    }

    @Override
    public void putProperties(Map<String, String> properties) {
        try {
            RequestBody body = RequestBody.create(MediaType.parse(APPLICATION_JSON), mapper.writeValueAsString(properties));
            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .build();
            processRequest(request);
        } catch (JsonProcessingException e) {
            logError("Error during converting map to json " + e.getMessage());
        } catch (IOException e) {
            logError("Error during put config properties " + e.getMessage());
        }
    }

    private String processRequest(Request request) throws IOException {
        int count = 1;
        while (true) {
            try {
                Token token = M2MManager.getInstance().getToken();
                request = request.newBuilder()
                        .addHeader("Authorization", token.getTokenType() + " " + token.getTokenValue())
                        .build();
                Response response = client.newCall(request).execute();
                return response.body().string();
            } catch (Exception e) {
                try {
                    logError("Error during loading config properties " + e.getMessage() + ". Retry: " + count + "/" + MAX_TRIES);
                    logError(e);
                    Thread.sleep(DELAY);
                } catch (InterruptedException interruptedException) {
                    throw new RuntimeException(interruptedException);
                }
                if (++count > MAX_TRIES) throw e;
            }
        }
    }

    private void logError(Exception e) {
        logError(ExceptionUtils.getStackTrace(e));
    }

    private void logError(String s) {
        // We use standard err output because during start time here logger don't init yet. Do not use logger here!!!
        System.err.println(formatter.format(new Date())
                + " [ERROR] [request_id=-] [tenant_id=] [thread=main] [class=ConfigServerClientImpl] "
                + s);
    }
}
