package org.qubership.cloud.mongoevolution.java;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.InetSocketAddress;

public class MongoServerConfiguration {
    protected MongoServer server;
    protected MongoClient mongoClient;

    @BeforeEach
    void setUp() {
        server = new MongoServer(new MemoryBackend());
        InetSocketAddress bind = server.bind();
        // bind on a random local port
        mongoClient = MongoClients.create("mongodb:/" + server.getLocalAddress());
    }

    @AfterEach
    void tearDown() {
        mongoClient.close();
        server.shutdownNow();
    }

}
