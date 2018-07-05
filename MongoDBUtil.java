package org.onosproject.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.reflect.Type;


/**
 * Created by root on 11/28/17.
 */
public class MongoDBUtil<T> {

    private static Map<String, Map<String, Long>> databaseMap = new HashMap<>();
    private static ReentrantLock databaseMapLock = new ReentrantLock();
    static MongoClient mongoClient = null;
    private final static Logger log = LoggerFactory.getLogger(MongoDBUtil.class);
    private static int globalCommitId = 0;
    private static Bson globalCommitIdFilter = Filters.eq("key", MagicBox.MAXCOMMITIDKEY);
    private static MongoCollection<Document> globalCommitIdCollection = null;
    private static Document globalCommitIdDocument = null;
    private static ReentrantLock idLock = new ReentrantLock();

    public static Integer getId() {
        int result = 0;

        idLock.lock();
        try {
            if (globalCommitId == Integer.MAX_VALUE)
                globalCommitId = 0;
            result = globalCommitId++;

            globalCommitIdDocument.put(MagicBox.MAXCOMMITIDKEY, result);
            globalCommitIdCollection.replaceOne(globalCommitIdFilter, globalCommitIdDocument);
        } catch (Exception e) {
            log.error("Get global commit index error: {} {}", e.getMessage(), e.getStackTrace());
        } finally {
            idLock.unlock();
        }

        return result;
    }

    /*
     * 代表MongoDB结构层次《DB->Collection->Document》的导向索引，是整个数据库结构的缓存。
     * TODO：考虑在人为改变数据库数据和结构的情况下，如何自动同步更新databaseMap？
     */

    static {
        try {

            Integer mongoPort = 27017;
            String mongoIp = System.getenv("MONGODB_SERVER");
            List<ServerAddress> addresses = new ArrayList<>();
            if (null == mongoIp || "".equals(mongoIp)) {
                mongoIp = "127.0.0.1";
                ServerAddress serverAddress = new ServerAddress(mongoIp, mongoPort);
                addresses.add(serverAddress);
            } else {
                String[] args = mongoIp.split("#");
                for(String single : args) {
                    String[] arg = single.split(":");
                    mongoIp = arg[0];
                    mongoPort = Integer.valueOf(arg[1]);
                    ServerAddress serverAddress = new ServerAddress(mongoIp, mongoPort);
                    addresses.add(serverAddress);
                }

            }
            log.info("mongodb {}", addresses);
            MongoCredential credential = MongoCredential.createCredential("sdwan", "admin", "sdwan".toCharArray());
            mongoClient = new MongoClient(addresses, Arrays.asList(credential));
            if (null == mongoClient) {
                log.error("=======!!!!!!!!mongoClient is null,because connection");
            } else {
                flushDBMap();
                log.info("mongodb map: {}", databaseMap);

                globalCommitIdCollection = mongoClient.getDatabase(Constants.sdwan.DBNAME).getCollection(Constants.sdwan.MAGICBOX);
                globalCommitIdDocument = globalCommitIdCollection.find(globalCommitIdFilter).first();
                if (globalCommitIdDocument == null) {
                    Document document = new Document();
                    document.put(MagicBox.MAXCOMMITIDKEY, 0);
                    document.put("key", MagicBox.MAXCOMMITIDKEY);
                    globalCommitIdCollection.insertOne(document);
                    globalCommitIdDocument = globalCommitIdCollection.find(globalCommitIdFilter).first();
                }
                globalCommitId = globalCommitIdDocument.getInteger(MagicBox.MAXCOMMITIDKEY);
            }
        } catch (Exception e) {
            log.error("MongoDBUtil=== {}",e);
        }
    }

    public static void flushDBMap() {
        databaseMapLock.lock();

        try {
            MongoCursor<String> databaseIter = mongoClient.listDatabaseNames().iterator();
            while (databaseIter.hasNext()) {
                String databaseName = databaseIter.next();
                MongoDatabase database = mongoClient.getDatabase(databaseName);

                Map<String, Long> collectionMap = new HashMap<>();

                MongoCursor<String> collectionIter = database.listCollectionNames().iterator();
                while (collectionIter.hasNext()) {
                    String collectionName = collectionIter.next();
                    MongoCollection<Document> collection = database.getCollection(collectionName);

                    collectionMap.put(collectionName, collection.count());
                }

                databaseMap.put(databaseName, collectionMap);
            }
        } catch (Exception e) {
            log.error("flush database map error: {} {}", e.getMessage(), e.getStackTrace());
        } finally {
            databaseMapLock.unlock();
        }

    }

