package com.aiknowhow.hitopvending.fragmentScreen

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aiknowhow.hitopvending.*
import com.aiknowhow.hitopvending.data.GlobalVariable
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PAGE_MAIN
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
import com.aiknowhow.hitopvending.serverInterface.VendingInterface
import com.google.gson.Gson
import kotlinx.android.synthetic.main.payment_dialog.view.*
import kotlinx.android.synthetic.main.setting_fragment.view.*
import retrofit2.Call
import retrofit2.Response
import android.view.Gravity

class SettingScreen : Fragment() {

    private val TAG = "SettingScreen"
    private var mCallback: Callback? = null
    lateinit var mContext: Context

    var setRow = 0
    var setCol = 0
    var btnArray = ArrayList<ArrayList<TextView>>()

    private lateinit var backTimer: CountDownTimer
    private var cdBack: Long = 600

    private var waitCallback = false
    private var isCbTest = false

    private var cashTestValue = 0
    private var cashTesting = false

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
        val view: View = inflater.inflate(R.layout.setting_fragment, container, false)

        view.inputKioskID.setText(mApp.kioskID)
        view.inputRegisterKey.setText(mApp.registerKey)
        view.inputTextAds.setText(mApp.textAds)
        view.selectVendingModel.setSelection(mApp.vendingModel-1)
        view.editMoneyReceived.setText(cashTestValue.toString())
        setViewID(view)
        setupButton(view)
        genGridButton(view)
        initTimer(view)
        return view
    }

    private fun setHeight(v: View) {
        val vendHeight = VendingService.getHeight()
        v.edt_storey0.setText(vendHeight[0].toString())
        v.edt_storey1.setText(vendHeight[1].toString())
        v.edt_storey2.setText(vendHeight[2].toString())
        v.edt_storey3.setText(vendHeight[3].toString())
        v.edt_storey4.setText(vendHeight[4].toString())
        v.edt_storey5.setText(vendHeight[5].toString())
        v.edt_storey6.setText(vendHeight[6].toString())
        v.edt_storey7.setText(vendHeight[7].toString())
        v.edt_storey8.setText(vendHeight[8].toString())
        v.edt_storey9.setText(vendHeight[9].toString())
    }

    private fun addCoinCheck(v: View){
        if(!MdbService.getMDBStatus()) return
        val tubeInfo = MdbService.getCashInfo()
        var map: Map<String, Any> = HashMap()
        map = Gson().fromJson(tubeInfo, map.javaClass)
//        var tmpCoinAmt = MdbService.getCoinAmount(1)
        v.text1b.text = map["C1"].toString()
        v.text2b.text = map["C2"].toString()
        v.text5b.text = map["C5"].toString()
        v.text10b.text = map["C10"].toString()

        val cashReceived = MdbService.getUserMoney()
        v.userMoneyShow.text = cashReceived.toString()
        if(MdbService.checkFinished() && cashTesting){
            cashTesting=false
            MdbService.deductedUserMoney(cashTestValue.toDouble())
            MdbService.finishSale()
        }
    }

    override fun onDestroyView() {
//        MdbService.stopSettingPage()
        backTimer.cancel()
        super.onDestroyView()
    }

    private fun initTimer(v: View) {
        backTimer = object : CountDownTimer(cdBack * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(TAG, "${millisUntilFinished / 1000} left")
                val timeLeft = millisUntilFinished / 1000
                v.tvTimeOut.text = "Timeout $timeLeft sec"
                addCoinCheck(v)
                if (!VendingService.isWorking() && waitCallback) {
                    waitCallback = false
                    val cb = VendingService.getError()
                    if (isCbTest) {
                        val isWarning = VendingService.checkProductWarning(cb.cbCode)
                        when {
                            cb.isSuccess -> {
                                btnArray[cb.x - 1][cb.y - 1].setBackgroundColor(
                                    ContextCompat.getColor(
                                        mContext,
                                        R.color.buttonTestPass
                                    )
                                )
                                VendingService.setSlotStatus(cb.x, cb.y, true)
                            }
                            isWarning -> {
                                btnArray[cb.x - 1][cb.y - 1].setBackgroundColor(
                                    ContextCompat.getColor(
                                        mContext,
                                        R.color.buttonTestWarning
                                    )
                                )
                                VendingService.setSlotStatus(cb.x, cb.y, true)
                            }
                            else -> {
                                btnArray[cb.x - 1][cb.y - 1].setBackgroundColor(
                                    ContextCompat.getColor(
                                        mContext,
                                        R.color.buttonTestFail
                                    )
                                )
                                VendingService.setSlotStatus(cb.x, cb.y, false)
                            }
                        }
                    } else {
                        setHeight(v)
                    }
                    v.tvStatus.text = cb.errerMsg
                }
            }

            override fun onFinish() {
                mCallback!!.replaceFragement(PAGE_MAIN)
            }
        }.start()
    }

    private fun restartTimerCountdown() {
        backTimer.cancel()
        backTimer.start()
    }

    private fun hideKeyboard() {
        val imm: InputMethodManager =
            context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view!!.windowToken, 0)
    }

    private fun setButton(v: View, r: Int, c: Int) {
        btnArray[r][c].visibility = View.VISIBLE
        btnArray[r][c].setOnClickListener {
            restartTimerCountdown()
            if (!waitCallback) {
                if (VendingService.sendDelivery(r + 1, c + 1)) {
                    ////// send command complete wait feedback
                    btnArray[r][c].setBackgroundColor(ContextCompat.getColor(
                        mContext,
                        R.color.buttonTestWorking
                    ))
                    v.tvStatus.text = "Send cmd delivery to slot ${r + 1}${c + 1}"
                    waitCallback = true
                    isCbTest = true
                }
            }
        }
    }

    private fun hideRow(r: Int) {
        for (i in 0..9) {   // column
            btnArray[r][i].visibility = View.INVISIBLE
        }
    }

    private fun showRow(r: Int) {
        for (i in 0..9) {   // column
            btnArray[r][i].visibility = View.VISIBLE
        }
    }

    private fun hideSetupHeightLift(v: View) {
        v.setHLine.visibility = View.INVISIBLE
    }

    private fun showSetupHeightLift(v: View) {
        v.setHLine.visibility = View.VISIBLE
    }

    private fun hideAllAisleSelectType(v: View) {
        v.row1Aisle.visibility = View.INVISIBLE
        v.row2Aisle.visibility = View.INVISIBLE
        v.row3Aisle.visibility = View.INVISIBLE
        v.row4Aisle.visibility = View.INVISIBLE
        v.row5Aisle.visibility = View.INVISIBLE
        v.row6Aisle.visibility = View.INVISIBLE
        v.row7Aisle.visibility = View.INVISIBLE
    }

    private fun showAllAisleSelectType(v: View) {
        v.row1Aisle.visibility = View.VISIBLE
        v.row2Aisle.visibility = View.VISIBLE
        v.row3Aisle.visibility = View.VISIBLE
        v.row4Aisle.visibility = View.VISIBLE
        v.row5Aisle.visibility = View.VISIBLE
        v.row6Aisle.visibility = View.VISIBLE
        v.row7Aisle.visibility = View.VISIBLE
    }

    private fun setAllAisleType(index: Int) {
        /**
         * look at string.xml name="row_type_val"
         */
        VendingService.setAisleTypeSlot(0, index)
        VendingService.setAisleTypeSlot(1, index)
        VendingService.setAisleTypeSlot(2, index)
        VendingService.setAisleTypeSlot(3, index)
        VendingService.setAisleTypeSlot(4, index)
        VendingService.setAisleTypeSlot(5, index)
        VendingService.setAisleTypeSlot(6, index)

        saveSharedPref(PREFS_ROW1_TYPE, index)
        saveSharedPref(PREFS_ROW2_TYPE, index)
        saveSharedPref(PREFS_ROW3_TYPE, index)
        saveSharedPref(PREFS_ROW4_TYPE, index)
        saveSharedPref(PREFS_ROW5_TYPE, index)
        saveSharedPref(PREFS_ROW6_TYPE, index)
        saveSharedPref(PREFS_ROW7_TYPE, index)
    }

    private fun setUltraPlusDefaultAisleType() {
        VendingService.setAisleTypeSlot(0, 5)
        VendingService.setAisleTypeSlot(1, 5)
        VendingService.setAisleTypeSlot(2, 5)
        VendingService.setAisleTypeSlot(3, 5)
        VendingService.setAisleTypeSlot(4, 5)
        VendingService.setAisleTypeSlot(5, 5)
        VendingService.setAisleTypeSlot(6, 5)

        saveSharedPref(PREFS_ROW1_TYPE, 5)
        saveSharedPref(PREFS_ROW2_TYPE, 5)
        saveSharedPref(PREFS_ROW3_TYPE, 5)
        saveSharedPref(PREFS_ROW4_TYPE, 5)
        saveSharedPref(PREFS_ROW5_TYPE, 5)
        saveSharedPref(PREFS_ROW6_TYPE, 5)
        saveSharedPref(PREFS_ROW7_TYPE, 5)
    }

    private fun genGridButton(v: View) {
        for (i in 0..6) {   // row
            for (j in 0..9) {   // column
                setButton(v, i, j)
            }
        }
    }

    private fun setupButton(v: View) {

        v.btnSetHeight.setOnClickListener {
            val setData = intArrayOf(
                v.edt_storey0.text.toString().toInt(),
                v.edt_storey1.text.toString().toInt(),
                v.edt_storey2.text.toString().toInt(),
                v.edt_storey3.text.toString().toInt(),
                v.edt_storey4.text.toString().toInt(),
                v.edt_storey5.text.toString().toInt(),
                v.edt_storey6.text.toString().toInt(),
                v.edt_storey7.text.toString().toInt(),
                v.edt_storey8.text.toString().toInt(),
                v.edt_storey9.text.toString().toInt()
            )
            if (!waitCallback) {
                if (VendingService.setAllStoreyHeight(setData)) {
                    waitCallback = true
                    isCbTest = false
                    v.tvStatus.text = "Send cmd to set height"
                    ////// send command complete wait feedback
                }
            }
        }

        v.btnLeave.setOnClickListener {
            /////// leave
//            mCallback!!.replaceFragement(PAGE_MAIN)
            mCallback!!.restartApp()
        }

        v.settingBg.setOnClickListener {
//            hideKeyboard()
        }

        v.btnSetting.setOnClickListener {
            mCallback!!.openAndroidSetting()
        }

        v.btnOpenCoin.setOnClickListener {
            cashTesting=true
            cashTestValue = v.editMoneyReceived.text.toString().toInt()
            MdbService.openReceived(cashTestValue.toDouble())
        }

        v.btnCloseCoin.setOnClickListener {
            cashTesting=false
            MdbService.cancelSale()
        }

        v.btnQueryHeight.setOnClickListener {
            if (!waitCallback) {
                if (VendingService.queryAllStorey()) {
                    v.tvStatus.text = "Send cmd to query height"
                    waitCallback = true
                    isCbTest = false
                }
            }
        }

        v.btnSaveSetting.setOnClickListener {
            mApp.kioskID = v.inputKioskID.text.toString()
            saveSharedPref(GlobalVariable.PREFS_KEY_KIOSK_ID, mApp.kioskID)

            mApp.registerKey = v.inputRegisterKey.text.toString()
            saveSharedPref(GlobalVariable.PREFS_KEY_REGISTER_KEY, mApp.registerKey)

            mApp.textAds = v.inputTextAds.text.toString()
            saveSharedPref(GlobalVariable.PREFS_KEY_SERVICE, mApp.textAds)

            val toast = Toast.makeText(context, "Saved", Toast.LENGTH_SHORT)
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
        }

        v.selectVendingModel.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mApp.vendingModel = mContext.resources.getIntArray(R.array.vending_model_val)[position]
                saveSharedPref(GlobalVariable.PREFS_VENDING_MODEL, mApp.vendingModel)

                when (mApp.vendingModel) {
                    1 -> {
                        hideRow(6)
                        hideAllAisleSelectType(v)
                        hideSetupHeightLift(v)
                        setAllAisleType(0)
                    }
                    2 -> {
                        hideRow(6)
                        hideAllAisleSelectType(v)
                        showSetupHeightLift(v)
                        setAllAisleType(1)
                    }
                    3 -> {
                        showRow(6)
                        hideAllAisleSelectType(v)
                        showSetupHeightLift(v)
                        setUltraPlusDefaultAisleType()
                    }
                    else -> {
                        showRow(6)
                        showAllAisleSelectType(v)
                        showSetupHeightLift(v)
                    }
                }
            }
        }
    }

    private fun setViewID(v: View) {
        val col1Array = ArrayList<TextView>()
        val col2Array = ArrayList<TextView>()
        val col3Array = ArrayList<TextView>()
        val col4Array = ArrayList<TextView>()
        val col5Array = ArrayList<TextView>()
        val col6Array = ArrayList<TextView>()
        val col7Array = ArrayList<TextView>()

        col1Array.add(v.btn11)
        col2Array.add(v.btn21)
        col3Array.add(v.btn31)
        col4Array.add(v.btn41)
        col5Array.add(v.btn51)
        col6Array.add(v.btn61)
        col7Array.add(v.btn71)

        col1Array.add(v.btn12)
        col2Array.add(v.btn22)
        col3Array.add(v.btn32)
        col4Array.add(v.btn42)
        col5Array.add(v.btn52)
        col6Array.add(v.btn62)
        col7Array.add(v.btn72)

        col1Array.add(v.btn13)
        col2Array.add(v.btn23)
        col3Array.add(v.btn33)
        col4Array.add(v.btn43)
        col5Array.add(v.btn53)
        col6Array.add(v.btn63)
        col7Array.add(v.btn73)

        col1Array.add(v.btn14)
        col2Array.add(v.btn24)
        col3Array.add(v.btn34)
        col4Array.add(v.btn44)
        col5Array.add(v.btn54)
        col6Array.add(v.btn64)
        col7Array.add(v.btn74)

        col1Array.add(v.btn15)
        col2Array.add(v.btn25)
        col3Array.add(v.btn35)
        col4Array.add(v.btn45)
        col5Array.add(v.btn55)
        col6Array.add(v.btn65)
        col7Array.add(v.btn75)

        col1Array.add(v.btn16)
        col2Array.add(v.btn26)
        col3Array.add(v.btn36)
        col4Array.add(v.btn46)
        col5Array.add(v.btn56)
        col6Array.add(v.btn66)
        col7Array.add(v.btn76)

        col1Array.add(v.btn17)
        col2Array.add(v.btn27)
        col3Array.add(v.btn37)
        col4Array.add(v.btn47)
        col5Array.add(v.btn57)
        col6Array.add(v.btn67)
        col7Array.add(v.btn77)

        col1Array.add(v.btn18)
        col2Array.add(v.btn28)
        col3Array.add(v.btn38)
        col4Array.add(v.btn48)
        col5Array.add(v.btn58)
        col6Array.add(v.btn68)
        col7Array.add(v.btn78)

        col1Array.add(v.btn19)
        col2Array.add(v.btn29)
        col3Array.add(v.btn39)
        col4Array.add(v.btn49)
        col5Array.add(v.btn59)
        col6Array.add(v.btn69)
        col7Array.add(v.btn79)

        col1Array.add(v.btn1A)
        col2Array.add(v.btn2A)
        col3Array.add(v.btn3A)
        col4Array.add(v.btn4A)
        col5Array.add(v.btn5A)
        col6Array.add(v.btn6A)
        col7Array.add(v.btn7A)

        btnArray.add(col1Array)
        btnArray.add(col2Array)
        btnArray.add(col3Array)
        btnArray.add(col4Array)
        btnArray.add(col5Array)
        btnArray.add(col6Array)
        btnArray.add(col7Array)

        val aisleType = VendingService.getAisleType()
        v.row1Aisle.setSelection(aisleType[0])
        v.row1Aisle.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                VendingService.setAisleTypeSlot(0, position)
                saveSharedPref(PREFS_ROW1_TYPE, position)
            }
        }

        v.row2Aisle.setSelection(aisleType[1])
        v.row2Aisle.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                VendingService.setAisleTypeSlot(1, position)
                saveSharedPref(PREFS_ROW2_TYPE, position)
            }
        }

        v.row3Aisle.setSelection(aisleType[2])
        v.row3Aisle.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                VendingService.setAisleTypeSlot(2, position)
                saveSharedPref(PREFS_ROW3_TYPE, position)
            }
        }

        v.row4Aisle.setSelection(aisleType[3])
        v.row4Aisle.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                VendingService.setAisleTypeSlot(3, position)
                saveSharedPref(PREFS_ROW4_TYPE, position)
            }
        }

        v.row5Aisle.setSelection(aisleType[4])
        v.row5Aisle.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                VendingService.setAisleTypeSlot(4, position)
                saveSharedPref(PREFS_ROW5_TYPE, position)
            }
        }

        v.row6Aisle.setSelection(aisleType[5])
        v.row6Aisle.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                VendingService.setAisleTypeSlot(5, position)
                saveSharedPref(PREFS_ROW6_TYPE, position)
            }
        }

        v.row7Aisle.setSelection(aisleType[6])
        v.row7Aisle.onItemSelectedListener = object : OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                VendingService.setAisleTypeSlot(6, position)
                saveSharedPref(PREFS_ROW7_TYPE, position)
            }
        }
    }

    private fun saveSharedPref(tag:String, value: Any?){
        with(sharedPref.edit()){
            when (value) {
                is String -> {
                    putString(tag, value)
                }
                is Int -> {
                    putInt(tag, value)
                }
                is Boolean -> {
                    putBoolean(tag, value)
                }
            }
            apply()
        }
    }
}