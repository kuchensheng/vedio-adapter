package com.sxc.adapter.vedio;

import java.util.List;

/**
 * ClassName:ReceiveDataModel
 * Description: 从算法识别侧接收的信息模型
 *
 * @author: kuchensheng
 * @version: Create at:  15:28
 * _
 * Copyright:   Copyright (c)2019
 * Company:     songxiaocai
 * _
 * Modification History:
 * Date              Author      Version     Description
 * ------------------------------------------------------------------
 * 15:28   kuchensheng    1.0
 */
public class ReceiveDataModel {

    /**
     * 视频名称
     */
    private String vedioName;

    /**
     * 帧的索引
     */
    private int frameIndex;

    /**
     * 方框左上角的横坐标
     */
    private int x_coordinate;

    /**
     * 方框左上角的纵坐标
     */
    private int y_coordinate;

    /**
     * 方框的宽度
     */
    private int width;

    /**
     * 方框的高度
     */
    private int height;

    /**
     * 旋转角度
     */
    private int rotate;

    /**
     * 摄像头序列号
     */
    private String deviceSerial;

    public String getVedioName() {
        return vedioName;
    }

    public void setVedioName(String vedioName) {
        this.vedioName = vedioName;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public int getX_coordinate() {
        return x_coordinate;
    }

    public void setX_coordinate(int x_coordinate) {
        this.x_coordinate = x_coordinate;
    }

    public int getY_coordinate() {
        return y_coordinate;
    }

    public void setY_coordinate(int y_coordinate) {
        this.y_coordinate = y_coordinate;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public String getDeviceSerial() {
        return deviceSerial;
    }

    public void setDeviceSerial(String deviceSerial) {
        this.deviceSerial = deviceSerial;
    }
}
