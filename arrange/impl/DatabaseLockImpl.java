package org.onosproject.arrange.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.mongodb.Constants;
import org.onosproject.mongodb.Constants.DBType;
import org.onosproject.mongodb.Constants.DevModel;
import org.onosproject.mongodb.MongoDBUtil;
import org.onosproject.arrange.api.DatabaseLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component(immediate = true)
@Service
public class DatabaseLockImpl implements DatabaseLock {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private class DeviceLock {
        public ReentrantLock candidateLock = new ReentrantLock();
        public ReentrantLock runningLock = new ReentrantLock();
        public Map<String, ReentrantLock> collections = new HashMap<>();
    }

    private Map<String, DeviceLock> deviceMap = new HashMap<>();
    private ReentrantLock globalLock = new ReentrantLock();

    private String getDBName(DBType type, String sn) {
        String name = null;
        DevModel model = null;

        Map<String, Map<String, Long>> databaseMap =  MongoDBUtil.getDBMap();
        for (Map.Entry<String, Map<String, Long>> entry : databaseMap.entrySet()) {
            if (entry.getKey().contains(sn)) {
                if (entry.getKey().contains(DevModel.ENG.toString())) {
                    model = DevModel.ENG;
                } else {
                    model = DevModel.GW;
                }
            }
        }

        if (type == DBType.CANDIDATE) {
            name = Constants.candidateDBName(sn, model);
        } else if (type == DBType.RUNNING) {
            name = Constants.runningDBName(sn, model);
        }
        return name;
    }

    public void lockGlobal() {
        globalLock.lock();
    }

    public void lockDatabase(DBType type, String sn) {
        DeviceLock deviceLock = null;
        String dbName = getDBName(type, sn);
        if (dbName == null) {
            return;
        }

        synchronized (dbName.intern()) {
            if (!deviceMap.containsKey(dbName)) {
                deviceLock = new DeviceLock();
                deviceMap.put(dbName, deviceLock);
            } else {
                deviceLock = deviceMap.get(dbName);
            }
        }
        
        if (type == DBType.CANDIDATE) {
            deviceLock.candidateLock.lock();
        }
        else if (type == DBType.RUNNING) {
            deviceLock.runningLock.lock();
        }
    }

    //细粒度，数据表锁
    public void lockCollection(DBType type, String sn, List<String> collections) {
        DeviceLock deviceLock = null;
        String dbName = getDBName(type, sn);
        if (dbName == null) {
            return;
        }

        //如果不存在相应数据库和数据表的锁，则创建。此过程需要同步
        synchronized (dbName.intern()) {
            if (!deviceMap.containsKey(dbName)) {
                log.debug("Database {} create device lock.", dbName);
                deviceLock = new DeviceLock();
                deviceMap.put(dbName, deviceLock);
            } else {
                deviceLock = deviceMap.get(dbName);
            }

            for (String collection : collections) {
                if (!deviceLock.collections.containsKey(collection)) {
                    log.debug("Database {} collection {} create lock.", dbName, collection);
                    deviceLock.collections.put(collection, new ReentrantLock());
                }
            }
        }

        // 对于一项具体的业务而言，不同线程lock表的顺序是至关重要的
        // 不同的lock顺序在没有同步保护的情况下会导致死锁的发生
        synchronized (dbName.intern()) {
            for (String collection : collections) {
                deviceLock.collections.get(collection).lock();
            }
        }
    }

    public boolean tryLockGlobal() {
        return globalLock.tryLock();
    }

    public boolean tryLockDatabase(DBType type, String sn) {
        DeviceLock deviceLock = null;
        String dbName = getDBName(type, sn);
        if (dbName == null) {
            return false;
        }

        synchronized (dbName.intern()) {
            if (!deviceMap.containsKey(dbName)) {
                deviceLock = new DeviceLock();
                deviceMap.put(dbName, deviceLock);
            } else {
                deviceLock = deviceMap.get(dbName);
            }
        }

        if (type == DBType.CANDIDATE) {
            return deviceLock.candidateLock.tryLock();
        }
        else if (type == DBType.RUNNING) {
            return deviceLock.runningLock.tryLock();
        }

        return false;
    }

    //细粒度，数据表锁
    public boolean tryLockCollection(DBType type, String sn, List<String> collections) {
        DeviceLock deviceLock = null;
        String dbName = getDBName(type, sn);
        if (dbName == null) {
            return false;
        }

        //如果不存在相应数据库和数据表的锁，则创建。此过程需要同步
        synchronized (dbName.intern()) {
            if (!deviceMap.containsKey(dbName)) {
                deviceLock = new DeviceLock();
                deviceMap.put(dbName, deviceLock);
            } else {
                deviceLock = deviceMap.get(dbName);
            }

            for (String collection : collections) {
                if (!deviceLock.collections.containsKey(collection)) {
                    deviceLock.collections.put(collection, new ReentrantLock());
                }
            }
        }

        // 对于一项具体的业务而言，不同线程lock表的顺序是至关重要的
        // 不同的lock顺序在没有同步保护的情况下会导致死锁的发生
        synchronized (dbName.intern()) {
            for (String collection : collections) {
                List<String> lockedList = new ArrayList<>();
                if (deviceLock.collections.get(collection).tryLock()) {
                    lockedList.add(collection);
                }
                else {
                    for (String locked : lockedList) {
                        deviceLock.collections.get(locked).unlock();
                    }
                    return false;
                }
            }
        }

        return true;
    }

