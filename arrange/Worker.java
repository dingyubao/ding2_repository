package org.onosproject.arrange;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.arrange.api.BusinessService;
import org.onosproject.arrange.utils.LockUtils;
import org.onosproject.mongodb.*;
import org.onosproject.mongodb.Constants.DBType;
import org.onosproject.mongodb.Constants.DevModel;
import org.onosproject.mongodb.Constants.BusinessType;
import org.onosproject.mongodb.Adapter.Operation;
import org.onosproject.mongodb.Adapter.State;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class Worker implements Runnable {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private DevModel devModel;
    private String devSn;
    private DeviceId deviceId;

    private static ServiceDirectory services = new DefaultServiceDirectory();

    public Worker(DevModel devModel, String sn, DeviceId deviceId) {
        this.devModel = devModel;
        this.devSn = sn;
        this.deviceId = deviceId;
    }

    @Override
    public void run() {
        log.debug("Arrange task worker {}:{} thread begin.", devModel, devSn);

        BusinessService master = null;
        while (master == null) {
            try {
                master = services.get(BusinessService.class);
            } catch (ServiceNotFoundException e) {
                log.error("Device {}:{} worker thread get service error: {}", devModel, devSn, e.getStackTrace());
            }
        }

        try {
            LinkedList<BusinessBasic> dealDelBusinessQ = Dependency.getInstance(devModel).getDependencyChain(Operation.DELETE);
            for (BusinessBasic business : dealDelBusinessQ) {
                businessWork(business, Operation.DELETE);
                if (Thread.interrupted()) return;
            }

            MongoDBUtil.flushDBMap(Constants.candidateDBName(devSn, devModel));

            LinkedList<BusinessBasic> dealAddBusinessQ = Dependency.getInstance(devModel).getDependencyChain(Operation.INSERT);
            for (BusinessBasic business : dealAddBusinessQ) {
                businessWork(business, Operation.INSERT);
                if (Thread.interrupted()) return;
                businessWork(business, Operation.UPDATE);
                if (Thread.interrupted()) return;
            }
        } catch (Exception e) {
            log.error("Device {}:{} work error: {} {}", devModel, devSn, e.getMessage(), e.getStackTrace());
        } finally {
            MongoDBUtil.flushDBMap(Constants.candidateDBName(devSn, devModel));
            master.workerFinishNotify(Constants.DevModel.ENG, devSn);
        }
        log.debug("Arrange task worker {}:{} thread end.", devModel, devSn);
    }

    private void businessWork(BusinessBasic business, Operation operation) {
        log.debug("Process {} business {} now!", operation, business.getBusinessType());

        //业务是否允许执行
        if (!checkPermission(business, operation))
            return;

        LockUtils.Locker lockerCandidate = LockUtils.lock(DBType.CANDIDATE, devSn, business.getLockCollectionList());
        LockUtils.Locker lockerRunning = LockUtils.lock(DBType.RUNNING, devSn, business.getLockCollectionList());

        log.debug("Worker {}:{} process {} business:{} begin.", devModel, devSn, operation, business.getBusinessType());

        boolean result = false;
        try {
            result = business.work(devSn, deviceId, operation);
        } catch (BusinessException e) {
            log.error("Device {} work {} business: {} config error: {} {}",
                    devSn, operation, business.getBusinessType(), e.getMessage(), e.getStackTrace());
            new BusinessExceptionProcessor(devModel, devSn, deviceId, business, operation).process(e);
        } catch (DBException e) {
            log.error("Device {} work {} business: {} DB error: {} {}",
                    devSn, operation, business.getBusinessType(), e.getMessage(), e.getStackTrace());
        } catch (Exception e) {
            log.error("Device {} work {} business: {} coding error: {} {}",
                    devSn, operation, business.getBusinessType(), e.getMessage(), e.getStackTrace());
        } finally {
            lockerCandidate.unlock();
            lockerRunning.unlock();
        }
        log.debug("Worker {}:{} process {} business:{} success? {}", devModel, devSn, operation, business.getBusinessType(), result);
    }

    private Boolean checkPermission(BusinessBasic business, Operation operation) {
        String database = Constants.candidateDBName(devSn, devModel);

        LockUtils.Locker locker = LockUtils.lock(DBType.CANDIDATE, devSn, business.getLockCollectionList());

        //判断业务的数据表是否全部为空，是则直接返回
        Boolean isCollectionEmpty = Boolean.TRUE;
        for (String collection : business.getLockCollectionList()) {
            MongoCollection<Document> mongoCollection = MongoDBUtil.getCollection(database, collection);
            for (Document document : mongoCollection.find()) {
                if (Operation.fromValue(document.getString("opt")) == operation) {
                    isCollectionEmpty = Boolean.FALSE;
                    break;
                }
            }
        }
        if (isCollectionEmpty) {
            locker.unlock();
            return Boolean.FALSE;
        }

        //判断业务是否执行错误，是则直接返回
        for (String collection : business.getLockCollectionList()) {
            MongoCollection<Document> mongoCollection = MongoDBUtil.getCollection(database, collection);
            for (Document document : mongoCollection.find()) {
                if (Operation.fromValue(document.getString("opt")) == operation &&
                        State.fromValue(document.getString("state")) == State.ERROR) {
                    locker.unlock();
                    return Boolean.FALSE;
                }
            }
        }

        //判断业务依赖的业务是否执行错误，是则直接返回
        for (String collection : business.getLockCollectionList()) {
            MongoCollection<Document> mongoCollection = MongoDBUtil.getCollection(database, collection);
            for (Document document : mongoCollection.find()) {
                if (Operation.fromValue(document.getString("opt")) == operation &&
                        State.fromValue(document.getString("state")) == State.PAUSE) {
                    locker.unlock();
                    return getDependencyState(database, business, operation);
                }
            }
        }

        locker.unlock();
        return Boolean.TRUE;
    }

    private Boolean getDependencyState(String database, BusinessBasic business, Operation operation) {
        LinkedList<BusinessType> importChain;
        Boolean result = Boolean.TRUE;
        switch (operation) {
            case INSERT:
            case UPDATE:
                importChain = business.getImportChainOfInsert();
                break;
            case DELETE:
                importChain = business.getImportChainOfDelete();
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }

        for (BusinessType businessType : importChain) {
            BusinessBasic businessDeal = Dependency.getInstance(devModel).getBusiness(businessType);
            //每个依赖链都由本身开头
            if (businessDeal == business)
                continue;

            LockUtils.Locker locker = LockUtils.lock(DBType.CANDIDATE, devSn, businessDeal.getLockCollectionList());
            try {
                for (String collection : businessDeal.getLockCollectionList()) {
                    MongoCollection<Document> mongoCollection = MongoDBUtil.getCollection(database, collection);
                    for (Document document : mongoCollection.find()) {
                        if (Operation.fromValue(document.getString("opt")) == operation &&
                                (State.fromValue(document.getString("state")) == State.ERROR ||
                                        State.fromValue(document.getString("state")) == State.PAUSE)) {
                            result = Boolean.FALSE;
                            break;
                        }
                    }
                    if(!result)
                        break;
                }
            } finally {
                locker.unlock();
            }
        }
        return result;
    }
}
