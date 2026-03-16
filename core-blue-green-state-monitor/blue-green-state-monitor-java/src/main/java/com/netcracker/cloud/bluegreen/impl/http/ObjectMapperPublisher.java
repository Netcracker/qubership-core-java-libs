package com.netcracker.cloud.bluegreen.impl.http;

import com.netcracker.cloud.bluegreen.impl.util.JsonUtil;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

public class ObjectMapperPublisher implements HttpRequest.BodyPublisher {
    private final HttpRequest.BodyPublisher delegate;

    public ObjectMapperPublisher(Object content) {
        this.delegate = HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(content), StandardCharsets.UTF_8);
    }

    @Override
    public long contentLength() {
        return delegate.contentLength();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
        delegate.subscribe(subscriber);
    }
}
