package org.onosproject.arrange;

import org.onosproject.mongodb.Constants.BusinessType;

import java.util.Date;

public class BusinessException extends Exception {
    public BusinessException(BusinessType businessType, Integer businessId, Date date, String message) {
        super(String.format("%s#%s#%s @ %s", businessType, businessId, date, message));
    }

    public BusinessException(BusinessType businessType, Integer businessId, Date date, String message, Throwable cause) {
        super(String.format("%s#%s#%s @ %s", businessType, businessId, date, message), cause);
    }

    public BusinessException(Throwable cause) {
        super(cause);
    }
}
