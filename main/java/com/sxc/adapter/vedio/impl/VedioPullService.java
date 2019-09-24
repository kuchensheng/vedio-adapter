package com.sxc.adapter.vedio.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sxc.adapter.vedio.IVedioPullService;
import com.sxc.adapter.vedio.VedioDataAddressModel;
import com.sxc.adapter.vedio.VedioPulper;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.bytedeco.javacv.FrameGrabber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * ClassName:VedioPullService
 * Description: TODO
 *
 * @author: kuchensheng
 * @version: Create at:  15:29
 * _
 * Copyright:   Copyright (c)2019
 * Company:     songxiaocai
 * _
 * Modification History:
 * Date              Author      Version     Description
 * ------------------------------------------------------------------
 * 15:29   kuchensheng    1.0
 */
public class VedioPullService implements IVedioPullService {

    private static final Logger logger = LoggerFactory.getLogger(VedioPullService.class);
    /**
     * 获取视频列表
     */
    public static final String URL_GET_VEDIO_LIST = "https://open.ys7.com/api/lapp/live/video/list";

    /**
     * 获取指定设备的视频信息
     */
    public static final String URL_GET_VEDIO_WITH_DEVICESERIAL="https://open.ys7.com/api/lapp/live/address/limited";

    /**
     * 获取AccessToken
     */
    public static final String URL_GET_ACCESSTOKEN="https://open.ys7.com/api/lapp/token/get";

    private static final CloseableHttpClient httpClient = HttpClients.createDefault();

    @Override
    public List<VedioDataAddressModel> vedioList(String accessToken) throws Exception {
        logger.info("获取accessToken={}对应的所有的视频列表信息",accessToken);
        HttpPost request = new HttpPost(URL_GET_VEDIO_LIST);
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("accessToken",accessToken));
        if(null == accessToken || accessToken.trim().length() ==0 ) {
            logger.error("accessToken is required");
            throw new Exception("accessToken is required");
        }

        formParams.add(new BasicNameValuePair("pageStart","0"));
        formParams.add(new BasicNameValuePair("pageSize","500"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams,"UTF-8");
        request.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(request);
        if (response.getStatusLine().getStatusCode() != 200) {
            logger.error("视频列表查询失败，请检查,statusCode={}",response.getStatusLine().getStatusCode());
        }
        InputStream content = response.getEntity().getContent();
        List<VedioDataAddressModel> result = new LinkedList<>();
        if(null != content) {
            String strContent = readInputStream(content);
            JSONObject jsonObject = JSONObject.parseObject(strContent);
            JSONArray data = jsonObject.getJSONArray("data");
            if(null == data || data.size() == 0) {
                return null;
            }
            logger.info("共获取到{}条视频列表",data.size());
            result = data.toJavaList(VedioDataAddressModel.class);
        }
        return result;
    }

    private String readInputStream(InputStream content) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(content,"UTF-8"));
        StringBuilder stringBuilder = new StringBuilder();
        String s;
        while ((s = reader.readLine()) != null) {
            stringBuilder.append(s);
        }
        reader.close();
        return stringBuilder.toString();
    }

    @Override
    public List<IVedioPullService> pullAllVedio(String accessToken, String targetPath) throws Exception{
        logger.info("将获取到的视频列表逐个启动视频录制,accessToken:{},targetPath:{}",accessToken,targetPath);
        List<VedioDataAddressModel> vedioDataAddressModels = vedioList(accessToken);
        List<IVedioPullService> result = new ArrayList<>(vedioDataAddressModels.size());
        for (VedioDataAddressModel addressModel : vedioDataAddressModels) {
            new VedioPulper(addressModel.getLiveAddress(),targetPath,addressModel.getDeviceSerial()).start();
            result.add(this);
        }
        return result;
    }

    @Override
    public List<IVedioPullService> pullAllVedioAndCutFrame(String accessToken, String targetPath, String frameTargetAddress, FrameTargetTypeEnum type) throws Exception {
        logger.info("将获取到的视频列表逐个启动视频录制,accessToken:{},targetPath:{}",accessToken,targetPath);
        List<VedioDataAddressModel> vedioDataAddressModels = vedioList(accessToken);
        List<IVedioPullService> result = new ArrayList<>(vedioDataAddressModels.size());
        for (VedioDataAddressModel addressModel : vedioDataAddressModels) {
            new VedioPulper(addressModel.getLiveAddress(),targetPath,frameTargetAddress,type,addressModel.getDeviceSerial()).start();
            result.add(this);
        }
        return result;
    }

    @Override
    public List<IVedioPullService> pullAllVedio(String appKey, String secret, String targetPath) throws Exception{
        return pullAllVedio(getAccessToken(appKey,secret),targetPath);
    }

    @Override
    public String getAccessToken(String appkey, String secret) throws Exception{
        HttpPost request = new HttpPost(URL_GET_ACCESSTOKEN);
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("appKey",appkey));
        formParams.add(new BasicNameValuePair("appSecret",secret));

        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams,"UTF-8");
        request.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(request);
        InputStream content = response.getEntity().getContent();

        if(null == content) {
            return null;
        }
        String readInputStream = readInputStream(content);
        JSONObject jsonObject = JSONObject.parseObject(readInputStream);
        JSONObject data = jsonObject.getJSONObject("data");
        if(null == data) {
            return null;
        }
        String accessToken = data.getString("accessToken");
        long expireTime = data.getLongValue("expireTime");
        logger.info("获取到token：{},超时时间",accessToken, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(expireTime),ZoneId.systemDefault())
        ));
        return accessToken;
    }

    @Override
    public List<IVedioPullService> pullVedioWithDeviceSerial(String accessToken, String[] deviceSerials, String targetPath) throws Exception{
        HttpPost request = new HttpPost(URL_GET_VEDIO_WITH_DEVICESERIAL);
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("accessToken",accessToken));
        formParams.add(new BasicNameValuePair("channelNo","1"));

        List<IVedioPullService> result = new ArrayList<>();
        for (String deviceSerial : deviceSerials) {
            formParams.add(new BasicNameValuePair("deviceSerial",deviceSerial));
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams,"UTF-8");
            request.setEntity(entity);

            CloseableHttpResponse response = httpClient.execute(request);
            InputStream content = response.getEntity().getContent();
            if(response.getStatusLine().getStatusCode() != 200) {
                logger.error("获取直播地址失败，deviceSerial={}",deviceSerial);
                continue;
            }
            if(null == content) {
                continue;
            }

            String readInputStream = readInputStream(content);
            JSONObject dataJson = JSONObject.parseObject(readInputStream);
            JSONObject data = dataJson.getJSONObject("data");
            new VedioPulper(data.getString("liveAddress"),targetPath,deviceSerial).start();
            result.add(this);
        }

        return result;
    }
}
