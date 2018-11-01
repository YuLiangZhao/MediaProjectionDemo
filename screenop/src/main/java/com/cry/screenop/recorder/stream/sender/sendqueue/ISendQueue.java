package com.cry.screenop.recorder.stream.sender.sendqueue;


import com.cry.screenop.recorder.stream.entity.Frame;

/**
 * @Title: ISendQueue
 * @Package com.laifeng.sopcastsdk.stream.sender.sendqueue
 * @Description:
 * @Author Jim
 * @Date 2016/11/21
 * @Time 上午10:15
 * @Version
 */

public interface ISendQueue {
    void start();
    void stop();
    void setBufferSize(int size);
    void putFrame(Frame frame);
    Frame takeFrame();
    void setSendQueueListener(SendQueueListener listener);
}
