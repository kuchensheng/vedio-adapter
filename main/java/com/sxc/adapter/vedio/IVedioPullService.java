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
     * 录制所有设备视频流信息
     * @param accessToken accessToken
     * @param targetPath 视频存放地址
     */
    List<IVedioPullService> pullAllVedio(String accessToken, String targetPath) throws Exception;

    /**
     * 录制所有设备的视频信息，根据规则截取帧，并将帧上传的指定位置
     * @param accessToken token
     * @param targetPath 录制的视频存放地址
     * @param frameTargetAddress 截取的帧存放的地址
     * @param type 帧存放地址的类型，kafka，redis或者oss
     * @return
     * @throws Exception
     */
    List<IVedioPullService> pullAllVedioAndCutFrame(String accessToken,String targetPath,String frameTargetAddress,FrameTargetTypeEnum type) throws Exception;

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
    String getAccessToken(String appkey,String secret) throws Exception;

    /**
     * 拉取指定设备的视频流信息
     * @param accessToken accessToken
     * @param deviceSerials 设别序列号
     * @param targetPath
     */
    List<IVedioPullService> pullVedioWithDeviceSerial(String accessToken, String[] deviceSerials, String targetPath) throws Exception;

    enum FrameTargetTypeEnum {
        KAFKA,REDIS,OSS
    }
}
