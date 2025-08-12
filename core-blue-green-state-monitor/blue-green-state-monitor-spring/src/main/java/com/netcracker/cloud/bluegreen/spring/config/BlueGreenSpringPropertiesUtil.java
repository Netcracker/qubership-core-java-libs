package com.netcracker.cloud.bluegreen.spring.config;

public class BlueGreenSpringPropertiesUtil {
    protected static final String CONSUL_URL_PROPERTY_SPEL = "${consul.url:${CONSUL_URL:}}";
    protected static final String NAMESPACE_PROPERTY_SPEL = "${cloud.microservice.namespace:${NAMESPACE:}}";
    protected static final String MS_NAME_PROPERTY_SPEL = "${cloud.microservice.name:${SERVICE_NAME:}}";
    protected static final String POD_NAME_PROPERTY_SPEL = "${pod.name:${POD_NAME:}}";
}
