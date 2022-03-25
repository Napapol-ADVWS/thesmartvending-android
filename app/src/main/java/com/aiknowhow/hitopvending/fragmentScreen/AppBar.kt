package com.aiknowhow.hitopvending.fragmentScreen

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aiknowhow.hitopvending.R
import com.aiknowhow.hitopvending.VendingService
import com.aiknowhow.hitopvending.data.GlobalVariable
import com.aiknowhow.hitopvending.mApp
import kotlinx.android.synthetic.main.appbar_fragment.view.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class AppBar : Fragment() {
    private val TAG = "AppBar"
    var currentDay = "0"
    private var signalLevel = 0
    private var pingFailedCount = 0
    private var pingTimerCounter = 0
    private var pingCheckURL = "1.1.1.1"

    private lateinit var ab_timer: CountDownTimer

    override fun onAttach(context: Context) {
        super.onAttach(context)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
//        val view: View = inflater.inflate(R.layout.appbar_fragment, container, false)
        val view: View = inflater.inflate(R.layout.appbar_fragment, container, false)

        if(mApp.isServerRegistered){
            view.onlineStatus.text = "Online"
        }else{
            view.onlineStatus.text = "Offline"
        }

        var idShow = "ID: " + mApp.kioskID
        view.machineID.text = idShow

        var appVersionShow = "Version: " + getString(R.string.app_version)
        view.appVersion.text = appVersionShow

        setupTimeView(view)

        updateSignalLevel(view, signalLevel)
        view.pingRes.text = "RT: checking"
        Handler().postDelayed({
            updatePing(view)
        }, 30000)

        initTimer(view)

        return view
    }

    @SuppressLint("SimpleDateFormat")
    private fun setupTimeView(v: View){
        var date = SimpleDateFormat("HH:mm")
        date.timeZone = TimeZone.getTimeZone("GMT+7")
        date = SimpleDateFormat("HH:mm")
        v.time_day.text = date.format(Date())

        date = SimpleDateFormat("EEEE")
        v.week_day.text = date.format(Date())

        date = SimpleDateFormat("dd-MM-yyyy")
        v.dmy.text = date.format(Date())


        date = SimpleDateFormat("dd")
        if(currentDay=="0"){
            currentDay = date.format(Date())
        }else{
            val newDate = date.format(Date())
            if(currentDay != newDate){
                mApp.updateAds = true
                currentDay = newDate
            }
        }
    }

    private fun updateStatus(v: View){
        if(mApp.isOnline){
//            v.onlineStatus.text = "Online" + " ${mApp.mqttReconnect}"
            v.onlineStatus.text = "Online"
        }else{
//            v.onlineStatus.text = "Offline" + " ${mApp.mqttReconnect}"
            v.onlineStatus.text = "Offline"
        }
    }

    private fun updateSignalLevel(v: View, level: Int){
        when (level) {
            0 -> v.signal_level.setImageResource(R.drawable.signal_level_0)
            1 -> v.signal_level.setImageResource(R.drawable.signal_level_1)
            2 -> v.signal_level.setImageResource(R.drawable.signal_level_2)
            3 -> v.signal_level.setImageResource(R.drawable.signal_level_3)
            4 -> v.signal_level.setImageResource(R.drawable.signal_level_4)
            else -> {
                v.signal_level.setImageResource(R.drawable.signal_level_0)
                Log.d(TAG, "Signal Level Invalid")
            }
        }
    }

    private fun pingCheck(url: String): String? {
        var str = ""
        val cmdPing = "/system/bin/ping -c 1 $url"

        try {
            val process = Runtime.getRuntime().exec(
                cmdPing
            )
            val reader = BufferedReader(
                InputStreamReader(
                    process.inputStream
                )
            )
            var i: Int
            val buffer = CharArray(4096)
            val output = StringBuffer()
            var op = arrayOfNulls<String>(64)
            var delay = arrayOfNulls<String>(8)
            while (reader.read(buffer).also { i = it } > 0) {
                output.append(buffer, 0, i)
            }
            reader.close()
//            Log.d(TAG, "CMD Output: $output")
            op = output.toString().split("\n").toTypedArray()
            if (op.size <= 1) {
                Log.d(TAG, "Output: $output")
                return "fail"
            }
            delay = op[1]!!.split("time=").toTypedArray()
            if (delay.size <= 1) {
                Log.d(TAG, "Output: $output")
                return "fail"
            }
            str = delay[1].toString()       // str = "55.66 ms"
            Log.d(TAG, "RT: $str")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return str
    }

    private fun updatePing(v: View) {
        val pingRes = pingCheck(pingCheckURL)
        v.pingRes.text = "RT: $pingRes"

        if (pingRes == "fail") {
            pingFailedCount++
            when (pingFailedCount) {
                1 -> {
                    Handler().postDelayed({
                        updatePing(v)
                    }, 5000)
                }
                in 2..5 -> signalLevel = 0
                else -> rebootDevice()
            }
        } else {
            pingFailedCount = 0
            val pingMS = pingRes.toString().split(" ").toTypedArray()[0].toFloat()
            signalLevel = when (pingMS) {
                in 0.00..99.99 -> 4
                in 100.00..299.99 -> 3
                in 300.00..599.99 -> 2
                in 600.00..1999.99 -> 1
                else -> 0
            }
        }

        updateSignalLevel(v, signalLevel)
    }

    private fun initTimer(v: View){
        ab_timer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            override fun onFinish() {
                updateStatus(v)
                setupTimeView(v)
                pingTimerCounter++
                if (pingTimerCounter >= 60) {
                    pingTimerCounter = 0
                    updatePing(v)
                }
                start()
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ab_timer.cancel()
    }

    private fun rebootDevice() {
        Runtime.getRuntime().exec("adb shell reboot").waitFor()
    }
}