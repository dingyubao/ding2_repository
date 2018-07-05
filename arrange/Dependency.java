package org.onosproject.arrange;

import org.onosproject.mongodb.Constants.BusinessType;
import org.onosproject.mongodb.Constants.DevModel;
import org.onosproject.mongodb.Adapter.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

/*
 * 依赖链的矩阵图结构表示（仅作为示例）：
 * business |   A   |   B   |   C   |   D   |   E   |   F   |
 * ---------|-------|-------|-------|-------|-------|-------|
 *    A     |self   | import|unknown|unknown|export |unknown|
 * ---------|-------|-------|-------|-------|-------|-------|
 *    B     |export |self   |import |unknown|unknown|export |
 * ---------|-------|-------|-------|-------|-------|-------|
 *    C     |unknown|export |self   |unknown|import |unknown|
 * ---------|-------|-------|-------|-------|-------|-------|
 *    D     |unknown|unknown|unknown|self   |import |export |
 * ---------|-------|-------|-------|-------|-------|-------|
 *    E     |import |unknown|export |export |self   |import |
 * ---------|-------|-------|-------|-------|-------|-------|
 *    F     |unknown|import |unknown|import |export |self   |
 */

public class Dependency {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static Dependency instanceEng = new Dependency(DevModel.ENG);
    private static Dependency instanceGw = new Dependency(DevModel.GW);

    enum Relationship {
        SELF("self"),
        IMPORT("import"),
        EXPORT("export"),
        UNKNOWN("unknown");

        private String value;

