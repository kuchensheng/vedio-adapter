package com.sxc.adapter.vedio;

public interface IVedioService {

    /**
     * 拉取所有设备视频流信息
     * @param accessToken accessToken
     * @param targetPath 视频存放地址
     */
    public void pullAllVedio(String accessToken,String targetPath);

    /**
     * 拉取所有设备视频流信息
     * @param appKey appkey
     * @param secret secret
     * @param targetPath 视频存放地址
     */
    public void pullAllVedio(String appKey,String secret,String targetPath);

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
    void pullVedioWithDeviceSerial(String accessToken,String[] deviceSerial,String targetPath);
}
