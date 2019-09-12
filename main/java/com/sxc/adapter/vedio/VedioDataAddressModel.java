package com.sxc.adapter.vedio;

import com.alibaba.fastjson.JSONObject;

import java.io.Serializable;

/**
 * ClassName:VedioDataAddressModel
 * Description: TODO
 *
 * @author: kuchensheng
 * @version: Create at:  14:29
 * _
 * Copyright:   Copyright (c)2019
 * Company:     songxiaocai
 * _
 * Modification History:
 * Date              Author      Version     Description
 * ------------------------------------------------------------------
 * 14:29   kuchensheng    1.0
 */
public class VedioDataAddressModel implements Serializable {
    /**
     * 设备序列号
     */
    private String deviceSerial;
    /**
     * 通道号
     */
    private Integer channelNo;
    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * HLS流畅直播地址
     */
    private String liveAddress;

    /**
     * HLS高清直播地址
     */
    private String hdAddress;

    /**
     * RTMP流畅直播地址
     */
    private String rtmp;

    /**
     * RTMP高清直播地址
     */
    private String rtmpHd;

    /**
     * 开始时间
     */
    private long beginTime;

    /**
     * 过期时间
     */
    private long endTime;

    /**
     * 地址使用状态：0-未使用或直播已关闭，1-使用中，2-已过期，3-直播已暂停
     */
    private int status;

    /**
     * 地址异常状态，0-正常，1-设备不在线，2-设备开始视频加密，3-设备删除，4-失效，5-未绑定，6-账户下流量已超出
     * 7-设备接入限制
     * 0/1/2/6状态返回地址，其他不返回
     */
    private int exception;

    public String getDeviceSerial() {
        return deviceSerial;
    }

    public void setDeviceSerial(String deviceSerial) {
        this.deviceSerial = deviceSerial;
    }

    public Integer getChannelNo() {
        return channelNo;
    }

    public void setChannelNo(Integer channelNo) {
        this.channelNo = channelNo;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getLiveAddress() {
        return liveAddress;
    }

    public void setLiveAddress(String liveAddress) {
        this.liveAddress = liveAddress;
    }

    public String getHdAddress() {
        return hdAddress;
    }

    public void setHdAddress(String hdAddress) {
        this.hdAddress = hdAddress;
    }

    public String getRtmp() {
        return rtmp;
    }

    public void setRtmp(String rtmp) {
        this.rtmp = rtmp;
    }

    public String getRtmpHd() {
        return rtmpHd;
    }

    public void setRtmpHd(String rtmpHd) {
        this.rtmpHd = rtmpHd;
    }

    public long getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getException() {
        return exception;
    }

    public void setException(int exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

}
