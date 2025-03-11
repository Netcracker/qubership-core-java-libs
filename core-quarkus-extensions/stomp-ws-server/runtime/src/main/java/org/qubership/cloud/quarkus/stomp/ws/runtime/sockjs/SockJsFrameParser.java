package org.qubership.cloud.quarkus.stomp.ws.runtime.sockjs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.stomp.StompServerOptions;
import io.vertx.ext.stomp.impl.FrameParser;
import org.jboss.logging.Logger;

public class SockJsFrameParser extends FrameParser {

    private static final Logger log = Logger.getLogger(SockJsFrameParser.class.getName());


    private ObjectMapper objectMapper = new ObjectMapper();

    public SockJsFrameParser(StompServerOptions options) {
        super(options);
    }

    @Override
    public synchronized void handle(Buffer event) {
        log.debugf("Receive message (before decode): " + event);
        String[] strings;
        try {
            strings = objectMapper.readValue(event.toString(), String[].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Buffer buffer = Buffer.buffer(strings[0]);
        log.debugf("Receive message (after decode): " + buffer);
        super.handle(buffer);
    }
}
