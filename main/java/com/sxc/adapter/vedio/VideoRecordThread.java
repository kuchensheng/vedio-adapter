package com.sxc.adapter.vedio;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    public VideoRecordThread(FFmpegFrameRecorder recorder, FFmpegFrameGrabber grabber, AtomicInteger index, String finalVedioPath,VedioPulper pulper) {
        this.recorder = recorder;
        this.grabber = grabber;
        this.index = index;
        this.finalVedioPath = finalVedioPath;
        this.pulper = pulper;
    }

    @Override
    public void run() {
        double frameRate = grabber.getFrameRate();
        long sleepTime = (long) (1/frameRate * 1000);
        long startTime_null = System.currentTimeMillis();
        boolean restart = false;
        while (true) {
            try {
                Frame frame = grabber.grabFrame();
                if(null == frame || null == frame.image) {
                    logger.info("获取到的frame为空，或者frame.image为空");
                    long endTime = System.currentTimeMillis();
                    if(endTime - startTime_null > 1000) {
                        restart = true;
                        break;
                    }
                }else {
                    startTime_null = System.currentTimeMillis();
                    recorder.record(frame,0);
                    if(index.get() % (int) (frameRate/2) == 0) {
                        this.pulper.send2Kafka(frame,finalVedioPath,index.get());
                    }
                }
//                TimeUnit.MILLISECONDS.sleep(sleepTime);
                index.incrementAndGet();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(restart) {
            logger.info("长时间获取不到frame，重新拉取视频");
            pulper.getTask().cancel();
            try {
                pulper.start(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
