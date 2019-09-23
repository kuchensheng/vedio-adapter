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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static final String URL_GET_VEDIO_LIST = "https://open.ys7.com/api/lapp/live/video/list";

    public static final String URL_GET_VEDIO_WITH_DEVICESERIAL="https://open.ys7.com/api/lapp/live/address/limited";

    private static final CloseableHttpClient httpClient = HttpClients.createDefault();

    @Override
    public List<VedioDataAddressModel> vedioList(String accessToken) throws Exception {

        HttpPost request = new HttpPost(URL_GET_VEDIO_LIST);
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("accessToken",accessToken));
        if(null == accessToken || accessToken.trim().length() ==0 ) {
            throw new Exception("accessToke is required");
        }

        formParams.add(new BasicNameValuePair("pageStart","0"));
        formParams.add(new BasicNameValuePair("pageSize","500"));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams,"UTF-8");
        request.setEntity(entity);

        CloseableHttpResponse response = httpClient.execute(request);
        InputStream content = response.getEntity().getContent();
        List<VedioDataAddressModel> result = new LinkedList<>();
        if(null != content) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(content,"UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                stringBuilder.append(s);
            }
            reader.close();
            JSONObject jsonObject = JSONObject.parseObject(stringBuilder.toString());
            JSONArray data = jsonObject.getJSONArray("data");
            result = data.toJavaList(VedioDataAddressModel.class);
        }
        return result;
    }

    @Override
    public List<IVedioPullService> pullAllVedio(String accessToken, String targetPath) throws Exception{
        List<VedioDataAddressModel> vedioDataAddressModels = vedioList(accessToken);
        List<IVedioPullService> result = new ArrayList<>(vedioDataAddressModels.size());
        for (VedioDataAddressModel addressModel : vedioDataAddressModels) {
            new VedioPulper(addressModel.getLiveAddress(),targetPath).start();
            result.add(this);
        }
        return result;
    }

    @Override
    public List<IVedioPullService> pullAllVedio(String appKey, String secret, String targetPath) throws Exception{
        return pullAllVedio(getAccessToken(appKey,secret),targetPath);
    }

    @Override
    public String getAccessToken(String appkey, String secret) {

        return null;
    }

    @Override
    public IVedioPullService pullVedioWithDeviceSerial(String accessToken, String[] deviceSerial, String targetPath) {
        return null;
    }
}
