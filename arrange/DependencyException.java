package org.onosproject.arrange;

public class DependencyException extends Exception {
    public DependencyException(BusinessBasic basicDeal, BusinessBasic basicCmp, String message) {
        super(String.format("Business: %s<->%s, message:%s", basicDeal.getBusinessType(), basicCmp.getBusinessType(), message));
    }
}