    public boolean tryLockGlobal(long timeout, TimeUnit unit) throws InterruptedException {
        return globalLock.tryLock(timeout,unit);
    }

    public boolean tryLockDatabase(DBType type, String sn, long timeout, TimeUnit unit) throws InterruptedException {
        DeviceLock deviceLock = null;
        String dbName = getDBName(type, sn);
        if (dbName == null) {
            return false;
        }

        if (Thread.interrupted())
            throw new InterruptedException();

        synchronized (dbName.intern()) {
            if (!deviceMap.containsKey(dbName)) {
                deviceLock = new DeviceLock();
                deviceMap.put(dbName, deviceLock);
            } else {
                deviceLock = deviceMap.get(dbName);
            }
        }

        if (type == DBType.CANDIDATE) {
            return deviceLock.candidateLock.tryLock(timeout, unit);
        }
        else if (type == DBType.RUNNING) {
            return deviceLock.runningLock.tryLock(timeout, unit);
        }

        return false;
    }

    //细粒度，数据表锁
    public boolean tryLockCollection(DBType type, String sn, List<String> collections, long timeout, TimeUnit unit) throws InterruptedException {
        DeviceLock deviceLock = null;
        String dbName = getDBName(type, sn);
        if (dbName == null) {
            return false;
        }

        if (Thread.interrupted())
            throw new InterruptedException();

        //如果不存在相应数据库和数据表的锁，则创建。此过程需要同步
        synchronized (dbName.intern()) {
            if (!deviceMap.containsKey(dbName)) {
                deviceLock = new DeviceLock();
                deviceMap.put(dbName, deviceLock);
            } else {
                deviceLock = deviceMap.get(dbName);
            }

            for (String collection : collections) {
                if (!deviceLock.collections.containsKey(collection)) {
                    deviceLock.collections.put(collection, new ReentrantLock());
                }
            }
        }

        // 对于一项具体的业务而言，不同线程lock表的顺序是至关重要的
        // 不同的lock顺序在没有同步保护的情况下会导致死锁的发生
        synchronized (dbName.intern()) {
            for (String collection : collections) {
                List<String> lockedList = new ArrayList<>();
                if (deviceLock.collections.get(collection).tryLock(timeout, unit)) {
                    lockedList.add(collection);
                }
                else {
                    for (String locked : lockedList) {
                        deviceLock.collections.get(locked).unlock();
                    }
                    return false;
                }
            }
        }

        return true;
    }

    public void unlockGlobal() {
        globalLock.unlock();
    }

    public void unlockDatabase(DBType type, String sn) {
        DeviceLock deviceLock = null;
        String dbName = getDBName(type, sn);
        if (dbName == null) {
            return;
        }

//        synchronized (dbName.intern()) {
//            if (!deviceMap.containsKey(dbName)) {
//                deviceLock = new DeviceLock();
//                deviceMap.put(dbName, deviceLock);
//            } else {
//                deviceLock = deviceMap.get(dbName);
//            }
//        }

        if (type == DBType.CANDIDATE) {
            deviceLock.candidateLock.unlock();
        }
        else if (type == DBType.RUNNING) {
            deviceLock.runningLock.unlock();
        }
    }

    //细粒度，数据表锁
    public void unlockCollection(DBType type, String sn, List<String> collections) {
        String dbName = getDBName(type, sn);
        if (dbName == null)
            return;

        DeviceLock deviceLock = deviceMap.get(dbName);
        if (deviceLock == null)
            return;

        for (String collection : collections) {
            ReentrantLock locker = deviceLock.collections.get(collection);
            if (locker == null)
                continue;
            locker.unlock();
        }
    }

    public boolean isGlobalLocked() {
        if (tryLockGlobal()) {
            unlockGlobal();
            return false;
        }
        return true;
    }

    public boolean isDatabaseLocked(DBType type, String sn) {
        if (tryLockDatabase(type, sn)) {
            unlockDatabase(type, sn);
            return false;
        }
        return true;
    }

    public boolean isCollectionLocked(DBType type, String sn, List<String> collections) {
        if (tryLockCollection(type, sn, collections)) {
            unlockCollection(type, sn, collections);
            return false;
        }
        return true;
    }
}
