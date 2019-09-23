package com.sxc.adapter.vedio;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.IplImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * ClassName:Video2JpgUtil
 * Description: TODO
 *
 * @author: kuchensheng
 * @version: Create at:  10:44
 * _
 * Copyright:   Copyright (c)2019
 * Company:     songxiaocai
 * _
 * Modification History:
 * Date              Author      Version     Description
 * ------------------------------------------------------------------
 * 10:44   kuchensheng    1.0
 */
public class Video2JpgUtil {

    private static final Logger logger = Logger.getLogger(Video2JpgUtil.class.getSimpleName());

    public static final String URL_GET_VEDIO_LIST = "https://open.ys7.com/api/lapp/live/video/list";

    public static final String URL_GET_VEDIO_WITH_DEVICESERIAL="https://open.ys7.com/api/lapp/live/address/limited";

    private static ExecutorService executorService = Executors.newFixedThreadPool(10);

    private static ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 * 512);
    /**
     * 截取视频指定帧，当帧的index % margin == dev的时候截取帧
     * @param ff 视频流
     * @param indexFrame 帧截取开始位置
     * @param margin 每截取一帧跳过多少帧
     * @param dev 余数 默认为0
     * @return
     * @throws Exception
     */
    public static Map<String,byte[]> fetchFrame(FFmpegFrameGrabber ff, Integer indexFrame, Integer margin, Integer dev) throws Exception {

        if (null == indexFrame) {
            indexFrame = 0;
        }
        if (null == dev) {
            dev = 0;
        }

        int length = ff.getLengthInFrames();
        Map<String,byte[]> frameTreeMap = new HashMap<>();
        for (int i = indexFrame.intValue();i<length;i++) {
            if(i % margin == dev) {
                Frame frame = ff.grabFrame();
                if(null == frame || null == frame.image) {
                    int innner = i;
                    while (innner < (i + 1)*margin) {
                        frame = ff.grabFrame();
                        if((innner >= i) && frame.image != null) {
                            break;
                        }
                    }
                }
                if(null == frame.image) {
                    continue;
                }

                frameTreeMap.put(i+"",write2Os(frame));
            }


        }
        return frameTreeMap;

    }

    /**
     * 获取指定位置的帧的内容
     * @param ff
     * @param indexFrame
     * @return
     * @throws Exception
     */
    public static byte[] getFrameData(FFmpegFrameGrabber ff,Integer indexFrame) throws Exception {
        int i = indexFrame;
        while (i <= (indexFrame + ff.getVideoFrameRate())) {
            Frame frame = ff.grabFrame();
            if(i >= indexFrame && frame.image != null) {
                return write2Os(frame);
            }
        }
        return null;
    }

    /**
     * 获取指定位置的帧的内容
     * @param ff
     * @param indexFrame
     * @return
     * @throws Exception
     */
    public static Frame getFrame(FFmpegFrameGrabber ff,Integer indexFrame) throws Exception {
        int i = indexFrame;
        while (i <= (indexFrame + ff.getVideoFrameRate())) {
            Frame frame = ff.grabFrame();
            if(i >= indexFrame && null !=frame && frame.image != null) {
                return frame;
            }
        }
        return null;
    }

    /**
     * 将帧写入字节流
     * @param frame 帧信息
     * @return
     * @throws IOException
     */
    private static byte[] write2Os(Frame frame) throws IOException {
        int owidth = frame.imageWidth ;
        int oheight = frame.imageHeight ;
        //TODO 等比压缩
//      int width = 800;
//      int height = (int) (((double) width / owidth) * oheight);
        Java2DFrameConverter converter =new Java2DFrameConverter();

        BufferedImage fecthedImage =converter.getBufferedImage(frame);
        BufferedImage bi = new BufferedImage(owidth, oheight, BufferedImage.TYPE_3BYTE_BGR);
        bi.getGraphics().drawImage(fecthedImage.getScaledInstance(owidth, oheight, Image.SCALE_SMOOTH),
                        0, 0, null);
//                bi=rotateImage(bi, 90);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(bi, "jpg", outputStream);
                return outputStream.toByteArray();
    }


    /**
     * 给图片添加方框和文字
     * @param imageStream 图片流
     * @param mark 需要显示的文字
     * @param x_coordinate x轴起点坐标
     * @param y_coordinate y轴起点坐标
     * @param x_length 方框长度
     * @param y_length 方框高度
     * @param color 方框颜色
     * @param targetFilePath 目标地址
     * @throws Exception
     */
    public static void addMarkBySingleText(InputStream imageStream,String mark,int x_coordinate,int y_coordinate,
                                           int x_length,int y_length, Color color,String targetFilePath) throws  Exception{

        BufferedImage image = ImageIO.read(imageStream);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(12.0f));
        graphics.drawRect(x_coordinate,y_coordinate,x_length,y_length);

        Font font = new Font("微软雅黑",Font.PLAIN,18);
        graphics.setStroke(new BasicStroke(20f));

        graphics.drawRect(x_coordinate + 5,y_coordinate - 30,250,20);

        graphics.setFont(font);
        graphics.setColor(Color.WHITE);

        int x_str_mark = x_coordinate;
        int y_str_mark = y_coordinate - font.getSize();
        graphics.drawString(mark,x_str_mark,y_str_mark);
        createFile(targetFilePath);
        FileOutputStream outputStream = new FileOutputStream(targetFilePath);
        ImageIO.write(image,targetFilePath.substring(targetFilePath.lastIndexOf(".")+1),outputStream);
        outputStream.close();
        outputStream.flush();
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

    /**
     * 获取视频流的每个帧的字节数组
     * @param inputStream
     * @return
     */
    public static List<byte[]> vedioConvert2Frames(InputStream inputStream) throws Exception {
        FFmpegFrameGrabber ff = new FFmpegFrameGrabber(inputStream);
        ff.start();

        return vedioConvert2Frames(ff);
    }

    /**
     * 获取视频流的每个帧的字节数组
     * @param fFmpegFrameGrabber
     * @return
     */
    public static List<byte[]> vedioConvert2Frames(FFmpegFrameGrabber fFmpegFrameGrabber) throws Exception {

        int length = fFmpegFrameGrabber.getLengthInFrames();
        List<byte[]> result = new LinkedList<>();
        for (int i = 0; i < length; i ++) {
            Frame frame = fFmpegFrameGrabber.grabFrame();
            if(frame.image != null) {
                result.add(write2Os(frame));
            }
        }
        return result;
    }

    /**
     * 获取视频流的每个帧的字节数组
     * @param fFmpegFrameGrabber
     * @return
     */
    public static List<byte[]> vedioConvert2Frames(FFmpegFrameGrabber fFmpegFrameGrabber,Integer start,Integer end) throws Exception {

        List<byte[]> result = new LinkedList<>();
        for (int i = start; i < end; i ++) {
            Frame frame = fFmpegFrameGrabber.grabFrame();
            if(frame.image != null) {
                result.add(write2Os(frame));
            }
        }
        return result;
    }


    /**
     * 获取指定帧之前的seconds秒的视频流
     * @param ff 视频流
     * @param indexFrame 指定帧的位置
     * @param seconds 指定的秒数
     * @return
     * @throws Exception
     */
    public static List<String> getBeforeSecondsFrames(FFmpegFrameGrabber ff ,Integer indexFrame,Integer seconds,String targetFramePath,String markFilePath) throws Exception{
        //视频帧数
        int length = ff.getLengthInFrames();
        //视频长度
        long duration = ff.getLengthInTime()/1000L /1000L;

        int startFrameIndex = indexFrame - (int)((length/duration) * seconds);

        startFrameIndex = startFrameIndex < 0 ? 0 : startFrameIndex;

        List<String> fileNames = vedioConvert2Frames(ff, startFrameIndex, indexFrame,targetFramePath);

//        byte[] data = getFrameData(ff,indexFrame);
//        String endFileName = write2Os(data,targetFramePath +fileNames.size()+".jpg");
        for (int i = 0;i<24;i++) {
            fileNames.add(markFilePath);
        }
        return fileNames;

    }

    private static List<String> vedioConvert2Frames(FFmpegFrameGrabber ff, int start, Integer end, String targetFramePath) throws Exception{
        List<String> result = new LinkedList<>();
        targetFramePath = targetFramePath.endsWith(File.separator) ? targetFramePath : targetFramePath.concat(File.separator);
        for (int i = start; i <= end; i ++) {

            Frame frame = ff.grabFrame();
            if(null != frame && frame.image != null) {

                String filePath = targetFramePath.concat("final_frame_"+i+".jpg");
                createFile(targetFramePath);

                result.add(write2Os(write2Os(frame),filePath));
            }
        }
        return result;
    }

    private static String write2Os(byte[] image,String target) throws Exception{
        FileOutputStream fileOutputStream = new FileOutputStream(target);
        fileOutputStream.write(image);
        fileOutputStream.close();
        return target;

    }

    /**
     * 根据帧信息合成视频
     * @param FilenameList 帧所在位置列表
     * @param targetFile 视频生成地址
     * @param imageWidth 帧的宽度
     * @param imageHeight 帧的高度
     * @param frameRate 帧的频率，1秒播放多少帧，默认24帧
     * @throws FrameRecorder.Exception
     */
    public static void createVedio(List<String> FilenameList,String targetFile,Integer imageWidth,Integer imageHeight,Integer frameRate) throws FrameRecorder.Exception {
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(targetFile,imageWidth,imageHeight);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("flv");
        recorder.setFrameRate((null == frameRate || frameRate < 20 ) ? 24 : frameRate);
//        recorder.setPixelFormat(0);
        recorder.start();

        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        for (String fileName : FilenameList) {
            IplImage image = opencv_imgcodecs.cvLoadImage(fileName);
            Frame frame = converter.convert(image);
            recorder.record(frame);
            opencv_core.cvReleaseImage(image);
        }
        recorder.stop();
        recorder.close();
    }



    public static VedioDataAddressModel getVedioInputStreamAddessWithDeviceSerial(String accessToken,String deviceSerial,Integer channelNo,Integer expireTime) throws Exception{
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost request = new HttpPost(URL_GET_VEDIO_WITH_DEVICESERIAL);
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("accessToken",accessToken));
        formParams.add(new BasicNameValuePair("deviceSerial",deviceSerial));
        formParams.add(new BasicNameValuePair("channelNo",channelNo+""));
        if(null != expireTime && expireTime != 0) {
            formParams.add(new BasicNameValuePair("expireTime",expireTime+""));
        }

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams,"UTF-8");
        request.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(request);
        InputStream content = response.getEntity().getContent();

        if(null != content) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(content, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                stringBuilder.append(s);
            }
            reader.close();
            JSONObject jsonObject = JSONObject.parseObject(stringBuilder.toString());
            VedioDataAddressModel model = JSONObject.parseObject(jsonObject.getString("data"),VedioDataAddressModel.class);
            return  model;
        }
        
        return null;
        
    }

    public static void main(String[] args) throws Exception {
        String accessToken = "at.dwy9dz86brqcexan5okk0wfc6ruplqsp-2dmn4vbr7f-0pa3qo1-dlbabghfa";
        String deviceSerial="228002873";
//        List<VedioDataAddressModel> vedioInputStreamAddress = getVedioInputStreamAddress("at.dwy9dz86brqcexan5okk0wfc6ruplqsp-2dmn4vbr7f-0pa3qo1-dlbabghfa", 0, 15);
//        for (VedioDataAddressModel vedioDataAddressModel : vedioInputStreamAddress) {
//            System.out.println(vedioDataAddressModel.toString());
//        }
        List<String> veidoAddress = getVedioStream(accessToken,deviceSerial,VedioTypeEnum.LIVE);




        for (String addr : veidoAddress) {
            test1(addr);
        }
        buffer.clear();

    }

    enum VedioTypeEnum {
        HD,LIVE,RTMP,HD_RPTM;
    }

    public static List<String> getVedioStream(String accessToken,String deviceSerial,VedioTypeEnum type) throws Exception {
        VedioDataAddressModel vedioInputStreamAddessWithDeviceSerial = getVedioInputStreamAddessWithDeviceSerial(accessToken, deviceSerial, 1, null);

        if(null == vedioInputStreamAddessWithDeviceSerial) {
            throw new Exception("无法获取对应设备信息");
        }
        String vedioAddress;
        switch (type) {
            case HD:
                vedioAddress = vedioInputStreamAddessWithDeviceSerial.getHdAddress();
                break;
            case RTMP:
                vedioAddress = vedioInputStreamAddessWithDeviceSerial.getRtmp();
                break;
            case HD_RPTM:
                vedioAddress = vedioInputStreamAddessWithDeviceSerial.getRtmpHd();
                break;
                default:
                    vedioAddress = vedioInputStreamAddessWithDeviceSerial.getLiveAddress();
                    break;
        }
        logger.info("视频地址："+vedioAddress);

        String indexContent = getIndexFile(vedioAddress);

        logger.info("内容解析："+indexContent);

        List<String> list = analysisIndex(indexContent);
        for (String content : list) {
            logger.info("视频ts地址："+content);
        }

        List<String> result = downLoadIndexFile(list);

        mergeFile2Vedio(result,"/Users/kuchensheng/Desktop/merge.mp4");

    return result;

    }

    private static void mergeFile2Vedio(List<String> result,String target) throws Exception{

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(new File(target),800,600);
//        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(targetFile,imageWidth,imageHeight);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
        recorder.setFormat("mp4");
        recorder.setFrameRate(24);
//        recorder.setPixelFormat(0);
        recorder.start();

        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
        for (String fileName : result) {
            IplImage image = opencv_imgcodecs.cvLoadImage(fileName);
            Frame frame = converter.convert(image);
            recorder.record(frame);
            opencv_core.cvReleaseImage(image);
        }
        recorder.stop();
        recorder.close();
    }

    private static List<String> downLoadIndexFile(List<String> list) throws Exception {
        List<String> result = new ArrayList<>();
        for (int i = 0 ;i < list.size();i++) {
            URL url = new URL(list.get(i));
            URLConnection conn = url.openConnection();
            InputStream content = conn.getInputStream();
            String filePath = "/Users/kuchensheng/Desktop/songxiaocai_"+i+".ts";
            createFile(filePath);
            FileOutputStream fileOutputStream = new FileOutputStream(new File(filePath));
            byte[] data = new byte[1024];
            while (content.read(data) != -1) {
                fileOutputStream.write(data);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            content.close();
            result.add(filePath);
        }
        return result;
//        return outputStream;
    }

    private static List analysisIndex(String content) {
        Pattern pattern = Pattern.compile(".*ts");
        Matcher ma = pattern.matcher(content);

        List<String> list = new ArrayList<>();

        while(ma.find()){
            String s = ma.group();
            list.add(s);
            System.out.println(s);
        }
        return list;
    }

    private static String getIndexFile(String vedioAddress) throws Exception{
        URL url = new URL(vedioAddress);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(),"UTF-8"));
        StringBuilder contentBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            contentBuilder.append(line).append("\n");
        }
        reader.close();
        return contentBuilder.toString();
    }

    private static void test1(String vedioAddress) throws Exception{
//        File file = new File("/Users/kuchensheng/Desktop/panda_new.mp4");
//        FileInputStream inputStream = new FileInputStream(file);
        FFmpegFrameGrabber ff = new FFmpegFrameGrabber(vedioAddress);
        ff.start();
        Map<String, byte[]> jpg = fetchFrame(ff, 0, 5,0);
        Map<String,byte[]> subJpgMap = new HashMap<>(5);
        for (String key : jpg.keySet()) {
            if(Integer.valueOf(key) % 13 == 0) {
                subJpgMap.put(key,jpg.get(key));
            }
        }

        List<String> finalVedioData = new LinkedList<>();
        int i = 1;
        Random random = new Random();
        for (String key : subJpgMap.keySet()) {
            addMarkBySingleText(new ByteArrayInputStream(subJpgMap.get(key)),
                    "卖大蔬 "+ LocalDateTime.now().plusSeconds(i++).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),random.nextInt(300),random.nextInt(400),290,300,
                    Color.RED,"/Users/kuchensheng/Desktop/panda/image"+key+".jpg");
            finalVedioData.addAll(getBeforeSecondsFrames(ff,Integer.valueOf(key),2,"/Users/kuchensheng/Desktop/panda/final/","/Users/kuchensheng/Desktop/panda/image"+key+".jpg"));
        }
        Frame frame = getFrame(ff,10);
        createVedio(finalVedioData,"/Users/kuchensheng/Desktop/panda_new_1.mp4",frame.imageWidth,frame.imageHeight,24);
        ff.close();
    }
}
