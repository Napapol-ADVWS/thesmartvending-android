package com.aiknowhow.hitopvending

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.aiknowhow.hitopvending.cash.MdbService
import com.aiknowhow.hitopvending.data.GlobalVariable
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.DEFAULT_KIOSK_ID
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.DEFAULT_REGISTER_KEY
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.DEFAULT_ROW_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.DEFAULT_SERVICE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.DEFAULT_VENDING_MODEL
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PAGE_CLOSE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PAGE_MAIN
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PAGE_POPUP
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PAGE_SETTING_KEY
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_KEY_KIOSK_ID
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_KEY_REGISTER_KEY
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_KEY_SERVICE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_VENDING_MODEL
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW1_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW2_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW3_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW4_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW5_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW6_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW7_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW8_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROW9_TYPE
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PREFS_ROWA_TYPE
import com.aiknowhow.hitopvending.fragmentScreen.*
import com.aiknowhow.hitopvending.serverInterface.MqttService
import com.aiknowhow.hitopvending.serverInterface.VideoCache
import com.aiknowhow.hitopvending.vendingService.ProductSlot
import com.aiknowhow.hitopvending.vendingService.VendingService
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import kotlin.system.exitProcess


var mApp = GlobalVariable()
var videoProxy = VideoCache()
var MdbService = MdbService()
var productList = ProductSlot()
lateinit var MqttService: MqttService
var VendingService = VendingService()
lateinit var sharedPref: SharedPreferences

//class MainActivity : AppCompatActivity(), Callback, TransactionListener, DevicesInitListener  {
class MainActivity : AppCompatActivity(), Callback {
    val TAG = "MainActivity"
    var MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 0
    lateinit var bodyFragment: Fragment