        Relationship(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static Relationship fromValue(String text) {
            for (Relationship b : Relationship.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    private DevModel devModel;
    private ReentrantLock globalLock;
    private LinkedList<BusinessBasic> businessChain;
    private ArrayList<LinkedList<BusinessBasic>> dependencyChain;
    private ArrayList<LinkedList<LinkedList<Relationship>>> dependencyGraph;

    private Boolean isGraphChanged = Boolean.FALSE;

    private Dependency(DevModel devModel) {
        this.devModel = devModel;
        globalLock = new ReentrantLock();
        businessChain = new LinkedList<>();

        dependencyChain = new ArrayList<>();
        dependencyChain.add(BusinessBasic.INSERT, new LinkedList<>());
        dependencyChain.add(BusinessBasic.DELETE, new LinkedList<>());

        dependencyGraph = new ArrayList<>();
        dependencyGraph.add(BusinessBasic.INSERT, new LinkedList<>());
        dependencyGraph.add(BusinessBasic.DELETE, new LinkedList<>());
    }

    public static Dependency getInstance(DevModel devModel) {
        switch (devModel) {
            case ENG:
                return instanceEng;
            case GW:
                return instanceGw;
            default:
                throw new IllegalArgumentException("argument out of range");
        }
    }

    public void add(BusinessBasic businessBasic) throws DependencyException {
        //屏蔽设备类型错误的业务
        if (businessBasic.getDevModel() != devModel)
            return;

        globalLock.lock();
        try {
            //屏蔽重复添加的业务
            for (BusinessBasic business : businessChain)
                if (business.getBusinessType() == businessBasic.getBusinessType())
                    return;

            businessChain.addLast(businessBasic);
            graphGrow(Operation.INSERT, businessBasic);
            graphGrow(Operation.DELETE, businessBasic);

            this.isGraphChanged = Boolean.TRUE;
        } finally {
            globalLock.unlock();
        }

        log.debug("INSERT: {}", graphDraw(Operation.INSERT));
        log.debug("DELETE: {}", graphDraw(Operation.DELETE));
    }

    public void remove(DevModel devModel, BusinessType businessType) {
        globalLock.lock();
        try {
            Iterator<BusinessBasic> iterator = businessChain.iterator();
            while (iterator.hasNext()) {
                BusinessBasic business = iterator.next();
                if (business.getDevModel() == devModel &&
                        business.getBusinessType() == businessType) {
                    graphGrowReverse(Operation.INSERT, business);
                    graphGrowReverse(Operation.DELETE, business);
                    iterator.remove();

                    this.isGraphChanged = Boolean.TRUE;
                    break;
                }
            }
        } finally {
            globalLock.unlock();
        }
        log.debug("INSERT: {}", graphDraw(Operation.INSERT));
        log.debug("DELETE: {}", graphDraw(Operation.DELETE));
    }

    public BusinessBasic getBusiness(BusinessType businessType) {
        for (BusinessBasic business : businessChain)
            if (business.getBusinessType() == businessType)
                return business;
        return null;
    }

    public LinkedList<BusinessBasic> getDependencyChain(Operation operation) throws DependencyException {
        globalLock.lock();
        try {
            if (isGraphChanged) {
                log.info("INSERT: {}", graphDraw(Operation.INSERT));
                log.info("DELETE: {}", graphDraw(Operation.DELETE));
                for (BusinessBasic business : businessChain) {
                    LinkedList<BusinessType> importInsertChain = new LinkedList<>();
                    LinkedList<BusinessType> importDeleteChain = new LinkedList<>();
                    LinkedList<BusinessType> exportInsertChain = new LinkedList<>();
                    LinkedList<BusinessType> exportDeleteChain = new LinkedList<>();

                    businessChainGrow(Operation.INSERT, Relationship.IMPORT, business, importInsertChain);
                    businessChainGrow(Operation.DELETE, Relationship.IMPORT, business, importDeleteChain);
                    businessChainGrow(Operation.INSERT, Relationship.EXPORT, business, exportInsertChain);
                    businessChainGrow(Operation.DELETE, Relationship.EXPORT, business, exportDeleteChain);

                    business.setImportChainOfInsert(importInsertChain);
                    business.setImportChainOfDelete(importDeleteChain);
                    business.setExportChainOfInsert(exportInsertChain);
                    business.setExportChainOfDelete(exportDeleteChain);

                    log.info(businessChainDraw(business, Operation.INSERT, Relationship.IMPORT));
                    log.info(businessChainDraw(business, Operation.INSERT, Relationship.EXPORT));
                    log.info(businessChainDraw(business, Operation.DELETE, Relationship.IMPORT));
                    log.info(businessChainDraw(business, Operation.DELETE, Relationship.EXPORT));
                }

                dependencyChainBuild(Operation.INSERT);
                dependencyChainBuild(Operation.DELETE);

                log.info(dependencyChainDraw(Operation.INSERT));
                log.info(dependencyChainDraw(Operation.DELETE));

                this.isGraphChanged = Boolean.FALSE;
            }
        } finally {
            globalLock.unlock();
        }

        if (operation == Operation.INSERT)
            return dependencyChain.get(BusinessBasic.INSERT);
        else
            return dependencyChain.get(BusinessBasic.DELETE);
    }

    private void graphGrow(Operation operation, BusinessBasic businessAdd) throws DependencyException {
        LinkedList<LinkedList<Relationship>> graph;
        switch (operation) {
            case INSERT:
                graph = dependencyGraph.get(BusinessBasic.INSERT);
                break;
            case DELETE:
                graph = dependencyGraph.get(BusinessBasic.DELETE);
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }

        //拓展图的边界，赋初始值
        for (LinkedList<Relationship> entry : graph) {
            entry.addLast(Relationship.UNKNOWN);
        }
        graph.addLast(new LinkedList<>());
        for (Integer i = 0; i < businessChain.size(); i++) {
            graph.getLast().addLast(Relationship.UNKNOWN);
        }

        //计算边界与相应业务的关系
        //可以检测出的循环依赖为A<->B
        for (Integer horizontal = 0; horizontal < businessChain.size(); horizontal++) {
            BusinessBasic businessCmp = businessChain.get(horizontal);

            if (businessCmp == businessAdd) {
                graph.get(horizontal).set(businessChain.size() - 1, Relationship.SELF);
                break;
            }

            LinkedList<BusinessType> importChainOfBusinessCmp;
            LinkedList<BusinessType> importChainOfBusinessAdd;

            switch (operation) {
                case INSERT:
                    importChainOfBusinessCmp = businessCmp.getImportChainOfInsert();
                    importChainOfBusinessAdd = businessAdd.getImportChainOfInsert();
                    break;
                case DELETE:
                    importChainOfBusinessCmp = businessCmp.getImportChainOfDelete();
                    importChainOfBusinessAdd = businessAdd.getImportChainOfDelete();
                    break;
                default:
                    throw new IllegalArgumentException("argument out of range");
            }

            for (BusinessType businessType : importChainOfBusinessCmp) {
                if (businessType == businessAdd.getBusinessType()) {
                    graph.get(horizontal).set(businessChain.size() - 1, Relationship.IMPORT);
                    break;
                }
            }

            for (BusinessType businessType : importChainOfBusinessAdd) {
                if (businessType == businessCmp.getBusinessType()) {
                    if (graph.get(horizontal).getLast() == Relationship.IMPORT)
                        throw new DependencyException(businessAdd, businessCmp, "circular dependency");
                    graph.get(horizontal).set(businessChain.size() - 1, Relationship.EXPORT);
                    break;
                }
            }

            if (graph.get(horizontal).getLast() == Relationship.IMPORT)
                graph.getLast().set(horizontal, Relationship.EXPORT);
            else if (graph.get(horizontal).getLast() == Relationship.EXPORT)
                graph.getLast().set(horizontal, Relationship.IMPORT);
        }
    }

    private void graphGrowReverse(Operation operation, BusinessBasic businessDel) {
        LinkedList<LinkedList<Relationship>> graph;
        switch (operation) {
            case INSERT:
                graph = dependencyGraph.get(BusinessBasic.INSERT);
                break;
            case DELETE:
                graph = dependencyGraph.get(BusinessBasic.DELETE);
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }

        Integer indexDel;
        for (indexDel = 0; indexDel < businessChain.size(); indexDel++)
            if (businessChain.get(indexDel) == businessDel)
                break;

        for (Integer i = 0; i < businessChain.size(); i++)
            graph.get(i).remove(indexDel);
        graph.remove(indexDel);
    }

    private void businessChainGrow(Operation operation, Relationship relationshipDeal, BusinessBasic businessDeal,
                                   LinkedList<BusinessType> chainDeal) throws DependencyException {
        LinkedList<LinkedList<Relationship>> graph;
        switch (operation) {
            case INSERT:
                graph = dependencyGraph.get(BusinessBasic.INSERT);
                break;
            case DELETE:
                graph = dependencyGraph.get(BusinessBasic.DELETE);
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }

        //占位，为需要处理的具体业务类型
        if (chainDeal.isEmpty())
            chainDeal.add(0, businessDeal.getBusinessType());

        /*
         * horizontal：矩阵横向
         * vertical  ：矩阵纵向
         */
        for (Integer horizontal = 0; horizontal < businessChain.size(); horizontal++) {
            if (businessChain.get(horizontal).getBusinessType() == businessDeal.getBusinessType()) {
                for (Integer vertical = 0; vertical < businessChain.size(); vertical++) {
                    BusinessBasic businessCmp = businessChain.get(vertical);
                    Relationship relationshipCmp = graph.get(horizontal).get(vertical);

                    if (relationshipCmp == relationshipDeal) {
                        Integer dealIndex = Integer.MAX_VALUE;
                        Integer cmpIndex = Integer.MAX_VALUE;

                        for (Integer i = 0; i < chainDeal.size(); i++) {
                            if (chainDeal.get(i) == businessDeal.getBusinessType())
                                dealIndex = i;
                            if (chainDeal.get(i) == businessCmp.getBusinessType())
                                cmpIndex = i;
                        }

                        //检测隐性的循环依赖，类似于D<-C<-B<-A<-D
                        if (dealIndex > cmpIndex) {
                            String errorMessage = String.format("Insert business %s into dependency chain below: \n",
                                    businessCmp.getBusinessType().toString());
                            for (BusinessType businessType : chainDeal) {
                                errorMessage = errorMessage.concat(businessType.toString());
                                switch (relationshipDeal) {
                                    case IMPORT:
                                        errorMessage = errorMessage.concat("->");
                                        break;
                                    case EXPORT:
                                        errorMessage = errorMessage.concat("<-");
                                        break;
                                    default:
                                        throw new IllegalArgumentException("argument out of range");
                                }
                            }
                            throw new DependencyException(businessDeal, businessCmp, String.format("circular dependency, %s", errorMessage));
                        } else if (cmpIndex == Integer.MAX_VALUE) {
                            log.debug("Business {} direct {}-{}", chainDeal.getFirst(), operation, relationshipDeal);
                            for (BusinessType businessType : chainDeal)
                                log.debug("--{}--", businessType);
                            chainDeal.add(dealIndex + 1, businessCmp.getBusinessType());
                            for (BusinessType businessType : chainDeal)
                                log.debug("++{}++", businessType);
                        }

                        //递归处理
                        this.businessChainGrow(operation, relationshipDeal, businessCmp, chainDeal);
                    }
                }
                break;
            }
        }
    }

    private void dependencyChainBuild(Operation operation) {
        LinkedList<BusinessBasic> chain;
        switch (operation) {
            case INSERT:
                chain = dependencyChain.get(BusinessBasic.INSERT);
                break;
            case DELETE:
                chain = dependencyChain.get(BusinessBasic.DELETE);
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }
        chain.clear();

        LinkedList<BusinessType> typeChain = new LinkedList<>();

        for (BusinessBasic business : businessChain) {
            LinkedList<BusinessType> importChain;
            LinkedList<BusinessType> exportChain;
            LinkedList<BusinessType> chainDeal = new LinkedList<>();

            switch (operation) {
                case INSERT:
                    importChain = business.getImportChainOfInsert();
                    exportChain = business.getExportChainOfInsert();
                    break;
                case DELETE:
                    importChain = business.getImportChainOfDelete();
                    exportChain = business.getExportChainOfDelete();
                    break;
                default:
                    throw new IllegalArgumentException("argument out of range");
            }

            for (BusinessType businessType : importChain)
                chainDeal.addFirst(businessType);
            //剔除自我重复的业务，因为import链和export链都由自身作为开始元素
            chainDeal.removeLast();
            chainDeal.addAll(exportChain);

            //依赖链初始化
            if (typeChain.isEmpty()) {
                typeChain.addAll(chainDeal);
                continue;
            }

            //依赖链合并
            chainMerge(typeChain, chainDeal);
        }

        for (BusinessType type : typeChain) {
            for (BusinessBasic business : businessChain) {
                if (type == business.getBusinessType()) {
                    chain.add(business);
                    break;
                }
            }
        }
    }

    private void chainMerge(LinkedList<BusinessType> parentChain, LinkedList<BusinessType> childChain) {
        Integer positionLeft = Integer.MAX_VALUE;
        Integer positionRight = Integer.MAX_VALUE;
        LinkedList<BusinessType> mergeList = new LinkedList<>();
        Boolean isElementNotFound = Boolean.FALSE;

        String stringParent = "before parent: \n";
        for (BusinessType businessType : parentChain)
            stringParent = stringParent.concat(businessType.toString()).concat("<-");

        String stringChild = "before child: \n";
        for (BusinessType businessType : childChain)
            stringChild = stringChild.concat(businessType.toString()).concat("<-");

        log.debug(stringParent);
        log.debug(stringChild);

        for (BusinessType businessType : childChain) {
            Integer currentIndex = parentChain.indexOf(businessType);
            if (currentIndex == -1) {
                mergeList.addLast(businessType);
                isElementNotFound = Boolean.TRUE;
                continue;
            }

            if (isElementNotFound)
                positionRight = currentIndex;
            else {
                //positionLeft和currentIndex对应的业务处于同级，没有依赖关系
                //但是他们的先后顺序必须和childChain中保持一致
                if (positionLeft != Integer.MAX_VALUE &&
                        positionLeft > currentIndex) {
                    BusinessType tmp = parentChain.remove(positionLeft.intValue());
                    parentChain.add(currentIndex, tmp);
                    positionLeft = currentIndex + 1;
                } else
                    positionLeft = currentIndex;
            }

            if (positionRight != Integer.MAX_VALUE &&
                    !mergeList.isEmpty()) {
                if (positionLeft != Integer.MAX_VALUE)
                    parentChain.addAll(positionLeft + 1, mergeList);
                else
                    parentChain.addAll(0, mergeList);
                isElementNotFound = Boolean.FALSE;
                mergeList.clear();
                positionLeft = positionRight;
                positionRight = Integer.MAX_VALUE;
            }
        }

        if (!mergeList.isEmpty())
            if (positionLeft != Integer.MAX_VALUE)
                parentChain.addAll(positionLeft + 1, mergeList);
            else
                parentChain.addAll(0, mergeList);

        String stringParent1 = "after parent: \n";
        for (BusinessType businessType : parentChain)
            stringParent1 = stringParent1.concat(businessType.toString()).concat("<-");
        log.debug(stringParent1);
    }

    private String graphDraw(Operation operation) {
        LinkedList<LinkedList<Relationship>> graph;
        switch (operation) {
            case INSERT:
                graph = dependencyGraph.get(BusinessBasic.INSERT);
                break;
            case DELETE:
                graph = dependencyGraph.get(BusinessBasic.DELETE);
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }

        String picture = "Graph draw below: \n";

        picture = picture.concat("BUSINESS");
        for (BusinessBasic business : businessChain) {
            String businessString = business.getBusinessType().toString();
            if (businessString.length() > 8)
                businessString = businessString.substring(0, 7);
            picture = picture.concat(String.format("|%-8s", businessString));
        }
        picture = picture.concat("| \n");

        /*
         * horizontal：矩阵横向
         * vertical  ：矩阵纵向
         */
        for (Integer vertical = 0; vertical < businessChain.size(); vertical++) {
            String businessString = businessChain.get(vertical).getBusinessType().toString();
            if (businessString.length() > 8)
                businessString = businessString.substring(0, 7);
            picture = picture.concat(String.format("%-8s", businessString));
            for (Integer horizontal = 0; horizontal < businessChain.size(); horizontal++) {
                picture = picture.concat(String.format("|%-8s", graph.get(horizontal).get(vertical)));
            }
            picture = picture.concat("| \n");
        }

        return picture;
    }

    private String businessChainDraw(BusinessBasic business, Operation operation, Relationship relationship) {
        String chainStr = String.format("Business %s direct %s-%s dependency: \n",
                business.getBusinessType(), operation, relationship);
        LinkedList<BusinessType> dependencyChain;
        switch (relationship) {
            case IMPORT:
                switch (operation) {
                    case INSERT:
                        dependencyChain = business.getImportChainOfInsert();
                        break;
                    case DELETE:
                        dependencyChain = business.getImportChainOfDelete();
                        break;
                    default:
                        throw new IllegalArgumentException("argument out of range");
                }
                break;
            case EXPORT:
                switch (operation) {
                    case INSERT:
                        dependencyChain = business.getExportChainOfInsert();
                        break;
                    case DELETE:
                        dependencyChain = business.getExportChainOfDelete();
                        break;
                    default:
                        throw new IllegalArgumentException("argument out of range");
                }
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }

        Iterator<BusinessType> iterator = dependencyChain.iterator();
        while (iterator.hasNext()) {
            BusinessType businessType = iterator.next();
            chainStr = chainStr.concat(businessType.toString());
            if (iterator.hasNext()) {
                switch (relationship) {
                    case IMPORT:
                        chainStr = chainStr.concat("->");
                        break;
                    case EXPORT:
                        chainStr = chainStr.concat("<-");
                        break;
                    default:
                        throw new IllegalArgumentException("argument out of range");
                }
            }
        }
        return chainStr;
    }

    private String dependencyChainDraw(Operation operation) {
        String chainStr = String.format("Business direct %s dependency chain: \n", operation);
        LinkedList<BusinessBasic> chain;
        switch (operation) {
            case INSERT:
                chain = dependencyChain.get(BusinessBasic.INSERT);
                break;
            case DELETE:
                chain = dependencyChain.get(BusinessBasic.DELETE);
                break;
            default:
                throw new IllegalArgumentException("argument out of range");
        }

        Iterator<BusinessBasic> iterator = chain.iterator();
        while (iterator.hasNext()) {
            BusinessBasic business = iterator.next();
            chainStr = chainStr.concat(business.getBusinessType().toString());
            if (iterator.hasNext())
                chainStr = chainStr.concat("<-");
        }

        return chainStr;
    }
}
