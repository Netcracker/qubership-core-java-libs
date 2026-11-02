package com.netcracker.cloud.quarkus.dbaas.cassandraclient.it;

import com.datastax.oss.quarkus.test.CassandraTestResource;

import java.util.Map;

/**
 * Since Quarkus 3.35 the values returned by {@code start()} are lost with {@code quarkus.test.flat-class-path=true},
 * which this module needs, so they also go to system properties.
 */
public class CassandraContainerResource extends CassandraTestResource {

    private Map<String, String> properties = Map.of();

    @Override
    public Map<String, String> start() {
        properties = super.start();
        properties.forEach(System::setProperty);
        return properties;
    }

    @Override
    public void stop() {
        properties.keySet().forEach(System::clearProperty);
        super.stop();
    }
}
