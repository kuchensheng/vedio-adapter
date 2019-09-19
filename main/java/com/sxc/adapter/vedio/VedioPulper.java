package com.sxc.adapter.vedio;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.ByteBufferSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_objdetect;
import org.bytedeco.opencv.opencv_core.IplImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.bytedeco.ffmpeg.global.avutil.AVCOL_RANGE_MPEG;

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

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);

    private static volatile  boolean state = false;

    private static Logger logger = Logger.getLogger(VedioPulper.class.getSimpleName());

    private String vedioAddress;

    private String targetFilePath;

    private String kafka_bootstrap_server;

    private KafkaProducer<String,String> producer;

    private static Queue<String> queue = new LinkedBlockingQueue<>();

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
        logger.info("拉取视频源："+this.vedioAddress+"\n存储地址："+this.targetFilePath);
        if(!this.targetFilePath.endsWith(File.separator)) {
            setTargetFilePath(targetFilePath.concat(File.separator));
        }

        createFile(targetFilePath.concat("frames/"));

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(this.vedioAddress);
        grabber.start();

        //开启监听frames
        final String vedioPath = this.targetFilePath.concat("vedio.mp4");
        final Integer frameWight = grabber.getImageWidth();
        final Integer frameHeigh = grabber.getImageHeight();

        final double frameRate = grabber.getFrameRate();
        final long sleepTime = (long) (1/frameRate * 1000);
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(vedioPath,frameWight,frameHeigh,2);
        recorder.setInterleaved(true);
        recorder.setVideoOption("tune","zerolatency");
        recorder.setVideoOption("preset","ultrafast");
        recorder.setVideoOption("crf","28");
        recorder.setVideoBitrate(2000000);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
        recorder.setFormat("mp4");
        recorder.setFrameRate(grabber.getFrameRate());
        recorder.setGopSize(60);
        recorder.start();
        long index = 0l;
        while (true) {
            try {
                Frame frame = grabber.grabFrame();
                if(null != frame && null != frame.image) {
                    recorder.record(frame);
                }
                if(index % (int) (frameRate/2) == 0) {
                    send2Kafka(frame,index);
                }
                TimeUnit.MILLISECONDS.sleep(sleepTime);
                index ++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    private static final String kafka_topic = "to_processed_frame";
    private void send2Kafka(Frame frame,long index) throws IOException {
        if(null != producer) {
            String key = index + "_" + frame.timestamp;
            byte[] byte_data = write2Os(frame);

//            String data = new String(byte_data);
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("key",key);
            jsonObject.put("img_data",byte_data);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(kafka_topic,jsonObject.toJSONString());
            producer.send(producerRecord, (metadata,exception) -> {
                logger.info("图片index="+key+"发送到Kafka成功");
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

    private List<String> frame2Imag(Frame frame) throws Exception{
        FileOutputStream fileOutputStream = null;

        List<String> imagesPath = new ArrayList<>();
        String imagePath = this.targetFilePath.concat(frame.timestamp+"")+".jpg";
        fileOutputStream = new FileOutputStream(imagePath);
        fileOutputStream.write(write2Os(frame));
        imagesPath.add(imagePath);
        return imagesPath;
    }

    private byte[] write2Os(Frame frame) throws IOException {
        int owidth = frame.imageWidth ;
        int oheight = frame.imageHeight ;
        Java2DFrameConverter converter =new Java2DFrameConverter();

        int width = 780;
        int height = (int) (((double) width / owidth) * oheight);
        BufferedImage fecthedImage =converter.getBufferedImage(frame);
        BufferedImage bi = new BufferedImage(owidth, oheight, BufferedImage.TYPE_3BYTE_BGR);
        bi.getGraphics().drawImage(fecthedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH),
                0, 0, null);
//                bi=rotateImage(bi, 90);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bi, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    private void recorderByFrame(FFmpegFrameGrabber grabber, FFmpegFrameRecorder recorder) throws Exception{
        try {
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
//            recorder.setPixelFormat(-1);
            recorder.setFormat(grabber.getFormat());
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.start();

            Frame frame = null;
            while ((frame = grabber.grabFrame()) != null) {
                recorder.record(frame);
            }
            recorder.stop();
            grabber.stop();
        } finally {
            grabber.stop();
        }
    }

    public void start() {
        state = true;
        try {
            pull();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    enum VedioType {
        FLV,MP4
    }

    public String getVedioAddress() {
        return vedioAddress;
    }

    public void setVedioAddress(String vedioAddress) {
        this.vedioAddress = vedioAddress;
    }

    public String getTargetFilePath() {
        return targetFilePath;
    }

    public void setTargetFilePath(String targetFilePath) {
        this.targetFilePath = targetFilePath;
    }

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
            String uri = "rtmp://rtmp01open.ys7.com/openlive/c5c1ba029e784f719b7ba3bf1f772c19";
            VedioPulper pulper = new VedioPulper(uri,"/Users/kuchensheng/Desktop/test/","kafka1:9092,kafka2:9092,kafka3:9092");
        pulper.start();

//            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(uri);
//            grabber.start();
//            pulper.shwoFrames(grabber);
//            grabber.stop();
//            grabber.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }
    }
}
