package org.qubership.cloud.dbaas.client.test;


public class TestConstants {

    public final static String DB_NAME = "dbName";
    public final static String DB_USER = "dbaas";
    public final static String AUTH_DB_NAME = "admin";
    public final static String DB_PASSWORD = "dbaas";
    public final static String MONGO_TEST_URI = "mongodb://"
            + DB_USER + ":" + DB_PASSWORD + "@localhost:27017,localhost:27018/";
    public final static String POSTGRES_TEST_URI = "jdbc:postgresql://localhost/";

}
