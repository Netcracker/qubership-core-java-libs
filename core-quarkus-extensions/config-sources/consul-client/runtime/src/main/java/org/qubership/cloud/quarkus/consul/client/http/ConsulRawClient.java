package org.qubership.cloud.quarkus.consul.client.http;

import org.qubership.cloud.quarkus.consul.client.model.GetValue;

import java.util.*;

public class ConsulRawClient {

    private final HttpTransport httpTransport;
    private final String agentAddress;

    public ConsulRawClient(String consulUrl) {
        this(new HttpTransport(), consulUrl);
    }

    protected ConsulRawClient(HttpTransport httpTransport, String consulUrl) {
        this.httpTransport = httpTransport;
        String consulUrlLowercase = consulUrl.toLowerCase();
        if (!consulUrlLowercase.startsWith("https://") && !consulUrlLowercase.startsWith("http://")) {
            consulUrlLowercase = "http://" + consulUrlLowercase;
        }

        this.agentAddress = consulUrlLowercase;
    }

    public Response<List<GetValue>> makeGetRequest(String endpoint, QueryParams queryParams) {
        String url = agentAddress + endpoint;
        url = generateUrl(url, queryParams);
        return httpTransport.makeGetRequest(url);
    }

    public static String generateUrl(String baseUrl, QueryParams queryParams) {
        List<String> allParams = new ArrayList<>(queryParams.toUrlParameters());
        StringBuilder result = new StringBuilder(baseUrl);

        Iterator<String> paramsIterator = allParams.iterator();
        if (paramsIterator.hasNext()) {
            result.append("?").append(paramsIterator.next());
            while (paramsIterator.hasNext()) {
                result.append("&").append(paramsIterator.next());
            }
        }
        return result.toString();
    }

}
