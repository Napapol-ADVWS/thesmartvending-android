package com.aiknowhow.hitopvending.serverInterface

import android.content.Context
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import com.aiknowhow.hitopvending.*
import com.aiknowhow.hitopvending.data.*
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.API_LOG
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.LAST_BUY_LOG
import com.google.gson.Gson
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList

class MqttService(context: Context?) {

    private val TAG = "MqttService"
    private lateinit var mContext: Context
    private lateinit var watchdogTimer: CountDownTimer

    private val watchdogTimeSec = 60L  // Long

    private var mqttAndroidClient: MqttAndroidClient
    private val serverUri = "tcp://mqtt.advancevending.net:2883"

    private var userName = ""
    private var topicList = ArrayList<String>()

    private var topicCheckIn = ""
    private var topicApiCmd = ""
    private var topicCron = ""

    private var cron_id = 0

    private var service_status = true

    private var adsQueue = PriorityQueue<adsConfigData>()
    private var noticeQueue = PriorityQueue<adsConfigData>()
    private var qrResultQueue = PriorityQueue<String>()

    //CMD to publish
    private val CMD_GET_INVENTORY = "get_inventory"
    private val CMD_GET_ADS = "get_ads"

    private var mqttStatus = false
    private var isWatchdogInit = false
    private var isWatchdogStart = false

    // CMD from Server
    private val CMD_SETUP_MENU = "setup_inventory"
    private val CMD_REFILL = "refill"
    private val CMD_RESTART = "restart_app"
    private val CMD_GET_API_LOG = "get_last_apilog"
    private val CMD_GET_LOG = "get_last_log"
    private val CMD_GET_LAST_TX = "get_last_buy"
    private val CMD_CLOSE_SERVICE = "closeservice"
    private val CMD_OPEN_SERVICE = "openservice"
    private val CMD_SETUP_TOKEN = "setup_token"
    private val CMD_PAYMENT_RESULT = "qr_payment_result"
    private val CMD_ADS_SETUP = "setup_ads"
    private val CMD_SET_PAYMENT_METHOD = "setup_qr_payment_method"
    private val CMD_CLOSE_CASH = "close_cash_payment"
    private val CMD_OPEN_CASH = "open_cash_payment"
    private val CMD_CHECKIN_RES = "checkin_res"

