package com.sxc.adapter.vedio;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

public interface IVedioPullService {
    /**
     * 获取摄像头列表信息
     * @param accessToken
     * @return
     */
    List<VedioDataAddressModel> vedioList(String accessToken) throws Exception;
    /**
     * 拉取所有设备视频流信息
     * @param accessToken accessToken
     * @param targetPath 视频存放地址
     */
    List<IVedioPullService> pullAllVedio(String accessToken, String targetPath) throws Exception;

    /**
     * 拉取所有设备视频流信息
     * @param appKey appkey
     * @param secret secret
     * @param targetPath 视频存放地址
     */
    List<IVedioPullService> pullAllVedio(String appKey, String secret, String targetPath) throws Exception;

    /**
     * 通过appkey和secret查询accessToken信息
     * @param appkey
     * @param secret
     * @return
     */
    String getAccessToken(String appkey,String secret);

    /**
     * 拉取指定设备的视频流信息
     * @param accessToken accessToken
     * @param deviceSerial 设别序列号
     * @param targetPath
     */
    IVedioPullService pullVedioWithDeviceSerial(String accessToken, String[] deviceSerial, String targetPath);
}
