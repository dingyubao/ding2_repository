package org.onosproject.arrange;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.glassfish.jersey.client.ClientProperties;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.arrange.utils.LockUtils;
import org.onosproject.arrange.utils.ModeUtils;
import org.onosproject.mongodb.Adapter;
import org.onosproject.mongodb.Adapter.Operation;
import org.onosproject.mongodb.Constants;
import org.onosproject.mongodb.Constants.BusinessType;
import org.onosproject.mongodb.Constants.DevModel;
import org.onosproject.mongodb.MongoDBUtil;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfCallHomeController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.vcpe.model.SdncAlertParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.*;

import java.util.LinkedList;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;

class BusinessExceptionProcessor {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static ServiceDirectory services = new DefaultServiceDirectory();

    private static final String STB_WEB_URL = System.getenv("STB_WEB_URL");
    private static final int DEFAULT_REST_TIMEOUT_MS = 15000;

    private DevModel devModel;
    private String devSn;
    private DeviceId deviceId;
    private BusinessBasic business;
    private Operation operation;

    BusinessExceptionProcessor(DevModel devModel, String devSn, DeviceId deviceId, BusinessBasic business, Operation operation) {
        this.devModel = devModel;
        this.devSn = devSn;
        this.deviceId = deviceId;
        this.business = business;
        this.operation = operation;
    }

    void process(BusinessException exception) {
        if (!checkIfOnline()) {
            log.debug("Device {} is not online!", devSn);
            return;
        }

        String message = exception.getMessage();
        checkStringNotNullAndEmpty(message);
        String errMsg = message.split("@")[1];
        checkStringNotNullAndEmpty(errMsg);

        if (errMsg.contains("interrupt")) {
            Thread.currentThread().interrupt();
            return;
        } else if (errMsg.contains("timeout")) {
            try {
                if (processTimeout())
                    return;
            } catch (Exception e) {
                message = e.getMessage();
                errMsg = message.split("@")[1];
            }
        } else {
            int retryCnt = 4;
            while ((retryCnt--) > 0) {
                try {
                    Thread.sleep(2000);
                    business.work(devSn, deviceId, operation);
                    return;
                } catch (Exception e) {
                    log.info("Retry count: {}, Exception message: {}", 4 - retryCnt, e.getMessage());
                }
            }
        }

        String[] businessMsg = message.split("@")[0].split("#");
        checkNotNull(businessMsg);
        checkStringNotNullAndEmpty(businessMsg[0]);
        checkStringNotNullAndEmpty(businessMsg[1]);
        checkStringNotNullAndEmpty(businessMsg[2]);

        BusinessType businessType = BusinessType.fromValue(businessMsg[0]);
        Integer businessId = Integer.valueOf(businessMsg[1]);
        String date = businessMsg[2];

        setStateError(businessType, businessId);

        if (ModeUtils.isArrangeModeOffline()) {
            //将所有依赖该业务的其他业务，状态置为PAUSE
            setDependencyState();

            //产生异常告警信息
            String alertDesc = String.format("Business %s-%s date %s operation %s error, message: %s",
                    businessType, businessId, date, operation, errMsg);
            postAlertInfo(businessType, businessId, alertDesc);
        }

    }

    private Boolean checkIfOnline() {
        NetconfCallHomeController chController = services.get(NetconfCallHomeController.class);
        NetconfDevice device = chController.getNetconfDevice(chController.getDeviceIdByIpOrSn(devSn));
        if (device == null)
            return Boolean.FALSE;
        return Boolean.TRUE;
    }

    private Boolean processTimeout() throws BusinessException {
        while (true) {
            try {
                Thread.sleep(2000);
                return business.work(devSn, deviceId, operation);
            } catch (Exception e) {
                log.info(e.getMessage());
                if (!e.getMessage().contains("timeout")) {
                    throw new BusinessException(e);
                }
            }
        }
    }

