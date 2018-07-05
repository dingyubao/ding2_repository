package org.onosproject.arrange.api;

import org.onosproject.arrange.BusinessBasic;
import org.onosproject.arrange.DependencyException;
import org.onosproject.mongodb.Constants.BusinessType;
import org.onosproject.mongodb.Constants.DevModel;

public interface BusinessService {
    void workerFinishNotify(DevModel model, String devSn);
}
