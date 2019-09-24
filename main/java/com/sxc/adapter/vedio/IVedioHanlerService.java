package com.sxc.adapter.vedio;

import com.alibaba.fastjson.JSONArray;

import java.io.File;
import java.util.Map;

/**
 * 视频处理相关接口
 */
public interface IVedioHanlerService {

    byte[] getFrameData(String vedioName, int frameIndex);

    Map<String/*vedioName*/,Map<String/*frameIndex*/,byte[]/*数据*/>> getFrameDatas(JSONArray array);

}
