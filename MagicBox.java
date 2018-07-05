package org.onosproject.mongodb;

import com.google.gson.annotations.SerializedName;

/*
 * 用于存放需要持久化的简单变量
 * TODO:需要考虑数据竞争的情况
 */
public final class MagicBox {
    public static String MAXCOMMITIDKEY = "maxCommitId";

    @SerializedName("maxCommitId")
    private Integer maxCommitId;

    public Integer getMaxCommitId() {
        return maxCommitId;
    }

    public void setMaxCommitId(Integer maxCommitId) {
        this.maxCommitId = maxCommitId;
    }
}
