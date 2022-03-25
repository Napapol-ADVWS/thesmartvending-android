package com.aiknowhow.hitopvending.data

import android.app.Application

class GlobalVariable: Application() {

    var infoGet = false
    var androidID = ""
    var imei = ""

    var kioskID = ""
    var registerKey = ""
    var serverToken = ""
    var tokenType = "Bearer"

    var mqttPublish = ArrayList<String>()
    var mqttSubscribe = ArrayList<String>()

//    var mqttReconnect = 0

    var paymentMethod = ArrayList<qrMethodData>()

    var textAds = ""

    var vendingModel = 1

    var dataChangedFlag = false
    var updateAds = false

    lateinit var menuSelected:menuData
    var txid = ""

    var pageId = 2
    var isServerRegistered = false
    var isOnline = false

    var adsData=ArrayList<adsVideoData>()
    var adsState = false

    var noticeData=ArrayList<noticeData>()
    var noticeState = false

    var getRestart = false

    companion object {
        /////// Page Key
        const val PAGE_SETTING_KEY = 1
        const val PAGE_MAIN = 2
        const val PAGE_POPUP = 3
        const val PAGE_CLOSE = 4

        const val PREFS_KEY_KIOSK_ID = "kiosk_id"
        const val DEFAULT_KIOSK_ID = ""
        const val PREFS_KEY_REGISTER_KEY = "server_register_key"
        const val DEFAULT_REGISTER_KEY = ""
        const val PREFS_KEY_SERVICE = "service_message_key"
        const val DEFAULT_SERVICE = ""
        const val PREFS_VENDING_MODEL = "vending_model"
        const val DEFAULT_VENDING_MODEL = 1
        const val PREFS_ROW1_TYPE = "row1_type_key"
        const val PREFS_ROW2_TYPE = "row2_type_key"
        const val PREFS_ROW3_TYPE = "row3_type_key"
        const val PREFS_ROW4_TYPE = "row4_type_key"
        const val PREFS_ROW5_TYPE = "row5_type_key"
        const val PREFS_ROW6_TYPE = "row6_type_key"
        const val PREFS_ROW7_TYPE = "row7_type_key"
        const val PREFS_ROW8_TYPE = "row8_type_key"
        const val PREFS_ROW9_TYPE = "row9_type_key"
        const val PREFS_ROWA_TYPE = "rowA_type_key"
        // 0 = Lift
        const val DEFAULT_ROW_TYPE = 0

        const val API_LOG = "/apiLog.txt"
        const val LAST_BUY_LOG = "/lastBuyLog.txt"
    }
}