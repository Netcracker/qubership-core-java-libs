package com.netcracker.cloud.consul.provider.common;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import com.netcracker.cloud.consul.provider.common.client.ConsulClient;
import com.netcracker.cloud.consul.provider.common.client.ConsulClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.OffsetDateTime;

//todo vlla напомни, чем мы руководствовались, выделяя SelfTokenReader в отдельный класс? Почему мы не оставили getSelfToken рядом с логином? Разве так было бы не проще с точки зрения кода?
public class SelfTokenReader {

    private static final Logger log = LoggerFactory.getLogger(SelfTokenReader.class);

    private final ConsulClient client;

    public SelfTokenReader(ConsulClient client) {
        this.client = client;
    }

    public Token read(String currentSecretId) throws IOException {
        ConsulClientResponse response = client.getSelfToken(currentSecretId);
        String bodyJson = response.getBodyJson();
        if (response.getCode() != 200) {
            throw new IOException(String.format("can not get self token from consul; response code=%s; body='%s'", response.getCode(), bodyJson));
        }

        String secretId = JsonPath.read(bodyJson, "$.SecretID");
        OffsetDateTime expirationTime = null;
        try {
            expirationTime = OffsetDateTime.parse(JsonPath.read(bodyJson, "$.ExpirationTime"));
        } catch (PathNotFoundException ex) {
            // No Expiration Time. Nothing to do.
        }
        log.debug("Got self token from Consul");
        return new Token(secretId, expirationTime);
    }
}
