package org.qubership.cloud.quarkus.dbaas.cassandraclient.it;

import com.datastax.oss.quarkus.test.CassandraTestResource;
import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.List;

public class CassandraResourceProfile implements QuarkusTestProfile {

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(CassandraTestResource.class));
    }
}