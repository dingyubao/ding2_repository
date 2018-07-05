package org.onosproject.arrange.api;

import org.onosproject.mongodb.Constants.DBType;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface DatabaseLock {
    void lockGlobal();
    void lockDatabase(DBType type, String sn);
    void lockCollection(DBType type, String sn, List<String> collections);

    boolean tryLockGlobal();
    boolean tryLockDatabase(DBType type, String sn);
    boolean tryLockCollection(DBType type, String sn, List<String> collections);

    boolean tryLockGlobal(long timeout, TimeUnit unit) throws InterruptedException;
    boolean tryLockDatabase(DBType type, String sn, long timeout, TimeUnit unit) throws InterruptedException;
    boolean tryLockCollection(DBType type, String sn, List<String> collections, long timeout, TimeUnit unit) throws InterruptedException;

    void unlockGlobal();
    void unlockDatabase(DBType type, String sn);
    void unlockCollection(DBType type, String sn, List<String> collections);

    boolean isGlobalLocked();
    boolean isDatabaseLocked(DBType type, String sn);
    boolean isCollectionLocked(DBType type, String sn, List<String> collections);
}
