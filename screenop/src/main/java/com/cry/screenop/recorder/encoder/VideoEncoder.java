package com.cry.screenop.recorder.encoder;

import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VideoEncoder
 * Created by a2957 on 4/24/2018.
 */
public class VideoEncoder {
    private static final int INVALID_INDEX = -1;
    private static final int MSG_START = 0;
    private static final int MSG_STOP = 1;
    private static final int MSG_ERROR = 2;
    private static final int STOP_WITH_EOS = 1;
    //常数配置项
    private final String TAG = "VideoEncoder";
    int height;
    int bitrate;
    int framerate;
    int iframeInterval;
    String codecName;
    String mimeType;
    //profile high medium baseline
    MediaCodecInfo.CodecProfileLevel codecProfileLevel;
    //配置项
    private int mWidth;
    private int mHeight;
    private int mDpi;
    private String mDstPath;
    //配置.这些配置需要经过Capabilities来进行校验
    private int width;
    private MediaProjection mMediaProjection;
    private AtomicBoolean mForceQuit = new AtomicBoolean(false);
    private AtomicBoolean mIsRunning = new AtomicBoolean(false);
    private VirtualDisplay mVirtualDisplay;
    private HandlerThread mWorker;
    private CallbackHandler mHandler;
    private Surface mEncoderInputSurface;
    //视频的输出格式
    private MediaFormat mVideoOutputFormat = null;
    private MediaMuxer mMuxer;
    private MediaCodec mEncoder;
    //这个index是什么？
    private int mVideoTrackIndex = INVALID_INDEX;
    private long mVideoPtsOffset;
    private boolean mMuxerStarted = false;
    private MediaProjection.Callback mProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            quit();
        }
    };
    private LinkedList<Integer> mPendingVideoEncoderBufferIndices = new LinkedList<>();
    private LinkedList<Integer> mPendingAudioEncoderBufferIndices = new LinkedList<>();
    private LinkedList<MediaCodec.BufferInfo> mPendingAudioEncoderBufferInfos = new LinkedList<>();
    private LinkedList<MediaCodec.BufferInfo> mPendingVideoEncoderBufferInfos = new LinkedList<>();
    //MediaCodec的CallBack,异步的设置
    private android.media.MediaCodec.Callback mCallBack = new MediaCodec.Callback() {
        /*
        InputBuffer可以使用了。传递回来codec和Index.这部分是将buffer填充完毕。往codec中填充
         */
        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            Log.i(TAG, "VideoEncoder input buffer available: index=" + index);
        }

        /*
        outputBuffer可以使用了。传递回来codec和Index。处理完了，往回传递
        */
        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            Log.i(TAG, "VideoEncoder output buffer available: index=" + index);
            try {
                //开始去Mux
                muxVideo(index, info);
            } catch (Exception e) {
                Log.e(TAG, "Muxer encountered an error! ", e);
                Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();
            }

        }

        /*
        出错了！！
         */
        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            e.printStackTrace();
            Log.e(TAG, "VideoEncoder ran into an error! ", e);
            Message.obtain(mHandler, MSG_ERROR, e).sendToTarget();

        }

        /*
        输出的格式发生了变化。则需要重新设置OutputFormat.
        这个方法在初始化的时候，就会回调。告诉你。outputFormat产生了变化
         */
        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            //需要重置输出的format
            resetVideoOutputFormat(format);
            //保存完格式，就可以开始合视频了
            startMuxerIfReady();
        }
    };

    public VideoEncoder(int mWidth, int mHeight, int mDpi, String mDstPath, int width, int height, int bitrate, int framerate, int iframeInterval, String codecName, String mimeType, MediaCodecInfo.CodecProfileLevel codecProfileLevel, MediaProjection mMediaProjection) {
        this.mWidth = mWidth;
        this.mHeight = mHeight;
        this.mDpi = mDpi;
        this.mDstPath = mDstPath;
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.framerate = framerate;
        this.iframeInterval = iframeInterval;
        this.codecName = codecName;
        this.mimeType = mimeType;
        this.codecProfileLevel = codecProfileLevel;
        this.mMediaProjection = mMediaProjection;
    }

    private void muxVideo(int index, MediaCodec.BufferInfo info) {
        //先取出处理后的数据

        /*
        如果还未开始muxer
         */
        if (!mMuxerStarted || mVideoTrackIndex == INVALID_INDEX) {
            mPendingVideoEncoderBufferIndices.add(index);
            mPendingVideoEncoderBufferInfos.add(info);
            return;
        }
        //先从encoder中得到output
        ByteBuffer outputBuffer = mEncoder.getOutputBuffer(index);
        writeSampleData(mVideoTrackIndex, info, outputBuffer);

        //重新将得到的buffer release ，还给codec.因为我们没有需要显示的surface，所以这里是false
        mEncoder.releaseOutputBuffer(index, false);

        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.d(TAG, "Stop encoder and muxer, since the buffer has been marked with EOS");
            // send release msg
            mVideoTrackIndex = INVALID_INDEX;
            //eos
            signalStop(true);
        }

    }

    private void writeSampleData(int track, MediaCodec.BufferInfo info, ByteBuffer encodedData) {
        int isConfig = info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
        if (isConfig != 0) {
            //只有在INFO_OUTPUT_FORMAT_CHANGED的情况下，才会重新家Config传递给muxer
            // The codec config data was pulled out and fed to the muxer when we got
            // the INFO_OUTPUT_FORMAT_CHANGED status.
            //这种情况下，护理这个buffer数据
            // Ignore it.
            Log.d(TAG, "Ignoring BUFFER_FLAG_CODEC_CONFIG");
            info.size = 0;
        }

        int isEnd = info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM;
        boolean eos = isEnd != 0;
        //这种情况就忽略掉
        if (info.size == 0 && !eos) {
            Log.d(TAG, "info.size == 0, drop it.");
            encodedData = null;
        } else {
            if (info.presentationTimeUs != 0) {// maybe 0 if eos 如果是最后一个，就是0

                if (track == mVideoTrackIndex) {   //如果是视频
                    resetVideoPts(info);
                }
            }

            Log.d(TAG, "[" + Thread.currentThread().getId() + "] Got buffer, track=" + track
                    + ", info: size=" + info.size
                    + ", presentationTimeUs=" + info.presentationTimeUs);

            //回调
//            if (!eos && mCallback != null) {
//                mCallback.onRecording(buffer.presentationTimeUs);
//            }

            //需要根据info里面的值，来对他进行偏置
            if (encodedData != null) {
                encodedData.position(info.offset);
                encodedData.limit(info.offset + info.size);
                //最后将数据写到muxer内
                mMuxer.writeSampleData(track, encodedData, info);

                Log.i(TAG, "Sent " + info.size + " bytes to MediaMuxer on track " + track);
            }
        }
    }

    //记录上一次的offset，和偏移这一次的
    private void resetVideoPts(MediaCodec.BufferInfo info) {
        if (mVideoPtsOffset == 0) {
            mVideoPtsOffset = info.presentationTimeUs;
            info.presentationTimeUs = 0;
        } else {
            info.presentationTimeUs -= mVideoPtsOffset;
        }
    }

    public void stopEncoders() {
        mIsRunning.set(false);
        mPendingAudioEncoderBufferInfos.clear();
        mPendingAudioEncoderBufferIndices.clear();
        mPendingVideoEncoderBufferInfos.clear();
        mPendingVideoEncoderBufferIndices.clear();

        try {
            if (mEncoder != null) mEncoder.stop();
        } catch (IllegalStateException e) {
            // ignored
        }
    }

    public void release() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mProjectionCallback);
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        mVideoOutputFormat = null;
        mVideoTrackIndex = INVALID_INDEX;
        mMuxerStarted = false;

        if (mWorker != null) {
            mWorker.quitSafely();
            mWorker = null;
        }
        if (mEncoderInputSurface != null) {
            mEncoderInputSurface.release();
            mEncoderInputSurface = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        if (mMuxer != null) {
            try {
                mMuxer.stop();
                mMuxer.release();
            } catch (Exception e) {
                // ignored
            }
            mMuxer = null;
        }
        mHandler = null;
    }

    private void startMuxerIfReady() {
        if (mMuxerStarted || mVideoOutputFormat == null) {
            return;
        }

        //将视频的format添加到muxer中。得到音轨的index
        mVideoTrackIndex = mMuxer.addTrack(mVideoOutputFormat);

        mMuxer.start();
        mMuxerStarted = true;
        Log.i(TAG, "Started media muxer, videoIndex=" + mVideoTrackIndex);

        //下面对之前换成的buffer进行处理
        if (mPendingVideoEncoderBufferIndices.isEmpty() && mPendingAudioEncoderBufferIndices.isEmpty()) {
            return;
        }

        Log.i(TAG, "Mux pending video output buffers...");
        MediaCodec.BufferInfo info;
        while ((info = mPendingVideoEncoderBufferInfos.poll()) != null) {
            int index = mPendingVideoEncoderBufferIndices.poll();
            muxVideo(index, info);
        }
//        if (mAudioEncoder != null) {
//            while ((info = mPendingAudioEncoderBufferInfos.poll()) != null) {
//                int index = mPendingAudioEncoderBufferIndices.poll();
//                muxAudio(index, info);
//            }
//        }
        Log.i(TAG, "Mux pending video output buffers done.");
    }

    private void resetVideoOutputFormat(MediaFormat newFormat) {
        // should happen before receiving buffers, and should only happen once
        //必须要开始接受buffer之前接受格式。如果格式在中途改变了。则直接报错
        if (mVideoTrackIndex >= 0 || mMuxerStarted) {
            throw new IllegalStateException("output format already changed!");
        }
        Log.i(TAG, "Video output format changed.\n New format: " + newFormat.toString());
        //将格式保存下来
        mVideoOutputFormat = newFormat;
    }

    //发送signal来停止编码
    private void signalEndOfStream() {
        MediaCodec.BufferInfo eos = new MediaCodec.BufferInfo();
        ByteBuffer buffer = ByteBuffer.allocate(0);
        eos.set(0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        Log.i(TAG, "Signal EOS to muxer ");
        if (mVideoTrackIndex != INVALID_INDEX) {
            writeSampleData(mVideoTrackIndex, eos, buffer);
        }
        mVideoTrackIndex = INVALID_INDEX;
    }

    public void init() {
        if (mIsRunning.get() || mForceQuit.get()) {
            throw new IllegalStateException();
        }
        if (mMediaProjection == null) {
            throw new IllegalStateException("maybe release");
        }
        mIsRunning.set(true);

        mMediaProjection.registerCallback(mProjectionCallback, mHandler);
        try {
            // create muxer
            mMuxer = new MediaMuxer(mDstPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            // create encoder and input surface

            prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(TAG + "-display",
                mWidth, mHeight, mDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mEncoderInputSurface
                , null, null);
        Log.d(TAG, "created virtual display: " + mVirtualDisplay.getDisplay());

    }

    public void prepare() {
        //-1.创建MediaFormat
        MediaFormat format = createMediaFormat();
        //0.创建Encoder
        MediaCodec encoder = createEncoder();

        //0-1.设置CallBack。设置了CallBack就使用异步的方式来进行编码
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            //不设置Handler，则表示使用默认的Handler
            encoder.setCallback(mCallBack, null);
        }
        //1.对Encoder进行配置
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        //1-2.在创建完成时调用
        mEncoderInputSurface = encoder.createInputSurface();
        //2.开始encoder的编码
        encoder.start();

        mEncoder = encoder;
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
            encoder = MediaCodec.createByCodecName(codecName);
        } catch (IOException e) {
            e.printStackTrace();

        }
        return encoder;
    }

    public final void quit() {
        mForceQuit.set(true);
        if (!mIsRunning.get()) {
            release();
        } else {
            signalStop(false);
        }
    }

    private void signalStop(boolean stopWithEOS) {
        Message msg = Message.obtain(mHandler, MSG_STOP, stopWithEOS ? STOP_WITH_EOS : 0, 0);
        mHandler.sendMessageAtFrontOfQueue(msg);
    }

    @Override
    protected void finalize() throws Throwable {
        if (mMediaProjection != null) {
            Log.e(TAG, "release() not called!");
            release();
        }
    }

    //开始。运行在ThreadHandler内
    public void start() {
        mWorker = new HandlerThread(TAG);
        mWorker.start();
        mHandler = new CallbackHandler(mWorker.getLooper());
        mHandler.sendEmptyMessage(MSG_START);
    }

    private class CallbackHandler extends Handler {
        CallbackHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START:
                    try {
                        init();
//                        if (mCallback != null) {
//                            mCallback.onStart();
//                        }
                        break;
                    } catch (Exception e) {
                        msg.obj = e;
                        e.printStackTrace();
                    }
                case MSG_STOP:
                case MSG_ERROR:

                    stopEncoders();
                    if (msg.arg1 != STOP_WITH_EOS) signalEndOfStream();
//                    if (mCallback != null) {
//                        mCallback.onStop((Throwable) msg.obj);
//                    }
                    release();
                    break;
            }
        }
    }

}