    public static void flushDBMap(String databaseName) {
        databaseMapLock.lock();

        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            Map<String, Long> collectionMap = new HashMap<>();

            MongoCursor<String> collectionIter = database.listCollectionNames().iterator();
            while (collectionIter.hasNext()) {
                String collectionName = collectionIter.next();
                MongoCollection<Document> collection = database.getCollection(collectionName);

                collectionMap.put(collectionName, collection.count());
            }

            databaseMap.put(databaseName, collectionMap);
        } catch (Exception e) {
            log.error("flush database map error: {} {}", e.getMessage(), e.getStackTrace());
        } finally {
            databaseMapLock.unlock();
        }
    }

    public static void flushDBMap(String databaseName, String collectionName) {
        databaseMapLock.lock();

        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            MongoCollection<Document> collection = database.getCollection(collectionName);

            Map<String, Long> collectionMap = databaseMap.get(databaseName);
            if (collectionMap == null) {
                collectionMap = new HashMap<>();
            }

            collectionMap.put(collectionName, collection.count());
            databaseMap.put(databaseName, collectionMap);
        } catch (Exception e) {
            log.error("flush database map error: {} {}", e.getMessage(), e.getStackTrace());
        } finally {
            databaseMapLock.unlock();
        }
    }

    public static Map<String, Map<String, Long>> getDBMap() {
        return databaseMap;
    }

    /**
     * 获取DB实例 - 指定DB
     *
     * @param databaseName
     * @return
     */
    public static MongoDatabase getDB(String databaseName) {
        if (databaseName != null && !"".equals(databaseName)) {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            return database;
        }
        return null;
    }

    /**
     * 获取collection对象 - 指定Collection
     *
     * @param collectionName
     * @return
     */
    public static MongoCollection<Document> getCollection(String databaseName, String collectionName) {
        if (null == collectionName || "".equals(collectionName)) {
            return null;
        }
        if (null == databaseName || "".equals(databaseName)) {
            return null;
        }
        MongoCollection<Document> collection = mongoClient.getDatabase(databaseName).getCollection(collectionName);
        return collection;
    }


    public static <T> Map<String, Adapter<T>> getCollection(String databaseName, String collectionName, Type type) {
        Map<String, Adapter<T>> map = new HashMap<>();
        if (null == collectionName || "".equals(collectionName)) {
            return null;
        }
        if (null == databaseName || "".equals(databaseName)) {
            return null;
        }

        MongoCollection<Document> docs = getCollection(databaseName, collectionName);
        FindIterable<Document> findIterable = docs.find();
        MongoCursor<Document> mongoCursor = findIterable.iterator();
        while (mongoCursor.hasNext()) {
            Document doc = mongoCursor.next();
            map.put(doc.getString("key"), GsonUtil.parseJsonWithGsonType(doc.toJson(), type));
        }
        return map;

    }

    /**
     * @param
     * @param key
     * @return
     */
    public static <T> Adapter<T> findByKey(String databaseName, String collectionName, String key, Type type) throws DBException {
        MongoCollection<Document> coll = getCollection(databaseName, collectionName);
        if (null == coll) {
            return null;
        }

        Adapter<T> result = null;

        try {
            Document myDoc = coll.find(Filters.eq("key", key)).first();
            if (myDoc == null) {
                return null;
            }
            result = GsonUtil.parseJsonWithGsonType(myDoc.toJson(), type);
        } catch (Exception e) {
            String errMesseage = String.format("database %s collection %s find by key: %s error: %s %s",
                    databaseName, collectionName, key, e.getMessage(),e.getStackTrace());
            throw new DBException(errMesseage);
        }

        return result;
    }

    public static int deleteByKey(String databaseName, String collectionName, String key) throws DBException {
        MongoCollection<Document> coll = getCollection(databaseName, collectionName);
        if (null == coll) {
            return 0;
        }

        int count = 0;

        try {
            Bson filter = Filters.eq("key", key);
            DeleteResult deleteResult = coll.deleteOne(filter);
            count = (int) deleteResult.getDeletedCount();
        } catch (Exception e) {
            String errMessage = String.format("database %s collection %s delete by key: %s error: %s %s",
                    databaseName, collectionName, key, e.getMessage(), e.getStackTrace());
            throw new DBException(errMessage);
        }

        flushDBMap(databaseName);

        return count;
    }

    public static <T> Document insertOne(String databaseName, String collectionName, String key, T t) throws DBException {
        MongoCollection<Document> coll = getCollection(databaseName, collectionName);
        if (null == coll) {
            return null;
        }

        Document doc = null;

        try {
            deleteByKey(databaseName, collectionName, key);

            if (t instanceof Document) {
                doc = (Document) t;
            } else {
                doc = Document.parse(GsonUtil.toJson(t));
            }

            doc.append("key", key);
            coll.insertOne(doc);
        } catch (Exception e) {
            String errMessage = String.format("database %s collection %s insert document: %s error: %s %s",
                    databaseName, collectionName, doc, e.getMessage(), e.getStackTrace());
            throw new DBException(errMessage);
        }

        flushDBMap(databaseName);

        return doc;
    }

    public static void deleteDB(String databaseName) throws DBException {
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            if (database != null) {
                database.drop();
                flushDBMap();
            }
        } catch (Exception e) {
            String errMessage = String.format("database %s delete error: %s %s", databaseName, e.getMessage(), e.getStackTrace());
            throw new DBException(errMessage);
        }
    }

    public static void createDB(String databaseName) throws DBException {
        try {
            MongoDatabase database = mongoClient.getDatabase(databaseName);
            if (database != null) {
                database.createCollection("placeholder");
            }
            flushDBMap();
        } catch (Exception e) {
            String errMessage = String.format("database %s create error: %s %s", databaseName, e.getMessage(), e.getStackTrace());
            throw new DBException(errMessage);
        }
    }

    public static <T> Document updateByKey(String databaseName, String collectionName, String key, T t) {
        MongoCollection<Document> coll = getCollection(databaseName, collectionName);
        if (null == coll) {
            return null;
        }
        Bson filter = Filters.eq("key", key);
        Document newdoc = Document.parse(GsonUtil.toJson(t));
        newdoc.append("key", key);
        coll.updateOne(filter, new Document("$set", newdoc));
        return newdoc;
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
        log.info("===============MongoDBUtil关闭连接========================");
    }
}
