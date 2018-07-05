package org.onosproject.arrange.utils;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.arrange.ModeOnlineInput;
import org.onosproject.arrange.ModeOnlineTask;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfCallHomeController;
import org.onosproject.netconf.NetconfDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ModeUtils {
    private static final String OFFLINE = "offline";
    private static final String ONLINE = "online";

    private static final Long TIMEOUT = 60L;
    private static final TimeUnit TIMEUNIT = TimeUnit.SECONDS;

    private final static Logger log = LoggerFactory.getLogger(ModeUtils.class);
    private static ServiceDirectory services = new DefaultServiceDirectory();
    private static NetconfCallHomeController chController = services.get(NetconfCallHomeController.class);

    public enum Result {
        TIMEOUT("TIMEOUT"),
        OK("OK"),
        ERROR("ERROR");

        private String value;

        Result(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }
    }

    private static ExecutorService servicePool = Executors.newFixedThreadPool(1);

    public static boolean isArrangeModeOnline() {
        String arrangeMode = System.getenv("ARRANGE_MODE");
        if(null != arrangeMode){
            return arrangeMode.equals(ModeUtils.ONLINE);
        }
        return true;
    }

    public static boolean isArrangeModeOffline() {
        String arrangeMode = System.getenv("ARRANGE_MODE");
        if(null != arrangeMode){
            return arrangeMode.equals(ModeUtils.OFFLINE);
        }
        return false;
    }

    public static boolean isDeviceOnline(String devSn) {
        DeviceId deviceId = chController.getDeviceIdByIpOrSn(devSn);
        if (deviceId == null)
            return false;

        NetconfDevice device = chController.getNetconfDevice(deviceId);
        return device != null;
    }

    public static ModeOnlineTask taskCreate(List<ModeOnlineInput> inputList) {
        return new ModeOnlineTask(inputList);
    }

    public static void taskExec(ModeOnlineTask task) {
        servicePool.submit(task);
    }

    public static ModeOnlineTask taskCreateAndExec(List<ModeOnlineInput> inputList) {
        ModeOnlineTask task = new ModeOnlineTask(inputList);
        servicePool.submit(task);
        return task;
    }

    public static Result taskSyncResult(ModeOnlineTask task) {
        Boolean result;
        log.debug("[taskSyncResult] begin");
        try {
            result = task.get(ModeUtils.TIMEOUT, ModeUtils.TIMEUNIT);
        } catch (TimeoutException e) {
            log.error("Task working timeout: {}", e.getMessage());
            task.cancel(true);
            task.processException();
            return Result.TIMEOUT;
        } catch (Exception e) {
            log.error("Task working exception: {} {}", e.getMessage(), e.getStackTrace());
            task.processException();
            return Result.ERROR;
        }

        log.debug("Task sync result: {}", result);
        if (!result) {
            task.processException();
            return Result.ERROR;
        }
        return Result.OK;
    }
}
