package com.cry.screenop.recorder.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;

/**
 * Created by a2957 on 4/24/2018.
 */

public class VideoEncoder {
    int height;
    int bitrate;
    int framerate;
    int iframeInterval;
    String codecName;
    String mimeType;
    MediaCodecInfo.CodecProfileLevel codecProfileLevel;
    //配置.这些配置需要经过Capabilities来进行校验
    private int width;
    //MediaCodec的CallBack,异步的设置
    private android.media.MediaCodec.Callback mCallBack;
    //用来发送消息的Handler
    private Handler mCodecCallBackHandler;

    private MediaCodec mEncoder;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private MediaCodec.BufferInfo[] mOutputBufferInfo;
    private ArrayDeque mAvailableInputBuffers;
    private ArrayDeque mAvailableOutputBuffers;


    public void prepare() {
        //-1.创建MediaFormat
        MediaFormat format = createMediaFormat();
        //0.创建Encoder
        MediaCodec encoder = createEncoder();

        //0-1.设置CallBack
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //不设置Handler，则表示使用默认的Handler
            encoder.setCallback(mCallBack, null);
        }
        //1.对Encoder进行配置
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        //2.开始encoder的编码
        encoder.start();

        mEncoder = encoder;
    }


    public void initAfterPrepare() {
        if (mEncoder == null) {
            return;
        }
        //先将buffer取出来
        mInputBuffers = mEncoder.getInputBuffers();
        mOutputBuffers = mEncoder.getOutputBuffers();
        //根据Buffer的大小，创建自己的Buffer
        mOutputBufferInfo = new MediaCodec.BufferInfo[mOutputBuffers.length];
        //根据各自的情况，创建队列
        mAvailableInputBuffers = new ArrayDeque<>(mOutputBuffers.length);
        mAvailableOutputBuffers = new ArrayDeque<>(mInputBuffers.length);
    }


    /*
    因为这里是视频，所以创建的是VideoEncoder
     */
    private MediaFormat createMediaFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(mimeType, width, height);
        //还需要对器进行插值。设置自己设置的一些变量
        //设置ColorFormat??
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitrate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iframeInterval);
        //对profile进行插值
        if (codecProfileLevel != null && codecProfileLevel.profile != 0 && codecProfileLevel.level != 0) {
            format.setInteger(MediaFormat.KEY_PROFILE, codecProfileLevel.profile);
            format.setInteger("level", codecProfileLevel.level);
        }
        return format;
    }

    //根据Codecame来创建MediaEncoder
    private MediaCodec createEncoder() {
        MediaCodec encoder = null;
        try {
            encoder = MediaCodec.createEncoderByType(codecName);
        } catch (IOException e) {
            e.printStackTrace();

        }
        return encoder;
    }
}
