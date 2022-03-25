package com.aiknowhow.hitopvending.fragmentScreen

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aiknowhow.hitopvending.*
import com.aiknowhow.hitopvending.data.GlobalVariable
import kotlinx.android.synthetic.main.setting_fragment.view.*

class CloseService:Fragment() {
    private val TAG = "CloseService"
    private var mCallback: Callback? = null
    lateinit var mContext: Context

    private lateinit var timer: CountDownTimer
    private var cdSyncTime:Long = 10 // Minute
    private var cdSyncTimer = cdSyncTime*60 // Sec

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
        mCallback = try {
            context as Callback
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement Callback")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        val view: View = inflater.inflate(R.layout.close_service, container, false)

        initTimer(view)
        return view
    }

    override fun onDestroyView() {
        timer.cancel()
        super.onDestroyView()
    }

    private fun initTimer(v: View) {
        timer = object : CountDownTimer(cdSyncTimer*1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "${millisUntilFinished / 1000} left")
                if(MqttService.getServiceStatus()){
                    mCallback!!.openService()
                }
            }

            override fun onFinish() {
                if(mApp.isServerRegistered){
                    MqttService.publishCheckIn()
                }
            }
        }.start()
    }
}