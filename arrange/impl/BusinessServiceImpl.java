package org.onosproject.arrange.impl;

import org.apache.felix.scr.annotations.*;
import org.onosproject.arrange.*;
import org.onosproject.arrange.api.BusinessService;
import org.onosproject.mongodb.Constants.BusinessType;
import org.onosproject.mongodb.Constants.DBType;
import org.onosproject.mongodb.Constants.DevModel;
import org.onosproject.mongodb.MongoDBUtil;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfCallHomeController;
import org.onosproject.netconf.NetconfDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component(immediate = true)
@Service
public class BusinessServiceImpl implements BusinessService {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Thread masterThread;
    private static ExecutorService workerThreadPool = Executors.newFixedThreadPool(10);
    private Map<String, Boolean> workerFinishMap = new HashMap<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetconfCallHomeController chController;

    private class Master implements Runnable {
        @Override
        public void run() {
            try {
                while (!BusinessTracker.getInstance().isBundleActive())
                    Thread.sleep(2000);
                BusinessTracker.getInstance().trace();
            } catch (Exception e) {
                log.error("Arrange master business trace error: {}", e.getMessage());
                return;
            }

            while (true) {
                log.debug("Arrange task master thread begin.");
                try {
                    Thread.sleep(2000);
                    Map<String, Map<String, Long>> databaseMap = MongoDBUtil.getDBMap();
                    log.debug("MongoDB map: {}", databaseMap);
                    for (Map.Entry<String, Map<String, Long>> entry : databaseMap.entrySet()) {
                        //我们只针对Candidate类型的数据库，并且数据库有内容，做业务处理
                        if (entry.getKey().contains(DBType.CANDIDATE.toString()) &&
                                !entry.getValue().isEmpty()) {
                            boolean isEmpty = true;
                            for (Map.Entry<String, Long> collectionEntry : entry.getValue().entrySet()) {
                                if (collectionEntry.getValue() != 0) {
                                    isEmpty = false;
                                }
                            }
                            if (isEmpty) {
                                continue;
                            }

                            log.debug("database name: {}", entry.getKey());

                            //通过数据库的名称获取相应的设备类型和SN标识
                            String[] args = entry.getKey().split("-");
                            String devModel = args[1];
                            String devSn = args[2];

                            //设备不在线，不会分配新的处理线程
                            DeviceId deviceId = chController.getDeviceIdByIpOrSn(devSn);
                            if (deviceId == null) {
                                log.debug("Device {} is not register or offline!", devSn);
                                continue;
                            }

                            NetconfDevice device = chController.getNetconfDevice(deviceId);
                            if (device == null) {
                                log.debug("Device {} is not online!", devSn);
                                continue;
                            }

                            //相应设备的worker线程存在并且没有执行结束，不会分配新的处理线程
                            Boolean doneFlag = workerFinishMap.get(devSn);
                            if (doneFlag != null && !doneFlag) {
                                log.debug("Device {} worker thread is still running!", devSn);
                                continue;
                            }

                            log.debug("Device {} {} offline arrange work start!", devSn, deviceId);

                            workerFinishMap.put(devSn, Boolean.FALSE);
                            workerThreadPool.execute(new Worker(DevModel.fromValue(devModel), devSn, deviceId));
                        }
                    }
                } catch (Exception e) {
                    log.info("Arrange master thread running error {} {}", e.getMessage(), e.getStackTrace());
                }
            }
        }
    }

    @Activate
    protected void activate() {
        log.info("BusinessServiceImpl Started");
        masterThread = new Thread(new Master());
        masterThread.start();
    }

    @Deactivate
    protected void deactivate() {
        log.info("BusinessServiceImpl Stopped");
        masterThread.interrupt();
    }

    public void workerFinishNotify(DevModel model, String devSn) {
        log.debug("set device {} worker thread done.", devSn);
        workerFinishMap.put(devSn, Boolean.TRUE);
    }
}
