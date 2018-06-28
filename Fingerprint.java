package org.onosproject.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CreateCollectionOptions;
import com.mongodb.operation.CreateCollectionOperation;
import org.bson.Document;
import org.onosproject.mongodb.Constants.DevModel;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public final class Fingerprint {

    public enum Event {
        @SerializedName("Register")
        REGISTER("Register"),

        @SerializedName("Online")
        ONLINE("Online"),

        @SerializedName("Offline")
        OFFLINE("Offline"),

        @SerializedName("Change")
        CHANGE("Change");

        private String value;

        Event(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static Event fromValue(String text) {
            for (Event b : Event.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @SerializedName("date")
    private Date date;

    @SerializedName("event")
    private Event event;

    @SerializedName("description")
    private String description;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void record(DevModel devModel, String devSn) throws DBException {
        String DBName = Constants.sdwan.DBNAME;
        if (devSn != null && !devSn.isEmpty())
            DBName = Constants.runningDBName(devSn, devModel);

        if (this.date == null ||
                this.event == null)
            throw new DBException("Fingerprint is null");

        Boolean needCreate = Boolean.TRUE;
        MongoDatabase mongoDatabase = MongoDBUtil.getDB(DBName);
        if (mongoDatabase == null)
            throw new DBException("Database is not exist");

        MongoCursor<String> iterator = mongoDatabase.listCollectionNames().iterator();
        while (iterator.hasNext()) {
            String collectionName = iterator.next();
            if (collectionName.equals(Constants.sdwan.FINGERPRINT)) {
                needCreate = Boolean.FALSE;
                break;
            }
        }

        if (needCreate) {
            CreateCollectionOptions options = new CreateCollectionOptions();
            options.capped(true);
            options.sizeInBytes(131072); //128K
            options.maxDocuments(128);
            mongoDatabase.createCollection(Constants.sdwan.FINGERPRINT, options);
        }

        Document document = null;
        try {
            document = Document.parse(GsonUtil.toJson(this));
            document.append("key", MongoDBUtil.getId().toString());

            mongoDatabase.getCollection(Constants.sdwan.FINGERPRINT).insertOne(document);
        } catch (Exception e) {
            String errMessage = String.format("database %s collection %s insert document: %s error: %s %s",
                    mongoDatabase.getName(), Constants.sdwan.FINGERPRINT, document, e.getMessage(), e.getStackTrace());
            throw new DBException(errMessage);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
