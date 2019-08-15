package com.grinderwolf.swm.plugin.loaders;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.log.Logging;
import com.mongodb.MongoException;
import com.mongodb.MongoNamespace;
import com.mongodb.client.*;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bukkit.configuration.ConfigurationSection;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MongoLoader implements SlimeLoader {

    private final MongoClient client;
    private final String database;
    private final String collection;

    MongoLoader(ConfigurationSection config) throws MongoException {
        String host = config.getString("host", "127.0.0.1");
        int port = config.getInt("port", 27017);

        String username = config.getString("username", "slimeworldmanager");
        String password = config.getString("password", "");
        String authSource = config.getString("auth", "admin");
        this.database = config.getString("database", "slimeworldmanager");
        this.collection = config.getString("collection", "worlds");

        this.client = MongoClients.create("mongodb://" + username + ":" + password + "@" + host + ":" + port + "/?authSource=" + authSource);

        MongoDatabase mongoDatabase = client.getDatabase(database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        mongoCollection.createIndex(Indexes.ascending("name"), new IndexOptions().unique(true));

        // Old GridFS importing
        for (String collectionName : mongoDatabase.listCollectionNames()) {
            if (collectionName.equals(collection + "_files.files") || collectionName.equals(collection + "_files.chunks")) {
                Logging.info("Updating MongoDB database...");

                mongoDatabase.getCollection(collection + "_files.files").renameCollection(new MongoNamespace(database, collection + ".files"));
                mongoDatabase.getCollection(collection + "_files.chunks").renameCollection(new MongoNamespace(database, collection + ".chunks"));

                Logging.info("MongoDB database updated!");

                break;
            }
        }
    }

    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException, WorldInUseException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Document worldDoc = mongoCollection.find(Filters.eq("name", worldName)).first();

            if (worldDoc == null) {
                throw new UnknownWorldException(worldName);
            }

            if (!readOnly) {
                if (worldDoc.getBoolean("locked")) {
                    throw new WorldInUseException(worldName);
                }

                mongoCollection.updateOne(Filters.eq("name", worldName), Updates.set("locked", true));
            }

            GridFSBucket bucket = GridFSBuckets.create(mongoDatabase, collection);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bucket.downloadToStream(worldName, stream);

            return stream.toByteArray();
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Document worldDoc = mongoCollection.find(Filters.eq("name", worldName)).first();

            return worldDoc != null;
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public List<String> listWorlds() throws IOException {
        List<String> worldList = new ArrayList<>();

        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

            MongoCursor<Document> documents = mongoCollection.find(Filters.text("name")).cursor();
            while(documents.hasNext()) {
                worldList.add(documents.next().getString("name"));
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }

        return worldList;
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            GridFSBucket bucket = GridFSBuckets.create(mongoDatabase, collection);
            GridFSFile oldFile = bucket.find(Filters.eq("filename", worldName)).first();

            if (oldFile != null) {
                bucket.rename(oldFile.getObjectId(), worldName + "_backup");
            }

            bucket.uploadFromStream(worldName, new ByteArrayInputStream(serializedWorld));

            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Document worldDoc = mongoCollection.find(Filters.eq("name", worldName)).first();

            if (worldDoc == null) {
                mongoCollection.insertOne(new Document().append("name", worldName).append("locked", lock));
            } else if (!worldDoc.getBoolean("lock") && lock) {
                mongoCollection.updateOne(Filters.eq("name", worldName), Updates.set("locked", true));
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void unlockWorld(String worldName) throws IOException, UnknownWorldException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            UpdateResult result = mongoCollection.updateOne(Filters.eq("name", worldName), Updates.set("locked", false));

            if (result.getMatchedCount() == 0) {
                throw new UnknownWorldException(worldName);
            }
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException, UnknownWorldException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            Document worldDoc = mongoCollection.find(Filters.eq("name", worldName)).first();

            if (worldDoc == null) {
                throw new UnknownWorldException(worldName);
            }

            return worldDoc.getBoolean("locked");
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws IOException, UnknownWorldException {
        try {
            MongoDatabase mongoDatabase = client.getDatabase(database);
            GridFSBucket bucket = GridFSBuckets.create(mongoDatabase, collection);
            GridFSFile file = bucket.find(Filters.eq("filename", worldName)).first();

            if (file == null) {
                throw new UnknownWorldException(worldName);
            }

            bucket.delete(file.getObjectId());

            MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);
            mongoCollection.deleteOne(Filters.eq("name", worldName));
        } catch (MongoException ex) {
            throw new IOException(ex);
        }
    }
}