    private var waitRebootActive = false

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)
        getDeviceInfo()
        sharedPref = getPreferences(Context.MODE_PRIVATE)
        loadSharedPref()
        videoProxy.getProxy(applicationContext)
        frameSetting.visibility = View.GONE
        bodyFragment = ShelfFragment()
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.appbarFrame, AppBar())
                .add(R.id.adsFrame, AdsFragment())
                .add(R.id.bodyFrame, bodyFragment)
                .add(R.id.adsTextFrame, AdsTextFragment())
                .commit()
            mApp.pageId = PAGE_MAIN
        }
        MqttService = MqttService(applicationContext)
        MdbService.openConnect()
        VendingService.open(applicationContext)
    }

    @SuppressLint("NewApi")
    private fun getDeviceInfo() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                MY_PERMISSIONS_REQUEST_READ_PHONE_STATE
            )
            return
        } else if(ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                MY_PERMISSIONS_REQUEST_READ_PHONE_STATE
            )
            return
        } else {
            createMain()
        }
    }

    @SuppressLint("NewApi")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_PHONE_STATE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    createMain()

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    finish()
                }

                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
                finish()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    private fun createMain() {
        delayGetImei()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    private fun getImei(){
        try{
//            val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            mApp.androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            mApp.imei =getIMEIDeviceId(applicationContext)
//            mApp.imei = tm.getImei(0)
            mApp.infoGet = true
        }catch (e:Exception){
            e.printStackTrace()
            delayGetImei()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun delayGetImei() {
        Handler().postDelayed({
            getImei()
        }, 30*1000)
    }
    @SuppressLint("HardwareIds")
    fun getIMEIDeviceId(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        } else {
            val mTelephony = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    return ""
                }
            }
            if (mTelephony.deviceId != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    mTelephony.imei
                } else {
                    mTelephony.deviceId
                }
            } else {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            }
        }
    }


    private fun showSetting() {
        frameSetting.visibility = View.VISIBLE
        appbarFrame.visibility = View.GONE
        adsFrame.visibility = View.GONE
        bodyFrame.visibility = View.GONE
        adsTextFrame.visibility = View.GONE
    }

    private fun hideSetting() {
        frameSetting.visibility = View.GONE
        appbarFrame.visibility = View.VISIBLE
        adsFrame.visibility = View.VISIBLE
        bodyFrame.visibility = View.VISIBLE
        adsTextFrame.visibility = View.VISIBLE
    }

    override fun replaceFragement(page: Int) {
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        when (page) {
            PAGE_SETTING_KEY -> {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                mApp.pageId = PAGE_SETTING_KEY
                transaction.remove(AppBar())
                    .remove(AdsFragment())
                    .remove(bodyFragment)
                    .remove(AdsTextFragment())
                    .add(R.id.frameSetting, SettingScreen())
                showSetting()
            }
            PAGE_CLOSE -> {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                mApp.pageId = PAGE_CLOSE
                transaction.remove(AppBar())
                    .remove(AdsFragment())
                    .remove(bodyFragment)
                    .remove(AdsTextFragment())
                    .add(R.id.frameSetting, CloseService())
                showSetting()
            }
            PAGE_MAIN -> {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                bodyFragment = ShelfFragment()
                if (mApp.pageId == PAGE_SETTING_KEY || mApp.pageId == PAGE_CLOSE) {
                    hideSetting()
                    transaction.remove(SettingScreen())
                        .add(R.id.appbarFrame, AppBar())
                        .add(R.id.adsFrame, AdsFragment())
                        .add(R.id.bodyFrame, bodyFragment)
                        .add(R.id.adsTextFrame, AdsTextFragment())
                } else {
                    transaction.replace(R.id.bodyFrame, bodyFragment)
                }
                mApp.pageId = PAGE_MAIN
            }
            else -> {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                bodyFragment = ShelfFragment()
                if (mApp.pageId == PAGE_SETTING_KEY) {
                    transaction.remove(SettingScreen())
                        .add(R.id.appbarFrame, AppBar())
                        .add(R.id.adsFrame, AdsFragment())
                        .add(R.id.bodyFrame, bodyFragment)
                        .add(R.id.adsTextFrame, AdsTextFragment())
                } else {
                    transaction.replace(R.id.bodyFrame, bodyFragment)
                }
                mApp.pageId = PAGE_MAIN
            }
        }
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun showPayment() {
        val paymentDialog = PaymentDialog()
        paymentDialog.show(supportFragmentManager, "PaymentDialog")
        mApp.pageId = PAGE_POPUP
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        if (mApp.pageId == PAGE_MAIN) {
            replaceFragement(PAGE_SETTING_KEY)
        }
    }

    override fun dialogDismiss() {
//        fragmentCallback.dialogResume()
        mApp.pageId = PAGE_MAIN
    }

    override fun onDestroy() {
        super.onDestroy()
        MdbService.closeConnect()
        VendingService.recycleSerialPort()
    }

    override fun openAndroidSetting() {
        startActivityForResult(Intent(Settings.ACTION_SETTINGS), 0);
    }

    private fun loadSharedPref() {
        mApp.kioskID = sharedPref.getString(PREFS_KEY_KIOSK_ID, DEFAULT_KIOSK_ID).toString()
        mApp.registerKey = sharedPref.getString(PREFS_KEY_REGISTER_KEY, DEFAULT_REGISTER_KEY).toString()
        mApp.textAds = sharedPref.getString(PREFS_KEY_SERVICE, DEFAULT_SERVICE).toString()
        mApp.vendingModel = sharedPref.getInt(PREFS_VENDING_MODEL, DEFAULT_VENDING_MODEL)

        val aisleListType = ArrayList<Int>()
        aisleListType.add(sharedPref.getInt(PREFS_ROW1_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROW2_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROW3_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROW4_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROW5_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROW6_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROW7_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROW8_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROW9_TYPE, DEFAULT_ROW_TYPE))
        aisleListType.add(sharedPref.getInt(PREFS_ROWA_TYPE, DEFAULT_ROW_TYPE))
        VendingService.setAisleType(aisleListType)

    }

    override fun closeService() {
        replaceFragement(PAGE_CLOSE)
    }

    override fun openService() {
        if (mApp.pageId == PAGE_CLOSE) {
            replaceFragement(PAGE_MAIN)
        }
    }

    override fun openVendingPort() {
        VendingService.open(applicationContext)
    }

    override fun restartApp(){
        val intent = Intent(applicationContext, MainActivity::class.java)
        val mPendingIntentId: Int = 0
        val mPendingIntent = PendingIntent.getActivity(
            applicationContext,
            mPendingIntentId,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        val mgr = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        mgr[AlarmManager.RTC, System.currentTimeMillis() + 100] = mPendingIntent
        exitProcess(0)
    }

    override fun rebootDevice() {
        Runtime.getRuntime().exec("adb shell reboot").waitFor()
    }

    override fun waitRebootDevice(sec: Long) {
        if (!waitRebootActive) {
            Log.d(TAG, "waitRebootDevice")
            Handler().postDelayed({
                rebootDevice()
            }, sec * 1000)
            waitRebootActive = true
        }
    }
}

interface Callback {
    fun replaceFragement(page: Int)
    fun showPayment()
    fun dialogDismiss()
    fun openAndroidSetting()
    fun closeService()
    fun openService()
    fun openVendingPort()
    fun restartApp()
    fun rebootDevice()
    fun waitRebootDevice(sec: Long)
}





