package org.onosproject.arrange.utils;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.arrange.api.DatabaseLock;
import org.onosproject.mongodb.Constants;

import java.util.List;

public class LockUtils {

    private static ServiceDirectory services = new DefaultServiceDirectory();
    protected static DatabaseLock databaseLock = services.get(DatabaseLock.class);

    public static class Locker {
        private Constants.DBType dbType;
        private String devSn;
        private List<String> collectionList;

        public Constants.DBType getDbType() {
            return dbType;
        }

        public void setDbType(Constants.DBType dbType) {
            this.dbType = dbType;
        }

        public String getDevSn() {
            return devSn;
        }

        public void setDevSn(String devSn) {
            this.devSn = devSn;
        }

        public List<String> getCollectionList() {
            return collectionList;
        }

        public void setCollectionList(List<String> collectionList) {
            this.collectionList = collectionList;
        }

        public void lock() {
            databaseLock.lockCollection(dbType, devSn, collectionList);
        }

        public void unlock() {
            databaseLock.unlockCollection(dbType, devSn, collectionList);
        }
    }

    public static void lockCollection(Constants.DBType type, String sn, List<String> list) {
        databaseLock.lockCollection(type, sn, list);
    }

    public static void unlockCollection(Constants.DBType type, String sn, List<String> list) {
        databaseLock.unlockCollection(type, sn, list);
    }

    public static Locker lock(Constants.DBType type, String sn, List<String> list) {
        databaseLock.lockCollection(type, sn, list);

        Locker locker = new Locker();
        locker.setDbType(type);
        locker.setDevSn(sn);
        locker.setCollectionList(list);

        return locker;
    }

    public static Locker getLocker(Constants.DBType type, String sn, List<String> list) {
        Locker locker = new Locker();
        locker.setDbType(type);
        locker.setDevSn(sn);
        locker.setCollectionList(list);

        return locker;
    }
}
