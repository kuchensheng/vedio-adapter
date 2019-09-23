package com.sxc.adapter.vedio;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;


/**
 * ClassName:VedioPullUtil
 * Description: 视频流拉取器
 *
 * @author: kuchensheng
 * @version: Create at:  21:16
 * _
 * Copyright:   Copyright (c)2019
 * Company:     songxiaocai
 * _
 * Modification History:
 * Date              Author      Version     Description
 * ------------------------------------------------------------------
 * 21:16   kuchensheng    1.0
 */
public class VedioPulper {

    private static Logger logger = Logger.getLogger(VedioPulper.class.getSimpleName());

    private String vedioAddress;

    private String targetFilePath;

    private String kafka_bootstrap_server;

    private KafkaProducer<String,String> producer;

    private static AtomicInteger index = new AtomicInteger(0);

    private static AtomicInteger vedio_index;

    private String finalVideoPath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));

    private static volatile long start = 0l;

    private Task task;

    public synchronized void setFinalVideoPath(String finalVideoPath) {
        this.finalVideoPath = finalVideoPath;
    }

    public String getFinalVideoPath() {
        return this.finalVideoPath;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public VedioPulper(String vedioAddress, String targetFilePath) {
        this.vedioAddress = vedioAddress;
        this.targetFilePath = targetFilePath;
    }

    public VedioPulper(String vedioAddress,String targetFilePath,String kafka_bootstrap_server) {
        this(vedioAddress,targetFilePath);
        this.kafka_bootstrap_server = kafka_bootstrap_server;
        initKafkaProvider();
    }

    private void initKafkaProvider() {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafka_bootstrap_server);//kafka地址，多个地址用逗号分割
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        this.producer = new KafkaProducer<>(p);
    }


    /**
     * 视频拉取
     */
    public void pull() throws Exception {
        vedio_index = new AtomicInteger(0);
        logger.info("拉取视频源："+this.vedioAddress+"\n存储地址："+this.targetFilePath);
        if(!this.targetFilePath.endsWith(File.separator)) {
            setTargetFilePath(targetFilePath.concat(File.separator));
        }

        createFile(targetFilePath);
        createFile(targetFilePath.concat("frames/"));

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(this.vedioAddress);
        grabber.start();

        //两个小时更改一次地址信息
        this.task = new Task(getTargetFilePath());
        new Timer("updateVedioPathTimer").schedule(task,0,7200000);
        logger.info("摄像头录制的视频地址："+getFinalVideoPath());
        final Integer frameWight = grabber.getImageWidth();
        final Integer frameHeigh = grabber.getImageHeight();

        final double frameRate = grabber.getFrameRate();
        final long sleepTime = (long) (1/frameRate * 1000);
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(getFinalVideoPath(),frameWight,frameHeigh,1);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("flv");
        recorder.setFrameRate(frameRate < 20 ? 24 : frameRate);
        recorder.start();

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
                        send2Kafka(frame,getFinalVideoPath());
                    }
                }
                TimeUnit.MILLISECONDS.sleep(sleepTime);
                index.incrementAndGet();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(restart) {
            logger.info("长时间获取不到frame，重新拉取视频");
            this.task.cancel();
            pull();
        }



    }

    class Task extends TimerTask {
        private String vedioPath;

        public Task(String vedioPath) {
            this.vedioPath = vedioPath;
        }

        @Override
        public void run() {
            LocalDateTime now = LocalDateTime.now();
            start = now.getNano();
            String endTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));
            setFinalVideoPath(getVedioPath().concat(endTime).concat("_"+vedio_index.get()).concat(".flv"));
            //当重新生成视频时，帧数要重新计算
            index.set(0);
        }

        public String getVedioPath() {
            return vedioPath;
        }

        public void setVedioPath(String vedioPath) {
            this.vedioPath = vedioPath;
        }

    }

    private static final String kafka_topic = "to_processed_frame";
    private void send2Kafka(Frame frame,String index) throws IOException {
        if(null != producer) {
            String key = index + "_" + frame.timestamp;
            byte[] byte_data = write2Os(frame);

//            String data = new String(byte_data);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key",key);
            jsonObject.put("img_data",byte_data);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(kafka_topic,jsonObject.toJSONString());
            producer.send(producerRecord, (metadata, exception) -> {
                if (exception != null) {
                    exception.printStackTrace();
                    logger.info(exception.getMessage());
                } else {
                    logger.info("图片index=" + key + "发送到Kafka成功");
                }

            });
        }

    }

    private static void createFile(String filePath) throws Exception {
        File file = new File(filePath);
        if(file.isFile()) {
            if(!file.exists()) {
                createFile(filePath.substring(0,filePath.lastIndexOf(File.separator)));
                file.createNewFile();
            }
        } else {
            if(!file.exists()) {
                filePath = !filePath.endsWith(File.separator) ? filePath : filePath.substring(0,filePath.length()-2);
                createFile(filePath.substring(0,filePath.lastIndexOf("/")));
            }
        }
    }

    private byte[] write2Os(Frame frame) throws IOException {
        int owidth = frame.imageWidth ;
        int oheight = frame.imageHeight ;
        Java2DFrameConverter converter =new Java2DFrameConverter();

        int width = 780;
        int height = (int) (((double) width / owidth) * oheight);
        BufferedImage fecthedImage =converter.getBufferedImage(frame);
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        bi.getGraphics().drawImage(fecthedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                0, 0, null);
//                bi=rotateImage(bi, 90);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bi, "jpg", outputStream);
        return outputStream.toByteArray();
    }


    public void start() throws Exception {
        pull();
    }

    public String getTargetFilePath() {
        return targetFilePath;
    }

    public void setTargetFilePath(String targetFilePath) {
        this.targetFilePath = targetFilePath;
    }

    /**
     * 视频监控
     * @param grabber
     * @throws Exception
     */
    private void shwoFrames(FFmpegFrameGrabber grabber) throws Exception{
        CanvasFrame canvasFrame = new CanvasFrame("监控录像");
        canvasFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);;
        canvasFrame.setAlwaysOnTop(true);
        while (true) {
            if(!canvasFrame.isVisible()) {
                break;
            }
            Frame frame = grabber.grabFrame();
            canvasFrame.showImage(frame);
            TimeUnit.MILLISECONDS.sleep(40);
        }
    }

    public String getKafka_bootstrap_server() {
        return kafka_bootstrap_server;
    }

    public void setKafka_bootstrap_server(String kafka_bootstrap_server) {
        this.kafka_bootstrap_server = kafka_bootstrap_server;
    }

    public static void main(String[] args) {
        try {
//            String str = Loader.load(opencv_objdetect.class);
//            logger.info(str);
            String uri = "rtmp://rtmp01open.ys7.com/openlive/60212ce632c341028b6da41da5dc4121";

            VedioPulper pulper = new VedioPulper(uri,"/Users/kuchensheng/Desktop/test","kafka1:9092,kafka2:9092,kafka3:9092");
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(uri);
            grabber.start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        pulper.shwoFrames(grabber);
                        grabber.stop();
                        grabber.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            pulper.start();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }
}
