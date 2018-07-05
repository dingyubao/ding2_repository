package org.onosproject.arrange;

import org.onosproject.mongodb.Constants.BusinessType;
import org.onosproject.mongodb.Constants.DevModel;
import org.onosproject.mongodb.Adapter.Operation;
import org.onosproject.mongodb.DBException;
import org.onosproject.net.DeviceId;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class BusinessBasic {
    public static final Integer INSERT = 0;
    public static final Integer DELETE = 1;
    private DevModel model;
    private BusinessType businessType;
    private List<String> lockCollectionList;

    /*
     * 业务依赖链，分INSERT和DELETE两个方向
     */
    private ArrayList<LinkedList<BusinessType>> importChain;
    private ArrayList<LinkedList<BusinessType>> exportChain;

    public BusinessBasic(DevModel model, BusinessType businessType) {
        this.model = model;
        this.businessType = businessType;
        this.importChain = new ArrayList<>();
        this.importChain.add(INSERT, new LinkedList<>());
        this.importChain.add(DELETE, new LinkedList<>());
        this.exportChain = new ArrayList<>();
        this.exportChain.add(INSERT, new LinkedList<>());
        this.exportChain.add(DELETE, new LinkedList<>());
    }

    public final void setDevModel(DevModel model) {
        this.model = model;
    }

    public final DevModel getDevModel() {
        return this.model;
    }

    public final void setBusinessType(BusinessType businessType) {
        this.businessType = businessType;
    }

    public final BusinessType getBusinessType() {
        return this.businessType;
    }

    public final void setLockCollectionList(List<String> lockCollectionList) {
        this.lockCollectionList = lockCollectionList;
    }

    public final List<String> getLockCollectionList() {
        return this.lockCollectionList;
    }

    public final ArrayList<LinkedList<BusinessType>> getImportChain() {
        return this.importChain;
    }

    public final LinkedList<BusinessType> getImportChainOfInsert() {
        return this.importChain.get(INSERT);
    }

    public final LinkedList<BusinessType> getImportChainOfDelete() {
        return this.importChain.get(DELETE);
    }

    public final void setImportChain(ArrayList<LinkedList<BusinessType>> importChain) {
        this.importChain = importChain;
    }

    public final void setImportChainOfInsert(LinkedList<BusinessType> importChain) {
        this.importChain.get(INSERT).clear();
        this.importChain.get(INSERT).addAll(importChain);
    }

    public final void setImportChainOfDelete(LinkedList<BusinessType> importChain) {
        this.importChain.get(DELETE).clear();
        this.importChain.get(DELETE).addAll(importChain);
    }

    public final ArrayList<LinkedList<BusinessType>> getExportChain() {
        return this.exportChain;
    }

    public final LinkedList<BusinessType> getExportChainOfInsert() {
        return this.exportChain.get(INSERT);
    }

    public final LinkedList<BusinessType> getExportChainOfDelete() {
        return this.exportChain.get(DELETE);
    }

    public final void setExportChain(ArrayList<LinkedList<BusinessType>> exportChain) {
        this.exportChain = exportChain;
    }

    public final void setExportChainOfInsert(LinkedList<BusinessType> exportChain) {
        this.exportChain.get(INSERT).clear();
        this.exportChain.get(INSERT).addAll(exportChain);
    }

    public final void setExportChainOfDelete(LinkedList<BusinessType> exportChain) {
        this.exportChain.get(DELETE).clear();
        this.exportChain.get(DELETE).addAll(exportChain);
    }

    /*
     * 业务处理逻辑实现方法
     * 所有的业务处理逻辑大致分为以下3步：
     * 1）从对应设备的candidate数据库对应的数据表中取数据，进行数据处理
     * 2）将处理好的数据下发到设备，并判断下发是否成功
     * 3）如果配置下发成功，将配置存入对应设备的running数据库对应的数据表中
     */
    public abstract boolean work(String devSn, DeviceId deviceId, Operation operation) throws DBException, BusinessException;
}
