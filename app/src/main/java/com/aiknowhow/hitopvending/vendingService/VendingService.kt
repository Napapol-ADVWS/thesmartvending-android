package com.aiknowhow.hitopvending.vendingService

import android.content.Context
import android.serialport.YySerialPort
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.aiknowhow.hitopvending.R
import com.lomoment.serialportsdk.VendingMachineCode
import com.lomoment.serialportsdk.VendingMachineKey
import com.lomoment.serialportsdk.VendingMachineMananger
import com.lomoment.serialportsdk.VendingMachineUtils
import com.lomoment.serialportsdk.entity.LCMachineInfo
import com.lomoment.serialportsdk.utils.VendingMachineDevicesUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class VendingService {
    val TAG = "VendingBoard"
    private val path = "/dev/ttyS4"
    private var indBaud = 0
    private val baud = arrayListOf(57600, 115200)
//    private val baudrate = 57600
//    private val baudrate = 115200
    private var storeyHeight = intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
    private var aisleRowType = ArrayList<Int>()
    private var isLiftEnable = true


    private var serialPort: VendingMachineMananger? = null
    private val shelfMap = ArrayList<LCMachineInfo>()
    private val requestMap = HashMap<String, Any>()
    private val mcuidList = ArrayList<String>()

    private val resultCountUtils = ResultCountUtils()
    private val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private var machineId: String = ""
    private lateinit var mContext: Context

//    private var isVendingReady = false
    private var isVendingReady = true
    private var isWorking = false

    private var cbStatus = vendingCallback()

    private var slotStatus = ArrayList<ArrayList<Boolean>>()

    init {
        for(i in 1..10){
            val rowStatus = arrayListOf(true, true, true, true, true, true, true, true, true, true)
            slotStatus.add(rowStatus)
        }
    }

    fun open(context: Context) {
        mContext = context
        connectSerialVending()
    }

    private fun connectSerialVending() {
        recycleSerialPort()
        createSerialPort()
    }

    fun recycleSerialPort() {
        if (serialPort != null) {
            serialPort!!.release()
        }
        isVendingReady = false
    }

    private fun createSerialPort() {
        serialPort = VendingMachineMananger(mContext)
        //设置su路径
        val supath = VendingMachineDevicesUtils.getSuPathFromDevices()
        YySerialPort.setSuPath(supath)
        serialPort!!.init(path, baud[indBaud])
//        serialPort!!.init(path, baudrate)
        if(++indBaud > 1) indBaud = 0
        //show log
        VendingMachineMananger.isShowLog = true
        //Implement the callback
        serialPort!!.setVendingmachineResponseListener { machine, opration, state, seq, args ->
            if (requestMap.containsKey(seq)) {
                //splicing result
                val sb = java.lang.StringBuilder()
                val cb = java.lang.StringBuilder()
                Log.v(TAG, "Get cb from command")
                cb.append(simpleDateFormat.format(Calendar.getInstance().time))
                cb.append("\t")

                sb.append(simpleDateFormat.format(Calendar.getInstance().time))
                sb.append("\t")
                sb.append(mContext.resources.getString(R.string.machine))
                sb.append("$machine\tseq：$seq")
                sb.append("\t")
                //deliver operation
                if (opration == VendingMachineKey.OP_DELIVER) {
                    if (state == VendingMachineCode.STATE_SUCCESS) {
                        if (args[1] == VendingMachineCode.DELIVER_EMBODY_SUCCESS) {
                            cbStatus.isSuccess = true
                            cb.append(mContext.resources.getString(R.string.deliver_success))
                            sb.append(mContext.resources.getString(R.string.deliver_success))
                        } else {
                            cbStatus.isSuccess = false
                            cb.append(mContext.resources.getString(R.string.deliver_fail))
                            cb.append(", fail code : ")
                            cb.append(args[1])

                            sb.append(mContext.resources.getString(R.string.deliver_fail))
                            sb.append("\t")
                            sb.append(mContext.resources.getString(R.string.explain_code))
                            sb.append(args[1])
                        }
                    } else if (state == VendingMachineCode.STATE_FAIL) {
                        cb.append(mContext.resources.getString(R.string.deliver_fail))
                        cb.append(", fail code : ")
                        cb.append(args[1])

                        sb.append(mContext.resources.getString(R.string.deliver_fail))
                        sb.append("\t")
                        sb.append(mContext.resources.getString(R.string.explain_code))
                        sb.append(args[1])
                    }
                    cb.append(", Slot ${args[2]}${args[3]}")
                    sb.append(mContext.resources.getString(R.string.aisle))
                    cbStatus.cbCode = args[1]
                    cbStatus.x = args[2]
                    cbStatus.y = args[3]
                    sb.append("  x " + args[2] + "   y " + args[3])
                    resultCountUtils.countResult(args[1].toString() + "")
                } else if (opration == VendingMachineKey.OP_LIFT_STORY_HEIGHT_SETTING_ALL || opration == VendingMachineKey.OP_LIFT_STORY_HEIGHT_QUERY || opration == VendingMachineKey.OP_LIFT_STORY_HEIGHT_RESET) {
                    if (state == VendingMachineKey.STATE_SUCCESS) {
                        cbStatus.isSuccess = true
//                        Toast.makeText(
//                            mContext,
//                            mContext.resources.getString(R.string.operation_success),
//                            Toast.LENGTH_SHORT
//                        ).show()
//                        if (opration == VendingMachineKey.OP_LIFT_STORY_HEIGHT_SETTING_ALL) {
//                            isVendingReady = true
//                        }
                        if (opration == VendingMachineKey.OP_LIFT_STORY_HEIGHT_QUERY) {
                            Log.v(TAG, "success get height")
                            storeyHeight[0] = args[0]
                            storeyHeight[1] = args[1]
                            storeyHeight[2] = args[2]
                            storeyHeight[3] = args[3]
                            storeyHeight[4] = args[4]
                            storeyHeight[5] = args[5]
                            storeyHeight[6] = args[6]
                            storeyHeight[7] = args[7]
                            storeyHeight[8] = args[8]
                            storeyHeight[9] = args[9]
                        }
                    } else {
                        cbStatus.isSuccess = false
                        cb.append(mContext.resources.getString(R.string.operation_fail))
//                        Toast.makeText(
//                            mContext,
//                            mContext.resources.getString(R.string.operation_fail),
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                } else if (opration == VendingMachineKey.OP_LIFT_STORY_HEIGHT_SETTING_SINGLE) {
                    if (state == VendingMachineCode.STATE_SUCCESS) {
                        cbStatus.isSuccess = true
//                        Toast.makeText(
//                            mContext,
//                            mContext.resources.getString(R.string.operation_success),
//                            Toast.LENGTH_SHORT
//                        ).show()
                    } else {
                        cbStatus.isSuccess = false
                        cb.append(mContext.resources.getString(R.string.operation_fail))
//                        Toast.makeText(
//                            mContext,
//                            mContext.resources.getString(R.string.operation_fail),
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                } else if (opration == VendingMachineKey.OP_LIGHT_BRIGHTNESS) {
                    if (state == VendingMachineCode.STATE_SUCCESS) {
                        cbStatus.isSuccess = true
//                        Toast.makeText(
//                            mContext,
//                            mContext.resources.getString(R.string.operation_success),
//                            Toast.LENGTH_SHORT
//                        ).show()
                    } else {
                        cbStatus.isSuccess = false
                        cb.append(mContext.resources.getString(R.string.operation_fail))
//                        Toast.makeText(
//                            mContext,
//                            mContext.resources.getString(R.string.operation_fail),
//                            Toast.LENGTH_SHORT
//                        ).show()
                    }
                } else {
                    if (state == VendingMachineCode.STATE_SUCCESS) {
                        cbStatus.isSuccess = true
                        sb.append(mContext.resources.getString(R.string.operation_success))
                    } else {
                        cbStatus.isSuccess = false
                        cb.append(mContext.resources.getString(R.string.operation_fail))
                        sb.append(mContext.resources.getString(R.string.operation_fail))
                    }
                }
                sb.append("\n")
                val msg = sb.toString()
                cbStatus.errerMsg = cb.toString()
                isWorking = false
                Log.v(TAG, "Working done")
                Log.v(TAG, cbStatus.errerMsg)
                Log.v(TAG, cb.toString())
                requestMap.remove(seq)
                //Statistics
//                val tempStr = resultCountUtils.getString().replace(",","\n").replace("\\{|\\}","")
//                tv_count.text = "statistics\n $tempStr"
            }
        }

        serialPort!!.setMachineQueryResponseListener { list ->
            shelfMap.clear()
            if (list != null && list.size > 0) {
                shelfMap.addAll(list)
                initMachineNumberSpinnerAdapter()
                isVendingReady = true
//                queryAllStorey()
            } else {
//                Toast.makeText(mContext, mContext.resources.getString(R.string.not_found_machine), Toast.LENGTH_LONG)
//                    .show()
            }
        }

        queryMachine()
    }

    private fun initMachineNumberSpinnerAdapter() {
        mcuidList.clear()
        for (info in shelfMap) {
            var name = ""
            if (serialPort != null && serialPort!!.isExistMachineNumber(info.mcuid)) {
                when (serialPort!!.getMachinePortByNumber(info.mcuid)) {
                    VendingMachineKey.MACHINE_ROUTE_MIDDLE -> {
                        name = "Main"
                    }
                    VendingMachineKey.MACHINE_ROUTE_LEFT -> {
                        name = "C"
                    }
                    VendingMachineKey.MACHINE_ROUTE_RIGHT -> {
                        name = "A"
                    }
                }
            } else {
                name = "unknown"
            }
            //Join device details (Routing,Machine number,Version)
            name =
                name + VendingMachineUtils.SEPARATOR + info.mcuid + VendingMachineUtils.SEPARATOR + serialPort!!.getMachineVersionByNumber(
                    info.mcuid
                )
            mcuidList.add(name)
        }
        if (mcuidList.size > 0) {
            mcuidList.sortWith(Comparator { o1, o2 -> o1.compareTo(o2) })
        }
        machineId = mcuidList[0]
    }

    /////// Call to check cmd complete
    fun isWorking(): Boolean{
        return isWorking
    }

    fun getError():vendingCallback{
        isWorking = false
        return cbStatus
    }

    fun checkProductWarning(code: Int): Boolean{
        return when (code) {
            2000 -> {
                true
            }
            in 4000..7000 -> {
                !(code == 4002 || code == 4024)
            }
            else -> {
                false
            }
        }
    }

    fun checkProductErrorCloseSale(code: Int): Boolean{
        return when (code) {
            in 3000..3999 -> {
                true
            }
            in 32000..32999 -> {
                code != 32100
            }
            else -> {
                false
            }
        }
    }

    ////// Call to send cmd to delivery
    fun sendDelivery(row: Int, col: Int): Boolean {
        if (serialPort == null) {
            return false
        }
        val machine = checkMachine()
        if (TextUtils.isEmpty(machine)) {
//            Toast.makeText(
//                mContext,
//                mContext.resources.getString(R.string.create_serial_port_operation_obj_please),
//                Toast.LENGTH_LONG
//            ).show()
            return false
        }
        cbStatus.isSuccess = false
//        if (spAisle.getSelectedItem() != null) {
//            point = spAisle.getSelectedItemPosition()
//        }
//        //截取机器编号——机器显示做了额外的数据添加（主副柜、单片机版本）

        var rowType = aisleRowType[row-1]
        var typeSend = mContext.resources.getIntArray(R.array.row_type_val)[rowType]
        val seq = serialPort!!.sendDeliveryPacketMsg(
            machine,
            row,
            col,
            typeSend           ////// 1 = spring no lift, 3 = lift spring, 4 = lift track
        )
        //保存seq值,Object为业务对象
        requestMap[seq] = Any()
        isWorking = true
        return true
    }

    ///////// Call to get all storey height in array
    fun getHeight():IntArray {
        return storeyHeight
    }

    //////// Call from outside to set storey height
    fun setAllStoreyHeight(storeySet:IntArray): Boolean {
        val tmpData = storeyHeight
        storeyHeight = storeySet
        return if(!setAllStorey()) {
            storeyHeight = tmpData
            false
        }else {
            isWorking = true
            true
        }
    }

    private fun queryMachine() {
        if (serialPort == null) {
            return
        }
        serialPort!!.sendQueryMachineMsg()
    }

    fun queryAllStorey():Boolean {
        val machine = checkMachine()
        if (TextUtils.isEmpty(machine)) {
//            Toast.makeText(
//                mContext,
//                mContext.resources.getString(R.string.create_serial_port_operation_obj_please),
//                Toast.LENGTH_LONG
//            ).show()
            return false
        }
        Log.v(TAG, "send cmd query storey height")
        requestMap[serialPort!!.queryAllStoreyOfLiftMachine(machine)] = Any()
        isWorking = true
        return true
    }

    private fun setAllStorey():Boolean {
        val machine: String = checkMachine().toString()
        if (TextUtils.isEmpty(machine)) {
//            Toast.makeText(
//                mContext,
//                mContext.resources.getString(R.string.create_serial_port_operation_obj_please),
//                Toast.LENGTH_LONG
//            ).show()
            return false
        }
        requestMap[serialPort!!.setAllStoreyOfLiftMachine(machine, storeyHeight)] = Any()
        return true
    }

    private fun checkMachine(): String? {
        var machine: String? = null
        if (!machineId.isBlank()) {
            machine = machineId
            machine = machine.split(VendingMachineUtils.SEPARATOR.toRegex()).toTypedArray()[1]
        }
        return if (TextUtils.isEmpty(machine)) {
            null
        } else machine
    }

    fun isReady():Boolean{
        return isVendingReady
    }

    fun setSlotStatus(row: Int,col: Int, status: Boolean){
        slotStatus[row-1][col-1] = status
    }

    fun getSlotStatus(row: Int, col: Int): Boolean{
        return  slotStatus[row-1][col-1]
    }

    fun setAisleType(type:ArrayList<Int>){
        aisleRowType = type
    }

    fun setAisleTypeSlot(slot:Int, type:Int){
        Log.v(TAG, "set slot $slot, type : $type")
        aisleRowType[slot] = type
    }

    fun getAisleType():ArrayList<Int>{
        return aisleRowType
    }

    fun setLiftStatus(status: Boolean){
        isLiftEnable = status
    }

    fun getLiftStatus():Boolean{
        return isLiftEnable
    }
}

data class vendingCallback (
    var isSuccess:Boolean = false,
    var cbCode:Int = 0,
    var errerMsg:String = "",
    var x:Int = 0,
    var y:Int = 0
)