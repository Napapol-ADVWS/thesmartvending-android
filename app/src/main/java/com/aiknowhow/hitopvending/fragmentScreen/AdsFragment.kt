package com.aiknowhow.hitopvending.fragmentScreen

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aiknowhow.hitopvending.MqttService
import com.aiknowhow.hitopvending.R
import com.aiknowhow.hitopvending.mApp
import com.aiknowhow.hitopvending.videoProxy
import kotlinx.android.synthetic.main.ads_fragment.view.*
import java.io.IOException

class AdsFragment : Fragment() {
    private val TAG = "ADS_Fragment"

    private lateinit var ads_timer: CountDownTimer

    private val DEFAULT_ADS = "https://advancevending.net/videos/Advance_Vending.mp4"

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.ads_fragment, container, false)

        showDefaultVideo(view)
        initTimer(view)

//        view.adsVideo.visibility = View.INVISIBLE

        return view
    }

    private fun showDefaultVideo(v: View){
        val urlVideo = DEFAULT_ADS
        val proxyUrl = videoProxy.getProxyUrl(urlVideo)
        v.adsVideo.setVideoPath(proxyUrl)
        v.adsVideo.start()
        v.adsVideo.setOnPreparedListener { mp -> mp.isLooping = true }
        v.adsVideo.setOnCompletionListener {
                mp -> mp.isLooping = true
        }
        v.adsVideo.visibility = View.VISIBLE
        v.adsText.visibility = View.GONE
    }

    private fun setupVideoUrl(v: View){
        var currentVideo = 0
        if(mApp.adsState){
            if(!mApp.adsData.isNullOrEmpty()){
                v.adsVideo.requestFocus()
                var proxyUrl = videoProxy.getProxyUrl(mApp.adsData[currentVideo].url)
                v.adsVideo.setVideoPath(proxyUrl)
                v.adsVideo.setOnPreparedListener {mediaPlayer ->
                    mediaPlayer.setVolume(1f,1f)
                    val videoRatio = mediaPlayer.videoWidth / mediaPlayer.videoHeight.toFloat()
                    val screenRatio = v.adsVideo.width / v.adsVideo.height.toFloat()
                    val scaleX = videoRatio / screenRatio
                    if (scaleX >= 1f) {
                        v.adsVideo.scaleX = scaleX
                    } else {
                        v.adsVideo.scaleY = 1f / scaleX
                    }
                    v.adsVideo.start()
                }
                v.adsVideo.setOnCompletionListener {
                    try {
                        currentVideo++
                        if(currentVideo>=mApp.adsData.size) currentVideo=0

                        proxyUrl = videoProxy.getProxyUrl(mApp.adsData[currentVideo].url)
                        v.adsVideo.setVideoPath(proxyUrl)
                    }catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }else{
            showDefaultVideo(v)
//            val urlVideo = DEFAULT_ADS
//            val proxyUrl = videoProxy.getProxyUrl(urlVideo)
//            v.adsVideo.setVideoPath(proxyUrl)
//            v.adsVideo.start()
//            v.adsVideo.setOnPreparedListener { mp -> mp.isLooping = true }
//            v.adsVideo.setOnCompletionListener {
//                    mp -> mp.isLooping = true
//            }
//            v.adsVideo.visibility = View.VISIBLE
//            v.adsText.visibility = View.GONE
        }
    }

    private fun initTimer(v: View){
        ads_timer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                val adsUpdate = MqttService.getAds()
                if(adsUpdate != null){
                    mApp.adsState = adsUpdate.state == "on"
                    if(mApp.adsState){
                        mApp.adsData = adsUpdate.ads
                    }
                    setupVideoUrl(v)
                }
                if(mApp.updateAds){
//                    getAdsVideo(v)
                    mApp.updateAds = false
                }
                start()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ads_timer.cancel()
    }

}