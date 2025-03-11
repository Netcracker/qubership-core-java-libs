package org.qubership.cloud.quarkus.dbaas.mongoclient.config;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.event.CommandListener;
import com.mongodb.reactivestreams.client.ReactiveContextProvider;
import io.quarkus.mongodb.reactive.ReactiveMongoClient;
import io.quarkus.mongodb.runtime.MongoClientCustomizer;
import io.quarkus.mongodb.runtime.MongoClientSupport;
import io.quarkus.mongodb.runtime.MongoClients;
import io.quarkus.mongodb.runtime.MongodbConfig;
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Singleton;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.pojo.PropertyCodecProvider;

@Singleton
@Alternative
@Priority(1)
public class CustomNCMongoClients extends MongoClients {

    public CustomNCMongoClients(MongodbConfig mongodbConfig,
                                MongoClientSupport mongoClientSupport,
                                Instance<CodecProvider> codecProviders,
                                Instance<PropertyCodecProvider> propertyCodecProviders,
                                Instance<CommandListener> commandListeners,
                                Instance<ReactiveContextProvider> reactiveContextProviders,
                                @Any Instance<MongoClientCustomizer> customizers) {
        super(mongodbConfig, mongoClientSupport, codecProviders, propertyCodecProviders, commandListeners, reactiveContextProviders, customizers);
    }

    @Override
    public MongoClient createMongoClient(String clientName) throws MongoException {
        return null;
    }

    @Override
    public ReactiveMongoClient createReactiveMongoClient(String clientName) throws MongoException {
        return null;
    }
}
