package com.aiknowhow.hitopvending.fragmentScreen

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import com.aiknowhow.hitopvending.MqttService
import com.aiknowhow.hitopvending.R
import com.aiknowhow.hitopvending.data.adsVideoData
import com.aiknowhow.hitopvending.data.noticeData
import com.aiknowhow.hitopvending.mApp
import kotlinx.android.synthetic.main.ads_text_fragment.view.*

class AdsTextFragment : Fragment() {
    private val TAG = "ADS_Text_Fragment"

    private lateinit var ads_timer: CountDownTimer
    private var cdTextRun: Long = 60

    private var txtIndex = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.ads_text_fragment, container, false)

        val marquee = AnimationUtils.loadAnimation(context, R.anim.text_marquee);
        view.adsTextShow.startAnimation(marquee)
        initTimer(view)
        setupNotice(view)

        return view
    }

    private fun setupNotice(v: View){
        if(mApp.noticeState && mApp.noticeData.isNotEmpty()){
            v.adsTextShow.text = mApp.noticeData[txtIndex].msg
        }else{
            v.adsTextShow.text = mApp.textAds
        }
        txtIndex++
        if(txtIndex>= mApp.noticeData.size){
            txtIndex = 0
        }
    }

    private fun initTimer(v: View){
        ads_timer = object : CountDownTimer(cdTextRun*1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val noticeUpdate = MqttService.getNotice()
                if(noticeUpdate != null){
                    mApp.noticeState = noticeUpdate.state == "on"
                    if(mApp.noticeState){
                        mApp.noticeData = noticeUpdate.notice
                    }
                    setupNotice(v)
                }
                if(mApp.updateAds){

                    mApp.updateAds = false
                }
            }

            override fun onFinish() {
                setupNotice(v)
                start()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ads_timer.cancel()
    }

}