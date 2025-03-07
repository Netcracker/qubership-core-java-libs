package org.qubership.cloud.mongoevolution;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.rules.ExternalResource;

public class JavaMongoServerExternalResource extends ExternalResource {

    private final MongoServer mongoServer = new MongoServer(new MemoryBackend());

    @Override
    protected void before() throws Throwable {
        mongoServer.bind();
    }

    @Override
    protected void after() {
        mongoServer.shutdown();
    }

    public MongoServer getMongoServer() {
        return mongoServer;
    }
}