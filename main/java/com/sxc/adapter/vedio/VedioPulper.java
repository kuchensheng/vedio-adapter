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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    private static final ScheduledExecutorService executors = Executors.newScheduledThreadPool(1);

    private String vedioAddress;

    private String targetFilePath;

    private String kafka_bootstrap_server;

    private KafkaProducer<String,String> producer;

    private static AtomicInteger index = new AtomicInteger(0);

    private AtomicInteger vedio_index;

    private String finalVideoPath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));

    private static volatile long start = 0l;

    private IVedioPullService.FrameTargetTypeEnum type;

    private String deviceSerial;

    private FFmpegFrameRecorder recorder;

    public VedioPulper(String vedioAddress, String targetPath, String frameTargetAddress, IVedioPullService.FrameTargetTypeEnum type,String deviceSerial) {
        this.vedioAddress = vedioAddress;
        this.targetFilePath = targetPath;
        this.kafka_bootstrap_server = frameTargetAddress;
        this.deviceSerial = deviceSerial;
        this.type = type;
        if (null == type) {
            throw new RuntimeException("type is required");
        }
        if(type == IVedioPullService.FrameTargetTypeEnum.KAFKA) {
            initKafkaProvider();
        }
    }

    public synchronized void setFinalVideoPath(String finalVideoPath) {
        this.finalVideoPath = finalVideoPath;
    }

    public String getFinalVideoPath() {
        return this.finalVideoPath;
    }


    public VedioPulper(String vedioAddress, String targetFilePath,String deviceSerial) {
        this.vedioAddress = vedioAddress;
        this.targetFilePath = targetFilePath;
        this.deviceSerial = deviceSerial;
    }

    public VedioPulper(String vedioAddress,String targetFilePath,String kafka_bootstrap_server,String deviceSerial) {
        this(vedioAddress,targetFilePath,deviceSerial);
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
     * @param delay 单位秒
     */
    public void pull(FFmpegFrameGrabber grabber, int delay) throws Exception {
        this.recorder = initRecorder(grabber);
        if(null == recorder) {
            return;
        }
        double frameRate = grabber.getFrameRate();
        long startTime_null = System.currentTimeMillis();
        boolean restart = false;

        long start=LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));

        while (true) {
            try {
                Frame frame = grabber.grabFrame();
                if (null == frame || null == frame.image) {
                    continue;
                }
                recorder.record(frame);
                long end = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));
                if(end - start >=delay) {
                    logger.info(String.format("定时关闭record,生成新视频"));
                    stopRecorder(Thread.currentThread());
                    break;
                }
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
                        this.send2Kafka(frame,getTargetFilePath(),index.get());
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
                recorder.stop();
                recorder.release();
                pull(grabber,delay);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecorder(Thread thread) {
        try {
            thread.interrupt();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FFmpegFrameRecorder initRecorder(FFmpegFrameGrabber grabber){
        try {
            Integer frameWight = grabber.getImageWidth();
            Integer frameHeigh = grabber.getImageHeight();

            if(frameWight < 780) {
                frameWight = 780;
            }
            String format= grabber.getFormat();
            String vedioName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH"));
            String vedioPath = this.targetFilePath.concat(vedioName).concat("_").concat(this.getVedio_index().get()+"").concat(".").concat(format);
            logger.info(String.format("摄像头录制的视频地址：%s",vedioPath));
            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(vedioPath,frameWight,frameHeigh,1);
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            recorder.setFormat(format);
            recorder.setFrameRate(10d);
            recorder.setGopSize(10);
            recorder.start();
            return recorder;
        } catch (FrameRecorder.Exception e) {
            logger.info("recorder初始化失败"+e);
        }

        return null;
    }

    public void start(VideoRecordThread videoRecordThread) {
        try {
            videoRecordThread.interrupt();
            start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static final String kafka_topic = "to_processed_frame";
    protected void send2Kafka(Frame frame, String videoName, int frameIndex) throws IOException {
        if(null != producer) {
            byte[] byte_data = write2Os(frame);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("videoName",videoName);
            jsonObject.put("frameIndex",frameIndex);
            jsonObject.put("img_data",byte_data);
            jsonObject.put("deviceSerial",this.deviceSerial);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(kafka_topic,jsonObject.toJSONString());
            producer.send(producerRecord, (metadata, exception) -> {
                if (exception != null) {
                    exception.printStackTrace();
                    logger.info(exception.getMessage());
                } else {
                    logger.info("摄像头=["+this.deviceSerial+"],所属视频=["+videoName+"],Frameindex=" + frameIndex + "发送到Kafka成功");
                }

            });
        }

    }

    private void createFile(String filePath) throws Exception {
        logger.info("创建文件夹及文件，path="+filePath);
        File file = new File(filePath);
        if(file.isFile()) {
            if(!file.exists()) {
                file.createNewFile();
            }
        } else {
            if(!file.exists()) {
                file.mkdirs();
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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bi, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * 启动器
     * @param grabber
     * @param delay 时间间隔 单位秒
     * @throws Exception
     */
    public void start(FFmpegFrameGrabber grabber, int delay) throws Exception {
        executors.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    if(null != recorder) {
                        recorder.stop();
                        recorder.release();
                    }
                    pull(grabber,delay);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },0,60,TimeUnit.SECONDS);
    }

    /**
     * 视频拉取器启动方法，默认两个小时 7200s拉取生成一个视频
     * @throws Exception
     */
    public void start() throws Exception {
        vedio_index = new AtomicInteger(0);
        logger.info("拉取视频源："+this.vedioAddress+"\n存储地址："+this.targetFilePath);
        if(!this.targetFilePath.endsWith(File.separator)) {
            setTargetFilePath(targetFilePath.concat(File.separator));
        }

        if(!getFinalVideoPath().contains(this.deviceSerial)) {
            setTargetFilePath(this.targetFilePath.concat(this.deviceSerial).concat(File.separator));
        }

        createFile(getTargetFilePath());
        final FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(this.vedioAddress);
        grabber.start();

        start(grabber,7200);
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

    public AtomicInteger getVedio_index() {
        return vedio_index;
    }

    public void setVedio_index(AtomicInteger vedio_index) {
        this.vedio_index = vedio_index;
    }

    public static void main(String[] args) {
        try {
            String uri = "rtmp://rtmp01open.ys7.com/openlive/60212ce632c341028b6da41da5dc4121.hd";
            VedioPulper pulper = new VedioPulper(uri,System.getProperty("user.home"),"D21784420");
            pulper.start();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }
}
