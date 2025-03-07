package org.qubership.cloud.mongoevolution.java;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.After;
import org.junit.Before;

import java.net.InetSocketAddress;

public class MongoServerConfiguration {
    protected MongoServer server;
    protected MongoClient mongoClient;

    @Before
    public void setUp() {
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress bind = server.bind();
        // bind on a random local port
        mongoClient = MongoClients.create("mongodb:/" + server.getLocalAddress());
    }

    @After
    public void tearDown() {
        mongoClient.close();
        server.shutdownNow();
    }

}
