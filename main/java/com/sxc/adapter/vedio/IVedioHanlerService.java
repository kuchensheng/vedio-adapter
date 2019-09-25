package com.sxc.adapter.vedio;

import java.util.List;
import java.util.Map;

/**
 * 视频处理相关接口
 */
public interface IVedioHanlerService {

    /**
     * 获取指定视频，指定位置的帧内容
     * @param deviceSerial 设备序列号
     * @param vedioName 原始视频
     * @param frameIndex 帧的位置索引
     * @return
     */
    byte[] getFrameData(String deviceSerial,String vedioName, int frameIndex) throws Exception;

    /**
     * 获取指定视频，指定位置的帧的集合
     * @param receiveDataModels 获取到的视频的集合
     * @return
     */
    Map<String/*vedioName*/,Map<String/*frameIndex*/,byte[]/*数据*/>> getFrameDatas(List<ReceiveDataModel> receiveDataModels) throws Exception;

    /**
     * 每3s合成一个小视频，然后整合成一个大视频
     * @param deviceSerial 设备序列号
     * @param vedioName 视频名称
     * @param frameIndexList 视频帧的位置索引
     * @return
     */
    String createVedio(String deviceSerial,String vedioName, List<Map<String/*frameIndex*/,byte[]/*帧的内容*/>> frameIndexList) throws Exception;

}
