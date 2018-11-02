package com.cry.mediaprojectiondemo

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.cry.mediaprojectiondemo.R.id.*
import com.cry.screenop.recorder.MediaCodecHelper
import com.cry.screenop.recorder.MediaCodecLogHelper
import com.cry.screenop.recorder.MediaCodecPermissionHelper
import com.cry.screenop.recorder.encoder.VideoEncoder
import kotlinx.android.synthetic.main.activity_recoder.*
import kotlinx.android.synthetic.main.content_recoder.*
import java.io.File

class RecoderActivity : AppCompatActivity() {

    private var isStart: Boolean = false
    private var videoEncoder: VideoEncoder? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recoder)
        setSupportActionBar(toolbar)


        //得到视频解码器的信息
        getVideoCodecName()

        //得到音频解码器的信息
        getAudioCodecName()

        fab.setOnClickListener {

            if (isStart) {
                videoEncoder!!.quit()
                Toast.makeText(this@RecoderActivity, "End!", Toast.LENGTH_SHORT).show()
            } else {
                MediaCodecPermissionHelper
                        .requestMediaProjection(this@RecoderActivity)
                        .subscribe(
                                {
                                    println(it)
                                    var file = File(Environment.getExternalStorageDirectory(), "mediaProjection/1.mp4")
                                    file.parentFile.mkdirs()

                                    val dest = file.absolutePath
                                    videoEncoder = VideoEncoder(
                                            480, 720, 1,
                                            dest,
                                            480, 720, 800000, 15, 1,
                                            "OMX.google.h264.encoder",
//                                            "OMX.MTK.VIDEO.ENCODER.AVC",
                                            "video/avc",
                                            null
                                            , it
                                    )
                                    videoEncoder!!.start()
                                    Toast.makeText(this@RecoderActivity, "start!", Toast.LENGTH_SHORT).show()
                                    isStart = true
                                },
                                { e -> Toast.makeText(this@RecoderActivity, e.message, Toast.LENGTH_SHORT).show() })
            }

        }

    }

    private fun getAudioCodecName() {
        val mimeType = MediaCodecHelper.AUDIO_AAC
        MediaCodecHelper.getAdaptiveEncoderCodec(mimeType)
                .toObservable()
                .flatMap { MediaCodecLogHelper.printAudioCodecCap(it, mimeType) }
                .subscribe(
                        { tv_a_codec.text = it },
                        { e -> Toast.makeText(this@RecoderActivity, e.message, Toast.LENGTH_SHORT).show() })
    }

    private fun getVideoCodecName() {
        val mimeType = MediaCodecHelper.VIDEO_AVC
        MediaCodecHelper.getAdaptiveEncoderCodec(mimeType)
                .toObservable()
                .flatMap { MediaCodecLogHelper.printVideoCodecCap(it, mimeType) }
                .subscribe(
                        { tv_v_codec.text = it },
                        { e -> Toast.makeText(this@RecoderActivity, e.message, Toast.LENGTH_SHORT).show() })
    }

}
