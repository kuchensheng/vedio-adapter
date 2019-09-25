package com.sxc.adapter.vedio;

import java.util.List;

/**
 * 视频拉取相关接口
 */
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
     */
    List<IVedioPullService> pullAllVedio(String accessToken) throws Exception;

    /**
     * 录制所有设备的视频信息，根据规则截取帧，并将帧上传的指定位置
     * @param accessToken token
     * @param frameTargetAddress 截取的帧存放的地址
     * @param type 帧存放地址的类型，kafka，redis或者oss
     * @return
     * @throws Exception
     */
    List<IVedioPullService> pullAllVedioAndCutFrame(String accessToken,String frameTargetAddress,FrameTargetTypeEnum type) throws Exception;

    /**
     * 拉取所有设备视频流信息
     * @param appKey appkey
     * @param secret secret
     */
    List<IVedioPullService> pullAllVedio(String appKey, String secret) throws Exception;

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
