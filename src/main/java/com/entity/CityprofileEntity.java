package com.entity;

import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@TableName("city_profile")
public class CityprofileEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;
    private String chengshi;
    private String biaoqian;
    private Integer redu;
    private Integer yijuxing;
    private Integer ziranjingguan;
    private Integer lishirenwen;
    private Integer haibindujia;
    private Integer meishixiuxian;
    private Integer dushilvyou;
    private String jianjie;

    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat
    private Date addtime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChengshi() {
        return chengshi;
    }

    public void setChengshi(String chengshi) {
        this.chengshi = chengshi;
    }

    public String getBiaoqian() {
        return biaoqian;
    }

    public void setBiaoqian(String biaoqian) {
        this.biaoqian = biaoqian;
    }

    public Integer getRedu() {
        return redu;
    }

    public void setRedu(Integer redu) {
        this.redu = redu;
    }

    public Integer getYijuxing() {
        return yijuxing;
    }

    public void setYijuxing(Integer yijuxing) {
        this.yijuxing = yijuxing;
    }

    public Integer getZiranjingguan() {
        return ziranjingguan;
    }

    public void setZiranjingguan(Integer ziranjingguan) {
        this.ziranjingguan = ziranjingguan;
    }

    public Integer getLishirenwen() {
        return lishirenwen;
    }

    public void setLishirenwen(Integer lishirenwen) {
        this.lishirenwen = lishirenwen;
    }

    public Integer getHaibindujia() {
        return haibindujia;
    }

    public void setHaibindujia(Integer haibindujia) {
        this.haibindujia = haibindujia;
    }

    public Integer getMeishixiuxian() {
        return meishixiuxian;
    }

    public void setMeishixiuxian(Integer meishixiuxian) {
        this.meishixiuxian = meishixiuxian;
    }

    public Integer getDushilvyou() {
        return dushilvyou;
    }

    public void setDushilvyou(Integer dushilvyou) {
        this.dushilvyou = dushilvyou;
    }

    public String getJianjie() {
        return jianjie;
    }

    public void setJianjie(String jianjie) {
        this.jianjie = jianjie;
    }

    public Date getAddtime() {
        return addtime;
    }

    public void setAddtime(Date addtime) {
        this.addtime = addtime;
    }
}
