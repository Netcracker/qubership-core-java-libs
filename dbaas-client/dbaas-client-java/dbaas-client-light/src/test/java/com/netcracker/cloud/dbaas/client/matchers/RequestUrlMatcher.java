package org.qubership.cloud.dbaas.client.matchers;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.ByteString;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.Arrays;


public class RequestUrlMatcher implements ArgumentMatcher<Request> {
    private Request expected;

    public RequestUrlMatcher(Request expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(Request actual) {
        if(expected.body() != null && actual.body() != null) {
            final Buffer bufferExpected = new Buffer();
            final Buffer bufferActual = new Buffer();
            try {
                expected.body().writeTo(bufferExpected);
                actual.body().writeTo(bufferActual);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return expected.url().toString().equals(actual.url().toString()) &&
                    expected.method().equals(actual.method()) &&
                    bufferActual.readUtf8().equals(bufferExpected.readUtf8());
        }
        else
            return expected.url().toString().equals(actual.url().toString()) && expected.method().equals(actual.method());
    }
}
