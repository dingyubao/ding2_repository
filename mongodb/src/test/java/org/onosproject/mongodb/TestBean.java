package org.onosproject.mongodb;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import java.util.Date;

/**
 * Created by root on 11/21/17.
 */
public class TestBean extends Document {
    /**
     * 序列化部分不管
     */
    private static final long serialVersionUID = 3610201746888594945L;

    public TestBean() {
        super();
    }
    /**
     *构造函数传入3个字段值，添加到Map中
     */
    public TestBean(String name, Integer sex, Date date) {
        super();
        this.append("name", name).append("sex", sex).append("date", date);
    }

    /**
     *该方法用于collection的update
     */
    public void setUpdate(TestBean bean){
        this.append("$set", bean);
    }
    //以下都是获取和设置字段值
    public ObjectId getId() {
        return this.getObjectId("_id");
    }
    public void setId(ObjectId id){
        this.append("_id", id);
    }
    public String getName() {
        return this.getString("name");
    }

    public void setName(String name) {
        this.append("name", name);
    }

    public Integer getSex() {
        return this.getInteger("sex");
    }

    public void setSex(Integer sex) {
        this.append("sex", sex);
    }

    public Date getDate() {
        return this.getDate("date");
    }

    public void setDate(Date date) {
        this.append("date", date);
    }

    @Override
    public String toString() {
        return "TestBean [id=" + getId().toString() + ", name=" + getName() + ", sex=" + getSex() + ", date=" + getDate() + "]";
    }

    public <TDocument> BsonDocument toBsonDocument(Class<TDocument> documentClass, CodecRegistry codecRegistry) {
        // TODO Auto-generated method stub
        return new BsonDocumentWrapper<TestBean>(this, codecRegistry.get(TestBean.class));
    }
}