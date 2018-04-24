package com.cry.mediaprojectiondemo

import android.media.MediaCodecInfo
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.cry.screenop.recorder.MediaCodecHelper
import com.cry.screenop.recorder.MediaCodecHelper.*

import kotlinx.android.synthetic.main.activity_recoder.*
import kotlinx.android.synthetic.main.content_recoder.*
import java.util.*

class RecoderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recoder)
        setSupportActionBar(toolbar)


        //得到视频解码器的信息
        getVideoCodecName()

        //得到音频解码器的信息
        getAudioCodecName()

    }

    private fun getAudioCodecName() {
        val mimeType = MediaCodecHelper.AUDIO_AAC
        getAdaptiveEncoderCodec(mimeType)
                .toObservable()
                .flatMap { printAudioCodecCap(it, mimeType) }
                .subscribe(
                        { tv_a_codec.text = it },
                        { e -> Toast.makeText(this@RecoderActivity, e.message, Toast.LENGTH_SHORT).show() })
    }

    private fun getVideoCodecName() {
        val mimeType = MediaCodecHelper.VIDEO_AVC
        getAdaptiveEncoderCodec(mimeType)
                .toObservable()
                .flatMap { printVideoCodecCap(it, mimeType) }
                .subscribe(
                        { tv_v_codec.text = it },
                        { e -> Toast.makeText(this@RecoderActivity, e.message, Toast.LENGTH_SHORT).show() })
    }

}
