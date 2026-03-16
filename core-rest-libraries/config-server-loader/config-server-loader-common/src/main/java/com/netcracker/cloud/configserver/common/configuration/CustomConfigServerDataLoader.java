package com.netcracker.cloud.configserver.common.configuration;

import com.netcracker.cloud.restclient.HttpMethod;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.entity.RestClientResponseEntity;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientException;
import com.netcracker.cloud.restclient.exception.MicroserviceRestClientResponseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServerConfigDataLoader;
import org.springframework.cloud.config.client.ConfigServerConfigDataResource;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomConfigServerDataLoader extends ConfigServerConfigDataLoader {
    private static final Log logger = LogFactory.getLog(CustomConfigServerDataLoader.class);

    public CustomConfigServerDataLoader() {
        super(destination -> logger);
    }

    @Override
    protected Environment getRemoteEnvironment(ConfigDataLoaderContext context, ConfigServerConfigDataResource resource, String label, String state) {
        String microserviceName = getMicroserviceName(context);
        ConfigClientProperties properties = resource.getProperties();
        String path = "/{name}/{profile}";
        String profile = resource.getProfiles();
        int noOfUrls = properties.getUri().length;
        if (noOfUrls > 1) {
            logger.info("Multiple Config Server Urls found listed.");
        }

        MicroserviceRestClient microserviceRestClient = context.getBootstrapContext().get(MicroserviceRestClient.class);
        Map<String, Object> args = new HashMap<>();
        args.put("name", microserviceName);
        args.put("profile", profile);

        if (StringUtils.hasText(label)) {
            if (label.contains("/")) {
                label = label.replace("/", "(_)");
            }
            path = path + "/{label}";
            args.put("label", label);
        }
        RestClientResponseEntity<Environment> response = null;

        for (int i = 0; i < noOfUrls; i++) {
            ConfigClientProperties.Credentials credentials = properties.getCredentials(i);
            String uri = credentials.getUri();
            // it's bootstrap time and at this time logger does not work. That's why we use standard output
            System.out.println("Fetching config from server at : " + uri);
            response = doRetryRequest(path, microserviceRestClient, args, uri, context);
            logger.debug("Response: " + response);
            if (response == null || response.getHttpStatus() != HttpStatus.OK.value()) {
                throw new RuntimeException("Response from config server was empty or response status code was not 200");
            } else {
                return response.getResponseBody();
            }
        }

        return null;
    }

    private String getMicroserviceName(ConfigDataLoaderContext context) {
        Binder binder = context.getBootstrapContext().get(Binder.class);
        BindResult<String> serviceNameBindResult = binder.bind("cloud.microservice.name", String.class);
        return serviceNameBindResult.orElseThrow(() -> new IllegalStateException("you must set 'cloud.microservice.name' parameter"));
    }

    private RestClientResponseEntity<Environment> doRetryRequest(String path, MicroserviceRestClient microserviceRestClient, Map<String, Object> args, String uri, ConfigDataLoaderContext context) {

        Binder binder = context.getBootstrapContext().getOrElse(Binder.class, null);
        BindHandler bindHandler = context.getBootstrapContext().getOrElse(BindHandler.class, null);
        Integer maxNumberOfAttempts = binder.bind("core.spring.cloud.config.retry.max-attempts", Bindable.of(Integer.class), bindHandler).orElse(12);
        Long delayTime = binder.bind("core.spring.cloud.config.retry.max-interval-ms", Bindable.of(Long.class), bindHandler).orElse(5000L);

        RuntimeException ex;
        for (int attempt = 1; attempt <= maxNumberOfAttempts; attempt++) {
            try {
                Map<String, List<String>> headers = new HashMap<>();
                headers.put("Accept", Collections.singletonList("application/json"));
                return microserviceRestClient.doRequest(uri + path, HttpMethod.GET, headers, null, Environment.class, args);
            } catch (MicroserviceRestClientResponseException e) {
                System.out.println("Caught exception during request to config server with http status: " + e.getHttpStatus()
                        + ", and body: " + e.getResponseBodyAsString() + "retry attempt " + attempt + " out of 12");
                ex = e;
            } catch (MicroserviceRestClientException e) {
                System.out.println("Can't connect to config server, retry attempt " + attempt + " out of 12");
                ex = e;
            }
            if (attempt == maxNumberOfAttempts) {
                throw ex;
            }
            try {
                Thread.sleep(delayTime);
            } catch (InterruptedException err) {
                System.err.println("Error with Tread.sleep occurred during retry");
                Thread.currentThread().interrupt();
            }
        }
        return null;
    }

}
