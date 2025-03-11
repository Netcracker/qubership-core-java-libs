package org.qubership.cloud.quarkus.consul.client.http;

import java.util.ArrayList;
import java.util.List;

public class QueryParams{

    private long waitTime;
    private long index;
    private String token;

    public void setToken(String token) {
        this.token = token;
    }

    public long getWaitTime() {
        return waitTime;
    }

    public long getIndex() {
        return index;
    }

    public String getToken() {
        return token;
    }

    public QueryParams( long waitTime, long index) {
        if (waitTime < -1 || waitTime > 600)
            throw new IllegalArgumentException("waitTime parameter must be greater than or equal to -1 and less than 600s, " +
                    "but passed value is " + waitTime);

        this.waitTime = waitTime;
        this.index = index;
    }

    public List<String> toUrlParameters() {
        List<String> params = new ArrayList<String>();

        if (waitTime != -1) {
            params.add("wait=" + waitTime + "s");
        }

        if (index != -1) {
            params.add("index=" + Long.toUnsignedString(index));
        }

        if (token != null) {
            params.add("token=" + token);
        }
        params.add("recurse");
        return params;
    }
}
