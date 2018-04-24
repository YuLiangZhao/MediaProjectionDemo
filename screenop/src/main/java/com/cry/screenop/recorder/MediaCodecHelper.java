package com.cry.screenop.recorder;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;

import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;

import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static android.media.MediaFormat.MIMETYPE_VIDEO_AVC;

/**
 * MediaCodec 工具类
 * Created by a2957 on 4/24/2018.
 */
public class MediaCodecHelper {
    //两种H.264
    public static final String VIDEO_AVC = MIMETYPE_VIDEO_AVC; // H.264 Advanced Video Coding
    public static final String AUDIO_AAC = MIMETYPE_AUDIO_AAC; // H.264 Advanced Audio Coding

    //得到制定的MimeType的Codec
    public static Single<List<MediaCodecInfo>> getAdaptiveEncoderCodec(String mimeType) {
        return Observable
                .fromArray(getMediaCodecInfos())
                .filter(mediaCodecInfo -> {
                    if (mediaCodecInfo.isEncoder()) {
                        try {
                            MediaCodecInfo.CodecCapabilities capabilitiesForType = mediaCodecInfo.getCapabilitiesForType(mimeType);
                            return capabilitiesForType != null;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                    return false;
                })
                .toList()
                ;
    }

    private static MediaCodecInfo[] getMediaCodecInfos() {
        //获取当前可以支持的MediaCodec
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        //得到所有CodecInfos
        return mediaCodecList.getCodecInfos();
    }

    //将音频解码器的能力打印出来
    public static Observable<String> printAudioCodecCap(List<MediaCodecInfo> mediaCodecInfos, String mimeType) {
        return Observable
                .just(mediaCodecInfos)
                .map(infoList -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("MIMETYPE_AUDIO_AAC INFO:")
                            .append("\n").append("Size of infoList ")
                            .append(infoList.size())
                            .append("\n");
                    for (MediaCodecInfo it : infoList) {
                        sb.append(it.getName()).append("\n");
                        //获取编码器的能力
                        MediaCodecInfo.CodecCapabilities codecCapabilities = it.getCapabilitiesForType(mimeType);
                        MediaCodecInfo.AudioCapabilities audioCapabilities = codecCapabilities.getAudioCapabilities();
                        MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();

                        printVideoCap(sb, videoCapabilities);
                        printColorFormat(sb, codecCapabilities);


                        printAudioCap(sb, audioCapabilities);
                    }
                    return sb.toString();
                });
    }

    private static void printColorFormat(StringBuilder sb, MediaCodecInfo.CodecCapabilities codecCapabilities) {
        if (codecCapabilities == null) {
            return;
        }
        //colorFormats & profileLevels
        sb.append("colorFormats INFO:").append("\n");
        int[] colorFormats = codecCapabilities.colorFormats;
        for (int colorFormat : colorFormats) {
            sb.append("colorFormat : ").append(MediaCodecReflectFieldName.toHumanReadable(colorFormat)).append("\n");
        }
    }

    //将视频解码器的能力打印出来
    public static Observable<String> printVideoCodecCap(List<MediaCodecInfo> mediaCodecInfos, String mimeType) {
        return Observable
                .just(mediaCodecInfos)
                .map(infoList -> {
                    StringBuilder sb = new StringBuilder();

                    sb.append("MIME_TYPE_VIDEO_AVC INFO:")
                            .append("\n").append("Size of infoList ")
                            .append(infoList.size())
                            .append("\n");

                    for (MediaCodecInfo it : infoList) {
                        sb.append(it.getName()).append("\n");
                        //获取编码器的能力
                        MediaCodecInfo.CodecCapabilities codecCapabilities = it.getCapabilitiesForType(mimeType);
                        MediaCodecInfo.VideoCapabilities videoCapabilities = codecCapabilities.getVideoCapabilities();
                        MediaCodecInfo.AudioCapabilities audioCapabilities = codecCapabilities.getAudioCapabilities();
                        printVideoCap(sb, videoCapabilities);

                        printProfileLevel(sb, codecCapabilities);
                        //colorFormats & profileLevels
                        printColorFormat(sb, codecCapabilities);

                        printAudioCap(sb, audioCapabilities);

                    }
                    return sb.toString();
                });
    }

    private static void printProfileLevel(StringBuilder sb, MediaCodecInfo.CodecCapabilities codecCapabilities) {
        if (codecCapabilities == null) {
            return;
        }
        //profileLevels
        sb.append("profileLevels INFO:").append("\n");
        MediaCodecInfo.CodecProfileLevel[] profileLevels = codecCapabilities.profileLevels;
        for (MediaCodecInfo.CodecProfileLevel profileLevel : profileLevels) {
            sb.append("profileLevel : ").append(MediaCodecReflectFieldName.avcProfileLevelToString(profileLevel)).append("\n");
        }
    }

    /**
     * 音频编码器的能力
     * bitrate
     * maxInoutChannelCount
     * SampleRate
     */
    private static void printAudioCap(StringBuilder sb, MediaCodecInfo.AudioCapabilities audioCapabilities) {
        if (audioCapabilities == null) {
            return;
        }
        sb.append("AudioCap INFO:").append("\n");

        sb.append("bitrateRange=").append(audioCapabilities.getBitrateRange()).append("\n");
        sb.append("maxInputChannelCount=").append(audioCapabilities.getMaxInputChannelCount()).append("\n");
        sb.append("supportedSampleRateRanges=").append(Arrays.toString(audioCapabilities.getSupportedSampleRates())).append("\n");
        sb.append("audioCapabilities=").append(audioCapabilities.toString()).append("\n");
    }

    /**
     * 视频编码器的能力包括
     * bitrate
     * FrameRate
     * Height
     * Width
     */
    private static void printVideoCap(StringBuilder sb, MediaCodecInfo.VideoCapabilities videoCapabilities) {
        if (videoCapabilities == null) {
            return;
        }
        sb.append("VideoCap INFO:").append("\n");

        sb.append("bitrateRange=").append(videoCapabilities.getBitrateRange()).append("\n");
        sb.append("supportedFrameRates=").append(videoCapabilities.getBitrateRange()).append("\n");
        sb.append("supportedHeights=").append(videoCapabilities.getSupportedHeights()).append("\n");
        sb.append("supportedWidths=").append(videoCapabilities.getSupportedWidths()).append("\n");
        sb.append("heightAlignment=").append(videoCapabilities.getHeightAlignment()).append("\n");
        sb.append("widthAlignment=").append(videoCapabilities.getWidthAlignment()).append("\n");
    }


}
