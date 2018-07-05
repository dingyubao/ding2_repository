package org.onosproject.arrange;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.onosproject.arrange.utils.LockUtils;
import org.onosproject.mongodb.Constants;
import org.onosproject.mongodb.Constants.BusinessType;
import org.onosproject.mongodb.Constants.DBType;
import org.onosproject.mongodb.Adapter.Operation;
import org.onosproject.mongodb.Adapter.State;
import org.onosproject.mongodb.MongoDBUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class ModeOnlineTask extends FutureTask<Boolean> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private List<ModeOnlineInput> inputList;

    public ModeOnlineTask(List<ModeOnlineInput> inputList) {
        super(new ModeOnlineCallable(inputList));
        this.inputList = inputList;
    }

    public void processException() {
        for (ModeOnlineInput input : inputList) {
            LockUtils.Locker locker = LockUtils.lock(DBType.CANDIDATE, input.getDevSn(), new ArrayList<>(input.getCollectionList()));
            try {
                String database = Constants.candidateDBName(input.getDevSn(), input.getDevModel());
                for (String collection : input.getCollectionList()) {
                    MongoCollection<Document> mongoCollection = MongoDBUtil.getCollection(database, collection);

                    for (Document document : mongoCollection.find()) {
                        if (Operation.fromValue(document.getString("opt")) == input.getOperation() &&
                                BusinessType.fromValue(document.getString("businessType")) == input.getBusinessType() &&
                                document.getInteger("businessId").equals(input.getBusinessId())) {
                            log.info("Online task {}:{}:{} delete {} due to error",
                                    input.getBusinessType(), input.getBusinessId(), input.getOperation(), document);
                            Bson filter = Filters.eq("key", document.getString("key"));
                            mongoCollection.deleteOne(filter);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                locker.unlock();
            }
        }
    }
}

class ModeOnlineCallable implements Callable<Boolean> {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private List<ModeOnlineInput> inputList;

    ModeOnlineCallable(List<ModeOnlineInput> inputList) {
        this.inputList = inputList;
    }

    @Override
    public Boolean call() throws Exception {
        Boolean result = Boolean.TRUE;
        LockUtils.Locker locker = null;
        try {
            while (true) {
                log.info("Online task begin with: {}", inputList);
                boolean isContinue = false;
                for (ModeOnlineInput input : inputList) {
                    log.debug("Online task process input: {}", input);
                    locker = LockUtils.getLocker(DBType.CANDIDATE, input.getDevSn(), new ArrayList<>(input.getCollectionList()));
                    String database = Constants.candidateDBName(input.getDevSn(), input.getDevModel());

                    locker.lock();

                    for (String collection : input.getCollectionList()) {
                        log.debug("Online task process collection: {}", collection);
                        MongoCollection<Document> mongoCollection = MongoDBUtil.getCollection(database, collection);

                        log.info("DB: {} collection: {} has {} document(s)", database, collection, mongoCollection.count());
                        for (Document document : mongoCollection.find()) {
                            log.debug("Online task process document: {}", document);

                            if (Operation.fromValue(document.getString("opt")) == input.getOperation() &&
                                    BusinessType.fromValue(document.getString("businessType")) == input.getBusinessType() &&
                                    document.getInteger("businessId").equals(input.getBusinessId())) {
                                if (State.fromValue(document.getString("state")) == State.ERROR) {
                                    log.info("Online task {}:{} operation {} state error", input.getBusinessType(), input.getBusinessId(), input.getOperation());
                                    result = Boolean.FALSE;
                                    break;
                                } else
                                    isContinue = true;
                            }
                        }
                    }

                    locker.unlock();
                    locker = null;
                }

                if (!isContinue)
                    break;

                Thread.sleep(2000);
            }
        } catch (Exception e) {
            log.error("Online task working exception: {} {}", e.getMessage(), e.getStackTrace());
        } finally {
            if (locker != null)
                locker.unlock();
        }
        return result;
    }
}
