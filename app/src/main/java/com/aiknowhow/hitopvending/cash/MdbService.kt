package com.aiknowhow.hitopvending.cash

import android.os.Handler
import android.serialport.YySerialPort
import android.util.Log
import com.google.gson.Gson
import com.lomoment.pmod.DevicesInitListener
import com.lomoment.pmod.IDevices
import com.lomoment.pmod.cash.CashDevicesManager
import com.lomoment.pmod.cash.TransactionListener
import com.lomoment.serialportsdk.utils.VendingMachineDevicesUtils
import java.lang.Exception
import java.util.*

class MdbService : TransactionListener, DevicesInitListener {
    private val TAG = "MdbService"

    private val MDB_Path = "/dev/ttyS1"

    //    private val mdb_path = arrayListOf("/dev/ttyS1","/dev/ttyS2")
//    private var indPath = 0
    private var waitConnect = false
    private val MDB_Baudrate = "9600"
    private var EnableMDB = false

    private var DeviceName = ""

    private var mdbConnected = false
    private var _fail_counter = 10
    private var isReceiveFinish = false

    private var isOpenReceived = false

    private var coinReceived: Double = 0.0
    private var billReceived: Double = 0.0

    private var isRefundProcess = false
    private var isRefundSuccess = true

    private var userMoney = 0

    private var cashStatus = true

    init {
        CashAmountManager.getInstance().addTransactionListener(this)
        CashAmountManager.getInstance().setDeviceInitListenerList(this)

        startBoutiqueRefreshTimer(100)
    }

    private fun startBoutiqueRefreshTimer(delayMs: Long) {
        Handler().apply {
            val runnable = object : Runnable {
                override fun run() {
                    checkConnection()
                    postDelayed(this, delayMs)
                }
            }
            postDelayed(runnable, delayMs)
        }
    }

    private fun checkConnection() {
        if (!mdbConnected && EnableMDB) {
            if (_fail_counter++ >= 5 * 10 && !waitConnect) {
                connectMDB()
                _fail_counter = 0
            }
        } else {
            _fail_counter = 0
        }
    }

    private fun connectMDB() {
        //set su path
        YySerialPort.setSuPath(VendingMachineDevicesUtils.getSuPathFromDevices())
        waitConnect = true
//        val pathConnect = mdb_path[indPath]
//        Log.v(TAG, "MDB Connect to path: $pathConnect")
//        CashAmountManager.getInstance().init(pathConnect, MDB_Baudrate.toInt())
        CashAmountManager.getInstance().init(MDB_Path, MDB_Baudrate.toInt())
//        if(++indPath>1) indPath=0
    }

    fun openConnect() {
        EnableMDB = true
    }

    fun closeConnect() {
        EnableMDB = false
        CashAmountManager.getInstance().release()
    }

    fun openReceived(price: Double) {
        CashAmountManager.getInstance().tryOpenReceived()
        CashAmountManager.getInstance().price = price
        resetMoney()
        isOpenReceived = true
    }

    fun stopReceived() {
        if (isOpenReceived) {
            CashAmountManager.getInstance().stopReceivedMoney()
            isOpenReceived = false
        }
    }

    fun resetMoney() {
//        CashAmountManager.getInstance().setUserAmount(0.0)
        isReceiveFinish = false
//        userMoney = 0
//        coinReceived = 0.0
//        billReceived = 0.0
    }

    fun checkFinished(): Boolean {
        return isReceiveFinish
    }

    fun deductedUserMoney(price: Double) {
        CashAmountManager.getInstance().executeAmountOfDeducted(price)
    }

    fun getMDBStatus(): Boolean {
        return mdbConnected
    }

    fun getUserMoney(): Int {
        return userMoney
    }

    fun cancelSale() {
        stopReceived()
        if (userMoney != 0) {
            CashAmountManager.getInstance().executeRefund()
            isRefundProcess = true
            isRefundSuccess = true
        }
    }

    fun finishSale() {
        if (userMoney != 0) {
            CashAmountManager.getInstance().executeRefund()
            isRefundProcess = true
            isRefundSuccess = true
        }
//        CashAmountManager.getInstance().transactionFinish()
    }

