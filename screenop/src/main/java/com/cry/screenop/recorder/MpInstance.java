package com.cry.screenop.recorder;

import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Handler;
import android.os.Message;

import com.cry.screenop.SurfaceFactory;
import com.cry.screenop.shot.RxScreenShot;

/**
 * 屏幕录制。需要使用MediaCodeC
 * Created by a2957 on 4/24/2018.
 */
public class MpInstance {
    private String TAG = "MpInstance";

    private Handler mCallBackHandler = new CallBackHandler();
    private MediaCallBack mMediaCallBack = new MediaCallBack();

    private MediaProjection mediaProjection;
    SurfaceFactory mSurfaceFactory;


    //需要配置的参数
    public int width = 480;
    public int height = 720;
    public int dpi = 1;

    private MpInstance(MediaProjection mediaProjection) {
        this.mediaProjection =
                mediaProjection;
    }

    public static MpInstance of(MediaProjection mediaProjection) {
        return new MpInstance(mediaProjection);
    }


    //创建MediaCodec
    public void createMediaCodec() {
        //创建Codec，需要先获取当前能够支持的Codec列表


        //注意这里使用RGB565报错提示，只能使用RGBA_8888
//        mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1000);
//        mSurfaceFactory = new RxScreenShot.ImageReaderSurface(mImageReader);
//        createProject();
//        return this;
    }


    private void createProject() {
        mediaProjection.registerCallback(mMediaCallBack, mCallBackHandler);
        mediaProjection.createVirtualDisplay(TAG + "-display", width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mSurfaceFactory.getInputSurface(), null, null);
    }

    class MediaCallBack extends MediaProjection.Callback {
        @Override
        public void onStop() {
            super.onStop();
            //这里还需要进行处理
        }
    }

    static class CallBackHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    }
}
