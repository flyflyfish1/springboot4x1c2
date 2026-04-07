package com.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@TableName("user_behavior")
public class UserBehaviorEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private Long userid;
    private Long jingdianid;
    private String behaviorType;
    private Integer behaviorWeight;
    private String diming;
    private String jingdianleixing;

    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat
    private Date addtime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public Long getJingdianid() {
        return jingdianid;
    }

    public void setJingdianid(Long jingdianid) {
        this.jingdianid = jingdianid;
    }

    public String getBehaviorType() {
        return behaviorType;
    }

    public void setBehaviorType(String behaviorType) {
        this.behaviorType = behaviorType;
    }

    public Integer getBehaviorWeight() {
        return behaviorWeight;
    }

    public void setBehaviorWeight(Integer behaviorWeight) {
        this.behaviorWeight = behaviorWeight;
    }

    public String getDiming() {
        return diming;
    }

    public void setDiming(String diming) {
        this.diming = diming;
    }

    public String getJingdianleixing() {
        return jingdianleixing;
    }

    public void setJingdianleixing(String jingdianleixing) {
        this.jingdianleixing = jingdianleixing;
    }

    public Date getAddtime() {
        return addtime;
    }

    public void setAddtime(Date addtime) {
        this.addtime = addtime;
    }
}
