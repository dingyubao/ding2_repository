package org.onosproject.mongodb;

import org.onosproject.mongodb.Constants.BusinessType;

import java.util.Date;
import com.google.gson.annotations.SerializedName;

public class Adapter<T> {

    public enum Operation {
        @SerializedName("Insert")
        INSERT("Insert"),

        @SerializedName("Delete")
        DELETE("Delete"),

        @SerializedName("Update")
        UPDATE("Update");

        private String value;

        Operation(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static Operation fromValue(String text) {
            for (Operation b : Operation.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    public enum State {
        @SerializedName("UNKNOWN")
        UNKNOWN("UNKNOWN"),

        @SerializedName("OK")
        OK("OK"),

        @SerializedName("PAUSE")
        PAUSE("PAUSE"),

        @SerializedName("CONTINUE")
        CONTINUE("CONTINUE"),

        @SerializedName("ERROR")
        ERROR("ERROR");

        private String value;

        State(String value) {
            this.value = value;
        }

        public String toString() {
            return String.valueOf(value);
        }

        public static State fromValue(String text) {
            for (State b : State.values()) {
                if (String.valueOf(b.value).equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @SerializedName("businessType")
    protected BusinessType businessType;

    @SerializedName("businessId")
    protected Integer businessId;

    @SerializedName("opt")
    protected Operation opt;

    @SerializedName("state")
    protected State state;

    @SerializedName("commitId")
    protected Integer commitId;

    @SerializedName("date")
    protected Date date;

    @SerializedName("obj")
    protected T obj;

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

    public Operation getOpt() {
        return opt;
    }

    public void setOpt(Operation opt) {
        this.opt = opt;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Integer getCommitId() {
        return commitId;
    }

    public void setCommitId(Integer commitId) {
        this.commitId = commitId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public T getObj() {
        return obj;
    }

    public void setObj(T obj) {
        this.obj = obj;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
