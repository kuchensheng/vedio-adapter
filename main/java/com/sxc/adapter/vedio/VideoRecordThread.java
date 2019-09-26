package com.sxc.adapter.vedio;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ClassName:VideoRecordThread
 * Description: TODO
 *
 * @author: kuchensheng
 * @version: Create at:  11:04
 * _
 * Copyright:   Copyright (c)2019
 * Company:     songxiaocai
 * _
 * Modification History:
 * Date              Author      Version     Description
 * ------------------------------------------------------------------
 * 11:04   kuchensheng    1.0
 */
public class VideoRecordThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(VideoRecordThread.class);
    private FFmpegFrameRecorder recorder;
    private FFmpegFrameGrabber grabber;
    private AtomicInteger index;
    private String finalVedioPath;
    private VedioPulper pulper;

    public VideoRecordThread(FFmpegFrameGrabber grabber, AtomicInteger index, String finalVedioPath,VedioPulper pulper) {
        this.grabber = grabber;
        this.index = index;
        this.finalVedioPath = finalVedioPath;
        this.pulper = pulper;
    }

    @Override
    public void run() {
        recorder = initRecorder();
        if(null == recorder) {
            return;
        }
        double frameRate = grabber.getFrameRate();
        long startTime_null = System.currentTimeMillis();
        boolean restart = false;
        while (true) {
            try {
                Frame frame = grabber.grabFrame();
                recorder.record(frame);
                if(null == frame || null == frame.image) {
                    logger.info("获取到的frame为空，或者frame.image为空");
                    long endTime = System.currentTimeMillis();
                    if(endTime - startTime_null > 1000) {
                        restart = true;
                        break;
                    }
                }else {
                    startTime_null = System.currentTimeMillis();
                    if(index.get() % (int) (frameRate/2) == 0) {
                        this.pulper.send2Kafka(frame,finalVedioPath,index.get());
                    }
                }
                index.incrementAndGet();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(restart) {
            logger.info("长时间获取不到frame，重新拉取视频");
            try {
                pulper.start(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private FFmpegFrameRecorder initRecorder(){
        try {
            Integer frameWight = grabber.getImageWidth();
            Integer frameHeigh = grabber.getImageHeight();

            if(frameWight < 780) {
                frameWight = 780;
            }
            String format= grabber.getFormat();
            String vedioName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm"));
            String vedioPath = this.finalVedioPath.concat(vedioName).concat("_").concat(this.pulper.getVedio_index().get()+"").concat(".").concat(format);
            logger.info("摄像头录制的视频地址：{}",vedioPath);
            System.out.println(String.format("摄像头录制的视频地址：%s",vedioPath));
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(vedioPath,frameWight,frameHeigh,1);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat(format);
            recorder.setFrameRate(10d);
            recorder.start();
            return recorder;
        } catch (FrameRecorder.Exception e) {
            logger.error("recorder初始化失败",e);
        }

        return null;
    }

}