    private void setStateError(BusinessType businessType, Integer businessId) {
        String database = Constants.candidateDBName(devSn, devModel);

        for(String collection : business.getLockCollectionList()) {
            MongoCollection<Document> mongoCollection = MongoDBUtil.getCollection(database, collection);
            for (Document document : mongoCollection.find()) {
                if (Operation.fromValue(document.getString("opt")) == operation &&
                        BusinessType.fromValue(document.getString("businessType")) == businessType &&
                        document.getInteger("businessId").equals(businessId)) {
                    log.info("Mark collection: {} document: {} state to ERROR", collection, document);
                    Bson filter = Filters.eq("key", document.getString("key"));
                    document.put("state", Adapter.State.ERROR.toString());
                    mongoCollection.replaceOne(filter, document);
                }
            }
        }
    }

    private void setDependencyState() {
        LinkedList<BusinessType> exportChain;
        Boolean isContinue = Boolean.TRUE;
        switch (operation) {
            case INSERT:
            case UPDATE:
                exportChain = business.getExportChainOfInsert();
                break;
            case DELETE:
                exportChain = business.getExportChainOfDelete();
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }

        String database = Constants.candidateDBName(devSn, devModel);

        //因为业务的异常处理器是在加锁环境下执行的，所以在这里需要先释放业务锁
        LockUtils.Locker lockerWorker = LockUtils.getLocker(Constants.DBType.CANDIDATE, devSn, business.getLockCollectionList());
        lockerWorker.unlock();

        for (BusinessType businessType : exportChain) {
            BusinessBasic businessDeal = Dependency.getInstance(devModel).getBusiness(businessType);
            //每个依赖链都由本身开头
            if (businessDeal == business)
                continue;

            LockUtils.Locker locker = LockUtils.lock(Constants.DBType.CANDIDATE, devSn, businessDeal.getLockCollectionList());
            try {
                for (String collection : businessDeal.getLockCollectionList()) {
                    MongoCollection<Document> mongoCollection = MongoDBUtil.getCollection(database, collection);
                    for (Document document : mongoCollection.find()) {
                        if (Operation.fromValue(document.getString("opt")) == operation) {
                            if (Adapter.State.fromValue(document.getString("state")) == Adapter.State.ERROR ||
                                    Adapter.State.fromValue(document.getString("state")) == Adapter.State.PAUSE) {
                                isContinue = Boolean.FALSE;
                                break;
                            }
                            log.info("Mark collection: {} document: {} state to PAUSE", collection, document);
                            Bson filter = Filters.eq("key", document.getString("key"));
                            document.put("state", Adapter.State.PAUSE.toString());
                            mongoCollection.replaceOne(filter, document);
                        }
                    }
                    if (!isContinue)
                        break;
                }
            } finally {
                locker.unlock();
            }
        }

        lockerWorker.lock();
    }

    private void postAlertInfo(BusinessType businessType, Integer businessId, String description) {
        SdncAlertParam alertParam = new SdncAlertParam();

        alertParam.setType(SdncAlertParam.TypeEnum.FLEXSDNC);
        alertParam.setDeviceName("ONOS");
        alertParam.setCode("350000");
        alertParam.setAlertType(4); //环境告警
        alertParam.setStatus(1);    //告警产生
        alertParam.setLevel(1);     //告警级别，严重告警
        alertParam.setStartTime(System.currentTimeMillis()); //告警产生时间

        if (description.hashCode() < 0)
            alertParam.setFlowId((long)-description.hashCode()); //流水号
        else
            alertParam.setFlowId((long)description.hashCode());
        alertParam.setSubSystemName(businessType.toString());
        alertParam.setModuleName(businessId.toString());
        alertParam.setDesc(description);

        log.info("Sending offline arrange alert info {} to SMS {} ", alertParam, STB_WEB_URL + "/notify/sdnc/alert");

        Client client = ClientBuilder.newClient();
        client.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_REST_TIMEOUT_MS);
        client.property(ClientProperties.READ_TIMEOUT, DEFAULT_REST_TIMEOUT_MS);

        ObjectMapper mapper = new ObjectMapper();

        WebTarget wt = client.target(STB_WEB_URL + "/notify/sdnc/alert");
        Invocation.Builder builder = wt.request(JSON_UTF_8.toString());
        try {
            builder.post(Entity.json(mapper.writeValueAsString(alertParam)), String.class);
        } catch (Exception e) {
            log.error("Unable to send offline arrange alert info to sms: {}", e.getMessage());
        }
    }

    private void checkStringNotNullAndEmpty(String reference) {
        if (reference == null || reference.isEmpty())
            throw new NullPointerException();
    }
}
