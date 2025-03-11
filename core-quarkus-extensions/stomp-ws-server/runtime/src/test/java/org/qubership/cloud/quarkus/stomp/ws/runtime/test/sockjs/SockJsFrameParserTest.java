package org.qubership.cloud.quarkus.stomp.ws.runtime.test.sockjs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.quarkus.stomp.ws.runtime.sockjs.SockJsFrameParser;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.impl.RecordParserImpl;
import io.vertx.ext.stomp.StompServerOptions;
import io.vertx.ext.stomp.impl.FrameParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;

class SockJsFrameParserTest {

    StompServerOptions stompServerOptions;

    SockJsFrameParser sockJsFrameParser;

    @BeforeEach
    void setUp() {
        stompServerOptions = mock(StompServerOptions.class);
        sockJsFrameParser = new SockJsFrameParser(stompServerOptions);
    }

    @Test
    void handleMessageBuffer() throws Exception {
        String[] strarray = new String[2];
        strarray[0] = "message";
        strarray[1] = "message2";
        StompServerOptions options = new StompServerOptions();
        SockJsFrameParser sockJsFrameParser2 = Mockito.spy(new SockJsFrameParser(options));
        sockJsFrameParser2.handle(Buffer.buffer(new ObjectMapper().writeValueAsBytes(strarray)));

        Field frameParserField = FrameParser.class.getDeclaredField("frameParser");
        frameParserField.setAccessible(true);
        RecordParserImpl frameParser = (RecordParserImpl) frameParserField.get(sockJsFrameParser2);

        Field buffField = RecordParserImpl.class.getDeclaredField("buff");
        buffField.setAccessible(true);
        Buffer buff = (Buffer) buffField.get(frameParser);
        Assertions.assertEquals(strarray[0], buff.toString());
    }

    @Test
    void testHandle_whenBufferIsNotArrayOfString_throwException() throws JsonProcessingException {
        Buffer buffer = Buffer.buffer(new ObjectMapper().writeValueAsBytes("message"));
        Assertions.assertThrows(RuntimeException.class, () -> sockJsFrameParser.handle(buffer));
    }
}
