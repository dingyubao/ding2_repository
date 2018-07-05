package org.onosproject.arrange;

import org.onosproject.mongodb.Constants.BusinessType;
import org.onosproject.mongodb.Constants.DevModel;
import org.onosproject.mongodb.Adapter.Operation;
import org.onosproject.net.DeviceId;

import java.util.List;

public class ModeOnlineInput {
    private String devSn;
    private DevModel devModel;
    private DeviceId devId;
    private Operation operation;

    private BusinessType businessType;
    private Integer businessId;

    private List<String> collectionList;

    public ModeOnlineInput() {}

    public ModeOnlineInput(String devSn, DevModel devModel, DeviceId devId, Operation operation,
                           BusinessType businessType, Integer businessId, List<String> collectionList) {
        this.devSn = devSn;
        this.devModel = devModel;
        this.devId = devId;
        this.operation = operation;
        this.businessType = businessType;
        this.businessId = businessId;
        this.collectionList = collectionList;
    }

    public String getDevSn() {
        return devSn;
    }

    public void setDevSn(String devSn) {
        this.devSn = devSn;
    }

    public DevModel getDevModel() {
        return devModel;
    }

    public void setDevModel(DevModel devModel) {
        this.devModel = devModel;
    }

    public DeviceId getDevId() {
        return devId;
    }

    public void setDevId(DeviceId devId) {
        this.devId = devId;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public BusinessType getBusinessType() {
        return businessType;
    }

    public void setBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }

    public Integer getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Integer businessId) {
        this.businessId = businessId;
    }

    public List<String> getCollectionList() {
        return collectionList;
    }

    public void setCollectionList(List<String> collectionList) {
        this.collectionList = collectionList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ModeOnlineInput {\n");

        sb.append("    devSn: ").append(toIndentedString(devSn)).append("\n");
        sb.append("    devModel: ").append(toIndentedString(devModel)).append("\n");
        sb.append("    deviceId: ").append(toIndentedString(devId)).append("\n");
        sb.append("    operation: ").append(toIndentedString(operation)).append("\n");
        sb.append("    businessType: ").append(toIndentedString(businessType)).append("\n");
        sb.append("    businessId: ").append(toIndentedString(businessId)).append("\n");
        sb.append("    collections: ").append(toIndentedString(collectionList)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
