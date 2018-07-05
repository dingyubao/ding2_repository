package org.onosproject.arrange;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static BundleContext context;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        log.info("Arrange Bundle Start");
        Activator.context = bundleContext;
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        log.info("Arrange Bundle Stop");
    }

    static BundleContext getBundleContext() {
        return Activator.context;
    }

    static Bundle getBundle() {
        return Activator.context.getBundle();
    }

    static Bundle getBundle(String location) {
        return Activator.context.getBundle(location);
    }
}
