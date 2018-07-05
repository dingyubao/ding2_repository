package org.onosproject.arrange;

import org.onosproject.mongodb.Adapter;
import org.onosproject.mongodb.DBException;
import org.onosproject.net.DeviceId;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class BusinessTracker {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static BusinessTracker instance = new BusinessTracker();

    private static final String TRACE_LOCATION = "mvn:org.onosproject/onos-app-vcpena/1.11.0-SNAPSHOT";
    private static final String TRACE_CLASSPATH = "/WEB-INF/classes/";
    private static final String TRACE_PACKAGE = "org/onosproject/vcpena/arrange";

    private static Bundle traceBundle;

    private BusinessTracker() {
        BusinessTracker.traceBundle = Activator.getBundle(TRACE_LOCATION);
    }

    public static BusinessTracker getInstance() {
        return instance;
    }

    public void trace() {
        List<Class<?>> businesses = loadBusinesses(getBusinesses());
        for (Class<?> business : businesses) {
            log.debug("Business {} load success, super: {}", business.getName(), business.getSuperclass().getName());

            if (!checkPermission(business))
                continue;

            log.debug("Business {} check success!", business.getName());

            BusinessBasic basic = null;
            try {
                basic = (BusinessBasic) business.newInstance();
            } catch (Exception e) {
                log.error("NewInstance Exception: {} {}", e.getMessage(), e.getStackTrace());
            }
            if (basic != null) {
                log.info("Load Business:{} {}", basic.getDevModel(), basic.getBusinessType());
                try {
                    Dependency.getInstance(basic.getDevModel()).add(basic);
                } catch (DependencyException e) {
                    log.error("DependencyException: {}", e.getMessage());
                    return;
                }
            }
        }
    }

    public Boolean isBundleActive() {
        return traceBundle.getState() == Bundle.ACTIVE;
    }

    private List<String> getBusinesses() {
        List<String> businessList = new ArrayList<>();

        try {
            Enumeration<URL> dirs = traceBundle.findEntries(TRACE_CLASSPATH + TRACE_PACKAGE, "*.class", true);
            while (dirs.hasMoreElements()) {
                String file = dirs.nextElement().getFile();
                String business = file.substring(TRACE_CLASSPATH.length(), file.length() - 6).replace("/", ".");
                businessList.add(business);
            }
        } catch (Exception e) {
            log.error("getBusinesses Exception: {}", e.getMessage());
        }

        return businessList;
    }

    private List<Class<?>> loadBusinesses(List<String> businesses) {
        List<Class<?>> businessList = new ArrayList<>();
        for (String business : businesses) {
            try {
                businessList.add(traceBundle.loadClass(business));
            } catch (ClassNotFoundException e) {
                log.error("ClassNotFoundException: {}, {}", e.getMessage(), e.getStackTrace());
            }
        }
        return  businessList;
    }

    private Boolean checkPermission(Class<?> business) {
        //step 1: 检查业务是否有Business注解
        Business annotation = business.getAnnotation(Business.class);
        if (annotation == null)
            return Boolean.FALSE;

        //step 2: 检查业务是否继承自BusinessBasic
        if (business.getSuperclass() != BusinessBasic.class)
            return Boolean.FALSE;

        try {
            //step 3: 检查业务是否有无参构造函数
            Constructor<?> constructor = business.getConstructor();
            if (constructor == null)
                return Boolean.FALSE;
            //step 4: 检查无参构造函数是否声明为废弃
            if (constructor.getDeclaredAnnotation(Deprecated.class) != null)
                return Boolean.FALSE;
        } catch (NoSuchMethodException e) {
            //没有无参构造函数，无法调用Class.newInstance
            return Boolean.FALSE;
        }

        try {
            //step 5: 检查业务是否实现work方法
            Method method = business.getMethod("work", String.class, DeviceId.class, Adapter.Operation.class);
            if (method == null)
                return Boolean.FALSE;
            //step 6: 检查work方法是否抛出DBException和BusinessException异常
            Class<?>[] exceptionTypes = method.getExceptionTypes();
            if (exceptionTypes.length != 2 ||
                    (exceptionTypes[0] != DBException.class &&
                            exceptionTypes[0] != BusinessException.class) ||
                    (exceptionTypes[1] != DBException.class &&
                            exceptionTypes[1] != BusinessException.class))
                return Boolean.FALSE;
        } catch (NoSuchMethodException e) {
            //没有实现业务需要的work方法
            return Boolean.FALSE;
        }

        return Boolean.TRUE;
    }
}