    fun connect(user: String, topic: ArrayList<String>) {
        userName = user
        topicList = topic
        val mqttConnectOptions = MqttConnectOptions()
        mqttConnectOptions.isAutomaticReconnect = true
        mqttConnectOptions.isCleanSession = false
        mqttConnectOptions.userName = user
//        mqttConnectOptions.password = password.toCharArray()
        try {
            mqttAndroidClient.connect(mqttConnectOptions, mContext, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    val disconnectedBufferOptions =
                        DisconnectedBufferOptions()
                    disconnectedBufferOptions.isBufferEnabled = true
                    disconnectedBufferOptions.bufferSize = 100
                    disconnectedBufferOptions.isPersistBuffer = false
                    disconnectedBufferOptions.isDeleteOldestMessages = false
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
                    subscribeTopic(topic)
                    mqttStatus = true
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    Log.w(
                        TAG,
                        "Failed to connect to: $serverUri$exception"
                    )
                    exception.printStackTrace()
                    onConnectFail()
                }
            })
//            mqttAndroidClient.connect(mqttConnectOptions, mContext,null)
        } catch (ex: MqttException) {
            ex.printStackTrace()
        }
    }

    private fun onConnectFail() {
//        Handler().postDelayed({
//            connect(userName, topicList)
//        }, 60000)
        mApp.isOnline = false
        mApp.isServerRegistered = false
//        mApp.mqttReconnect++
        disconnect()
    }

    private fun disconnect() {
        try {
            mqttAndroidClient.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Disconnected")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    Log.d(TAG, "Failed to disconnect")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    private fun subscribeTopic(allTopic: ArrayList<String>) {
        allTopic.forEach {
            subscribeToTopic(it)
        }
    }

    private fun subscribeToTopic(topic: String) {
        try {
            mqttAndroidClient.subscribe(topic, 2, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    Log.w(TAG, "Topic($topic) Subscribed!")
                }

                override fun onFailure(
                    asyncActionToken: IMqttToken,
                    exception: Throwable
                ) {
                    Log.w(TAG, "Topic($topic) Subscribed fail!")
                }
            })
        } catch (ex: MqttException) {
            System.err.println("Exception whilst subscribing")
            ex.printStackTrace()
        }
    }

    fun getAds(): adsConfigData? {
        if (adsQueue.isEmpty()) {
            return null
        }
        return try {
            adsQueue.poll()
        }catch (e:Exception){
            e.printStackTrace()
            null
        }
    }

    fun clearAds() {
        adsQueue.clear()
    }

    fun getNotice(): adsConfigData? {
        if (noticeQueue.isEmpty()) {
            return null
        }
        return try {
            noticeQueue.poll()
        }catch (e:Exception){
            e.printStackTrace()
            null
        }
    }

    fun clearNotice() {
        noticeQueue.clear()
    }

    fun getPaymentResult(): String? {
        return qrResultQueue.poll()
    }

    fun clearPaymentResult() {
        qrResultQueue.clear()
    }

    fun getMqttStatus(): Boolean {
        return mqttStatus
    }

    fun setupTopic(topicList: ArrayList<String>) {
        topicList.forEach {
            when {
                it.contains("checkin") -> {
                    topicCheckIn = it
                }
                it.contains("server/api/command") -> {
                    topicApiCmd = it
                }
                it.contains("cron/success") -> {
                    topicCron = it
                }
            }
        }
    }

    fun publishRequestProduct() {
        if (topicApiCmd.isNotEmpty()) {
            val payload = HashMap<String, Any>()
//            payload["KioskID"] = mApp.kioskID.toInt()
            payload["cmd"] = CMD_GET_INVENTORY
            val gson = Gson()
            MqttService.publish(topicApiCmd, gson.toJson(payload).toString())
        }
    }

    fun publishGetAds() {
        if (topicApiCmd.isNotEmpty()) {
            val payload = HashMap<String, Any>()
//            payload["KioskID"] = mApp.kioskID.toInt()
            payload["cmd"] = CMD_GET_ADS
            val gson = Gson()
            MqttService.publish(topicApiCmd, gson.toJson(payload).toString())
        }
    }

    fun publishCheckIn() {
        if (!mqttStatus) {
            Log.d(TAG, "Can't CheckIn MQTT Not Ready")
            return
        }
        if (topicCheckIn.isNotEmpty()) {
            val payload = HashMap<String, Any>()
            if(MdbService.getMDBStatus()){
                val tubeInfo = MdbService.getCashInfo()
                val jsonObj = JSONObject(tubeInfo)
                val mapTube = jsonObj.toMap()
                payload["coinStack"] = mapTube
            }
            payload["boardStatus"] = VendingService.isReady()
            payload["mdbStatus"] = MdbService.getMDBStatus()
            val gson = Gson()
            MqttService.publish(topicCheckIn, gson.toJson(payload).toString())
            startWatchdog()
        }
    }

    fun getServiceStatus(): Boolean {
        return service_status
    }

    fun publishCron(data: String) {
        if (topicCron.isNotEmpty()) {
            val payload = HashMap<String, Any>()
            payload["cronID"] = cron_id
            if (data.isNotEmpty()) {
                val jsonObj = JSONObject(data)
                val map = jsonObj.toMap()
                payload["data"] = map
            }
            val gson = Gson()
            MqttService.publish(topicCron, gson.toJson(payload).toString())
        }
    }

    private fun publish(topic: String, msg: String) {
        Log.w(TAG, "PublishTopic($topic), MSG($msg)")
        try {
            val encodedPayload = msg.toByteArray(charset("UTF-8"))
            val message = MqttMessage(encodedPayload)
            message.qos = 2
            message.isRetained = false
            mqttAndroidClient.publish(topic, message)
            Log.d(TAG, "Publish success")
        } catch (e: Exception) {
            // Give Callback on error here
            Log.d(TAG, "Error $e")
        } catch (e: MqttException) {
            // Give Callback on error here
            Log.d(TAG, "Error $e")
        }
    }

    private fun responseApiLog(){
        val lastApiLog = FileManager().readFile(mContext, API_LOG)
        publishCron(lastApiLog)
    }

    private fun responseLastBuyLog(){
        val lastBuyLog = FileManager().readFile(mContext, LAST_BUY_LOG)
        publishCron(lastBuyLog)
    }


    init {
        if (context != null) {
            mContext = context
        }

        val persistence = MemoryPersistence()
        initWatchdog(watchdogTimeSec)
        mqttAndroidClient = MqttAndroidClient(context, serverUri, mApp.kioskID, persistence)
        mqttAndroidClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                Log.w(TAG, s)
                mqttStatus = true
            }

            override fun connectionLost(throwable: Throwable?) {
                mqttStatus = false
            }

            @Throws(Exception::class)
            override fun messageArrived(
                topic: String,
                mqttMessage: MqttMessage
            ) {
                Log.w(TAG, "Received Topic($topic) , Msg(${mqttMessage})")
                val jsonObj = JSONObject(mqttMessage.toString())
                val map = jsonObj.toMap()
                val gson = Gson()
                when (map["cmd"]) {
                    CMD_SETUP_MENU -> {
                        val responseMenu =
                            gson.fromJson(mqttMessage.toString(), mqttCmdSetupMenu::class.java)
                        productList.setupMenu(responseMenu.inventory)
                        Log.w(TAG, "Cmd(${responseMenu.cmd}) , Product(${responseMenu.inventory})")
                    }
                    CMD_REFILL -> {
                        cron_id = map["cronID"].toString().toInt()
                        publishCron("")
                    }
                    CMD_RESTART -> {
                        cron_id = map["cronID"].toString().toInt()
                        publishCron("")
                        mApp.getRestart = true
                    }
                    CMD_GET_API_LOG -> {
                        cron_id = map["cronID"].toString().toInt()
                        responseApiLog()
                    }
                    CMD_GET_LOG -> {
                        cron_id = map["cronID"].toString().toInt()
                        publishCron("")
                    }
                    CMD_GET_LAST_TX -> {
                        cron_id = map["cronID"].toString().toInt()
                        responseLastBuyLog()
                    }
                    CMD_CLOSE_SERVICE -> {
                        cron_id = map["cronID"].toString().toInt()
                        service_status = false
                        publishCron("")
                    }
                    CMD_OPEN_SERVICE -> {
                        cron_id = map["cronID"].toString().toInt()
                        service_status = true
                        publishCron("")
                    }
                    CMD_SETUP_TOKEN -> {
                        val newToken = map["token"].toString()
                        if (newToken.isNotEmpty()) {
                            mApp.serverToken = newToken
                        }
                    }
                    CMD_PAYMENT_RESULT -> {
                        val result = map["result"].toString()
                        if (result.isNotEmpty()) {
                            qrResultQueue.add(gson.toJson(map["result"]))
                        }
                    }
                    CMD_ADS_SETUP -> {
                        val adsConfig = gson.fromJson(mqttMessage.toString(), adsSetup::class.java)
                        if (adsConfig.config.type == "ads") {
                            adsQueue.add(adsConfig.config)
                        } else if (adsConfig.config.type == "notice") {
                            noticeQueue.add(adsConfig.config)
                        }
                    }
                    CMD_SET_PAYMENT_METHOD -> {
                        val methodResponse =
                            gson.fromJson(mqttMessage.toString(), qrMethodResponseMqtt::class.java)
                        mApp.paymentMethod = methodResponse.method
                        Log.w(
                            TAG,
                            "Cmd(${methodResponse.cmd}) , QrMethod(${methodResponse.method})"
                        )
                    }
                    CMD_CLOSE_CASH -> {
                        MdbService.setCashStatus(false)
                    }
                    CMD_OPEN_CASH -> {
                        MdbService.setCashStatus(true)
                    }
                    CMD_CHECKIN_RES -> {
                        Log.d(TAG, "Checkin Res Complete")
                        stopWatchdog()
                    }
                    else -> {
                        Log.w(TAG, "Cmd(${map["cmd"]})")
                    }
                }
            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                Log.w(TAG, "delivery complete")
            }
        })
    }

    private fun initWatchdog(seconds: Long) {
        if (isWatchdogInit) {
            Log.d(TAG, "Watchdog Initial Done")
            return
        } else {
            Log.d(TAG, "Watchdog Initial")
        }
        isWatchdogInit = true
        watchdogTimer = object : CountDownTimer(seconds * 1000, 1000) {

            override fun onTick(p0: Long) {
                Log.d(TAG, "Watchdog Timer: ${p0 / 1000}")
            }

            override fun onFinish() {
                Log.d(TAG, "Watchdog Timeout")
//                mApp.getRestart = true
                mApp.isOnline = false
                mApp.isServerRegistered = false
//                mApp.mqttReconnect++
                stopWatchdog()
            }
        }

    }

    private fun startWatchdog() {
        if (!mqttStatus) {
            Log.d(TAG, "Watchdog Can't Start MQTT Not Ready")
            return
        }
        if (isWatchdogStart) {
            Log.d(TAG, "Watchdog Started")
            return
        } else {
            Log.d(TAG, "Watchdog Starting...")
        }
        isWatchdogStart = true
        watchdogTimer.start()
    }

    private fun stopWatchdog() {
        Log.d(TAG, "Watchdog Stop")
        isWatchdogStart = false
        watchdogTimer.cancel()
    }
}

fun JSONObject.toMap(): Map<String, *> = keys().asSequence().associateWith {
    when (val value = this[it]) {
        is JSONArray -> {
            val map = (0 until value.length()).associate { Pair(it.toString(), value[it]) }
            JSONObject(map).toMap().values.toList()
        }
        is JSONObject -> value.toMap()
        JSONObject.NULL -> null
        else -> value
    }
}
