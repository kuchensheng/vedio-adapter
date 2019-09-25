package com.sxc.adapter.vedio.impl;

import com.sxc.adapter.vedio.IVedioHanlerService;
import com.sxc.adapter.vedio.ReceiveDataModel;
import com.sxc.adapter.vedio.Video2JpgUtil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ClassName:VedioHandlerService
 * Description: TODO
 *
 * @author: kuchensheng
 * @version: Create at:  17:52
 * _
 * Copyright:   Copyright (c)2019
 * Company:     songxiaocai
 * _
 * Modification History:
 * Date              Author      Version     Description
 * ------------------------------------------------------------------
 * 17:52   kuchensheng    1.0
 */
public class VedioHandlerService implements IVedioHanlerService {

    private Video2JpgUtil video2JpgUtil = new Video2JpgUtil();
    private static final String targetPath = System.getProperty("user.home");

    @Override
    public byte[] getFrameData(String deviceSerial,String vedioName, int frameIndex) throws Exception {
        String vedioPath = targetPath.concat(File.separator).concat(deviceSerial).concat(File.separator).concat(vedioName);
        byte[] frameData = video2JpgUtil.getFrameData(FFmpegFrameGrabber.createDefault(vedioPath), frameIndex);
        return frameData;
    }

    @Override
    public Map<String, Map<String, byte[]>> getFrameDatas(List<ReceiveDataModel> receiveDataModels) throws Exception{
        Map<String,Map<String,byte[]>> result = new HashMap<>();
        for (ReceiveDataModel dataModel : receiveDataModels) {
            if(!result.containsKey(dataModel.getVedioName())) {
                Map<String,byte[]> frameindex_dataMap = new HashMap<>();
                result.put(dataModel.getVedioName(),frameindex_dataMap);
            }
            result.get(dataModel.getVedioName())
                    .put(String.valueOf(dataModel.getFrameIndex()),getFrameData(dataModel.getDeviceSerial(),
                            dataModel.getVedioName(),dataModel.getFrameIndex()));
        }
        return result;
    }

    @Override
    public String createVedio(String deviceSerial, String vedioName, List<Map<String, byte[]>> frameIndexList) throws Exception {

        return null;
    }
}
