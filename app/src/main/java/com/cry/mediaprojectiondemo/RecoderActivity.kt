package com.cry.mediaprojectiondemo

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.cry.screenop.recorder.MediaCodecHelper
import com.cry.screenop.recorder.MediaCodecLogHelper
import com.cry.screenop.recorder.MediaCodecPermissionHelper
import kotlinx.android.synthetic.main.activity_recoder.*
import kotlinx.android.synthetic.main.content_recoder.*

class RecoderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recoder)
        setSupportActionBar(toolbar)


        //得到视频解码器的信息
        getVideoCodecName()

        //得到音频解码器的信息
        getAudioCodecName()

        fab.setOnClickListener {
            MediaCodecPermissionHelper
                    .requestMediaProjection(this@RecoderActivity)
                    .subscribe(
                            {
                                println(it)
                            },
                            { e -> Toast.makeText(this@RecoderActivity, e.message, Toast.LENGTH_SHORT).show() })
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