    fun getCashDeviceInfo() {
        var info = CashAmountManager.getInstance().cashMoneyInfo
        for (key in info.keys) {
            Log.d("CashEquipINFO", "$key = ${info[key]}\n")
        }
    }

//    fun getCoinStatus():Boolean{
//        var a = CashAmountManager.getInstance().checkConnected()
//    }

    fun getCashStatus():Boolean{
        return cashStatus
    }

    fun setCashStatus(a: Boolean){
        cashStatus = a
    }

    fun getCashInfo(): String {
        var money: Map<Double, Int> = CashAmountManager.getInstance().tubeInfo
//        money = money-2.0
//        cashStatus = try{
//            !(money[1.0]!! < 4 || money[5.0]!! < 1 || money[10.0]!! < 10)
//        }catch (e:Exception){
//            e.printStackTrace()
//            false
//        }
        val gson = Gson()
        val moneyJson = gson.toJson(money).toString()
            .replace("1.0", "C1")
            .replace("2.0", "C2")
            .replace("5.0", "C5")
            .replace("10.0", "C10")
        Log.d("CashINFO", "${moneyJson}\n")
        return moneyJson
    }

    override fun initSuccess(p0: IDevices?) {
        if (p0 != null) {
            DeviceName = p0.devicesName
        }
    }

    override fun initFinish(p0: IDevices?) {
        if (p0 != null) {
            DeviceName = p0.devicesName
        }
        Log.d(TAG, "Device Name : ${DeviceName}")
        mdbConnected = true
        CashAmountManager.getInstance().stopReceivedMoney()
        getCashDeviceInfo()
        waitConnect = false
    }

    override fun initFail(p0: IDevices?) {
        Log.d(TAG, "MDB Init Failed")
        if (p0 != null) {
            DeviceName = p0.devicesName
        }
        mdbConnected = false
        waitConnect = false
    }

    override fun receivedMoneyFinish() {
        isReceiveFinish = true
        isOpenReceived = false
    }

    override fun refuseToAcceptTheMoney(p0: Int, p1: Double) {
        Log.d(TAG, "refuse to accept money $p0, $p1")
    }

//    override fun acceptTheMoneySuccess(p0: Double) {
//        Log.d(TAG, "accept money success $p0")
//    }

    override fun acceptTheMoneySuccess(type: Int, money: Double) {
        when (type) {
            CashDevicesManager.CURRENCY_TYPE_BILL -> {
                billReceived += money
            }
            CashDevicesManager.CURRENCY_TYPE_COIN -> {
                coinReceived += money
            }
            CashDevicesManager.CURRENCY_TYPE_CASHLESS -> {
                //            label = "Cashless"
            }
        }
        Log.d(TAG, "accept money success $money")
    }

    fun getCoinReceived(): Double {
        return coinReceived
    }

    fun getBillReceived(): Double {
        return billReceived
    }

    fun getRefundProcess(): Boolean {
        return isRefundProcess
    }

    fun getRefundStatus(): Boolean {
        return isRefundSuccess
    }

    fun refundMoney(money: Int){
        val canRefund = CashAmountManager.getInstance().isCanRefundFinish(money.toDouble())
        if(canRefund){
            CashAmountManager.getInstance().refundMoney(money.toDouble())
            isRefundProcess = true
            isRefundSuccess = true
        }else{
            isRefundProcess = false
            isRefundSuccess = false
        }
    }

    override fun userMoneyChange(p0: Double) {
        userMoney = p0.toInt()
        if (userMoney == 0) {
            coinReceived = 0.0
            billReceived = 0.0
        }
    }

    override fun executeRefundFail() {
        Log.d(TAG, "refund fail")
        isRefundProcess = false
        isRefundSuccess = false
    }

    override fun executeRefundSuccess() {
        Log.d(TAG, "refund success")
        isRefundProcess = false
        isRefundSuccess = true
    }

    override fun clearMoneyEven(p0: Double) {
        Log.d(TAG, "clear money ${p0}")
    }
}