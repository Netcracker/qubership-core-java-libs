package org.qubership.cloud.bluegreen.impl.http;

public class StringResponseBodyHandler extends ResponseHandler<String> {
    public static StringResponseBodyHandler INSTANCE = new StringResponseBodyHandler();

    private StringResponseBodyHandler() {
        super(String.class);
    }
}
