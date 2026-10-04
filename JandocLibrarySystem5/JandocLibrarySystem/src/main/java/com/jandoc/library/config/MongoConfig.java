package com.jandoc.library.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Manages the lifecycle of the single shared MongoDB client connection
 * used throughout the Jandoc Library Management System.
 */
public class MongoConfig {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "millerLibraryDB";

    private static MongoClient mongoClient = null;

    public static MongoDatabase getDatabase() {
        if (mongoClient == null) {
            mongoClient = MongoClients.create(CONNECTION_STRING);
        }
        return mongoClient.getDatabase(DATABASE_NAME);
    }

    public static void closeConnection() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
    }
}
