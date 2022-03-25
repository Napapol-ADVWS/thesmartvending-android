package com.aiknowhow.hitopvending.fragmentScreen

import android.content.Context
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.aiknowhow.hitopvending.*
import com.aiknowhow.hitopvending.data.*
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.API_LOG
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PAGE_MAIN
import com.aiknowhow.hitopvending.serverInterface.FileManager
import com.aiknowhow.hitopvending.serverInterface.VendingInterface
import com.bumptech.glide.Glide
import com.google.gson.Gson
import kotlinx.android.synthetic.main.custom_product.view.*
import kotlinx.android.synthetic.main.payment_dialog.view.*
import kotlinx.android.synthetic.main.shelf_fragment.view.*
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ShelfFragment: Fragment() {
    private val TAG = "Shelf Fragment"
    private var mCallback: Callback? = null
    lateinit var mContext: Context

    private lateinit var mAdapter: CustomAdapter
    private lateinit var syncTimer: CountDownTimer
    private var cdSyncTime:Long = 10 // Minute
    private var cdSyncTimer = cdSyncTime*60 // Sec

    class CustomAdapter(
        val mDataList: ArrayList<menuData>,
        private val mContext: Context?,
        val itemClickListener: (menuData) -> Unit
    ) : RecyclerView.Adapter<ViewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.custom_product,
                parent,
                false
            )
            return ViewHolder(
                view
            )
        }

        override fun getItemCount(): Int {
//            return 10
            return (mDataList.size)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            try {
            val _item = mDataList[position]
            val itRow = _item.slot.row
            val itCol = _item.slot.col
            val itStatus = VendingService.getSlotStatus(itRow, itCol)

                holder.prodName.text = _item.productName
                holder.prodSlot.text = "${itRow}${itCol}"
                holder.prodExp.text = _item.expireMsg
                if(_item.price.sale < _item.price.normal && _item.price.sale != 0){
                    //view.prodPro.visibility = View.VISIBLE
                    val priceShow = "${_item.price.normal} -> ${_item.price.sale} บาท"
                    var ss = SpannableString(priceShow)
                    val strikeSpan = StrikethroughSpan()
                    ss.setSpan(strikeSpan, 0, _item.price.normal.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    holder.prodPrice.text = ss
                    holder.prodSale.visibility = View.VISIBLE;
                    holder.prodPrice.setBackgroundResource(R.drawable.bg_sale)
                }else{
                    holder.prodPrice.text = "${_item.price.normal} บาท"
                    holder.prodSale.visibility = View.GONE;
                }

                Glide.with(mContext!!).load(_item.productImage).into(
                    holder.prodImg
                )

                if(itStatus && _item.remain>0){
                    val cm = ColorMatrix()
                    cm.setSaturation(1f)
                    holder.prodImg.colorFilter = ColorMatrixColorFilter(cm)
                    holder.prodTap.setOnClickListener {
                        itemClickListener(mDataList[position])
                    }
                }else{
                    val cm = ColorMatrix()
                    cm.setSaturation(0f)
                    holder.prodImg.colorFilter = ColorMatrixColorFilter(cm)
                    holder.prodTap.setOnClickListener {
//                    itemClickListener(mDataList[position])
                    }
                }
            }catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val prodTap: RelativeLayout = itemView.prodTap

        //        val prodName = itemView.prodName
        val prodImg: ImageView = itemView.prodImg
        val prodName: TextView = itemView.prodName
        val prodPrice: TextView = itemView.prodPrice
        val prodSlot: TextView = itemView.prodSlot
        var prodSale: ImageView = itemView.sale
        val prodExp: TextView = itemView.prodExp
    }


    private val itemOnClick: (menuData) -> Unit = { data ->
        mApp.menuSelected = data
        if(mApp.menuSelected.price.sale < mApp.menuSelected.price.normal && mApp.menuSelected.price.sale != 0){
            mApp.menuSelected.payPrice = mApp.menuSelected.price.sale
        }else{
            mApp.menuSelected.payPrice = mApp.menuSelected.price.normal
        }
        mCallback!!.showPayment()
    }

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
        val view: View = inflater.inflate(R.layout.shelf_fragment, container, false)

        setupMenuView(view)
        initTimer(view)


        return view
    }

    private fun setupMenuView(v: View){
        mAdapter =
            CustomAdapter(
                ArrayList(),
                requireContext(),
                itemClickListener = itemOnClick
            )

        v.productView.adapter = mAdapter
        val layoutManager = GridLayoutManager(activity, 4)
        v.productView.layoutManager = layoutManager

        val scrollSpeed = 1000f
        val linearSmoothScroller: LinearSmoothScroller =
            object : LinearSmoothScroller(v.productView.context) {
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return scrollSpeed / displayMetrics.densityDpi
                }
            }

        val delayScroll = 60*1000L
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0) {
                    linearSmoothScroller.targetPosition = mAdapter.itemCount - 1
                    layoutManager.startSmoothScroll(linearSmoothScroller)
                } else {
                    linearSmoothScroller.targetPosition = 0
                    layoutManager.startSmoothScroll(linearSmoothScroller)
                }
            }
        }, 0, delayScroll)

        if(!VendingService.isReady()){
            showBoardError(v)
        }else{
            showConnectSv(v)
            val productCheck = productList.getProductList().isEmpty()
            if(mApp.isServerRegistered && productCheck){
//                MqttService.publishRequestProduct()
            }else{
                setupProductShow(v)
            }
        }
    }

    private fun registerServer(v: View){
        if (mApp.kioskID.isEmpty() || mApp.registerKey.isEmpty()) {
            return
        }
        showConnectSv(v)
//        val versionCode = BuildConfig.VERSION_CODE
//        val versionName = BuildConfig.VERSION_NAME
        val appName = getString(R.string.app_name)
        val appVersion = getString(R.string.app_version)
        val registerTag = "MachineRegister"
        val manager = mContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager
        val info = manager.connectionInfo
        val address = info.macAddress
        Log.d("WIFI", "MAC WIFI = $address")
        Log.d(registerTag, "Token : ${mApp.serverToken}")
        var bodyApi = HashMap<String, Any>()
        bodyApi["kioskID"] = mApp.kioskID.toInt()
        bodyApi["registerKey"] = mApp.registerKey
        bodyApi["androidID"] = mApp.androidID
        bodyApi["imei"] = mApp.imei
        bodyApi["appName"] = appName
        bodyApi["appVersion"] = appVersion
        val jsonBody = Gson().toJson(bodyApi).toString()
        var _body = RequestBody.create( MediaType.parse("application/json"),jsonBody)

        val _call = VendingInterface.getClient().register(
            body = _body
        )
        var logData = HashMap<String, Any>()
        logData["api"] = "register"
        logData["body"] = bodyApi
        _call.enqueue(object : retrofit2.Callback<registerResponse> {
            override fun onFailure(call: Call<registerResponse>, t: Throwable) {
                v.errorMsg.text = "Network Error"
                v.serverProgress.visibility = View.GONE
                Log.d(registerTag, "Failed to connect server")
                logData["status"] = "fail"
                val logJson = Gson().toJson(logData).toString()
                FileManager().writeFile(mContext, API_LOG, logJson)
//                Toast.makeText(mContext, "Cannot register to server", Toast.LENGTH_LONG).show()
                if (mApp.pageId == PAGE_MAIN) {
                    mCallback!!.waitRebootDevice(300)
                }
            }

            override fun onResponse(call: Call<registerResponse>, response: Response<registerResponse>) {
//                Log.d("RequestProduct", response?.body()!!)
                mApp.isOnline = true
                if (response.isSuccessful) {
                    if (response.body()!!.code == 200) {
                        Log.d(registerTag, "Response : ${response.body()}")
                        mApp.serverToken = response.body()!!.data.token
                        mApp.mqttPublish = response.body()!!.data.publish.toList() as ArrayList<String>
                        mApp.mqttSubscribe = response.body()!!.data.subscribe.toList() as ArrayList<String>
                        mApp.isServerRegistered = true
                        MqttService.connect(mApp.serverToken, mApp.mqttSubscribe)
                        MqttService.setupTopic(mApp.mqttPublish)
//                        testGson()
//                        requestQrMehod()
                        logData["status"] = "success"
                        val logJson = Gson().toJson(logData).toString()
                        FileManager().writeFile(mContext, API_LOG, logJson)
                        waitMqttConnect()
//                        MqttService.publishCheckIn()
                        Log.d(registerTag, "Request success")
                    } else {
                        v.errorMsg.text = "Server error : ${response.body()!!.code}\n${response.body()!!.message}"
                        v.serverProgress.visibility = View.GONE
                        logData["status"] = "fail"
                        val logJson = Gson().toJson(logData).toString()
                        FileManager().writeFile(mContext, API_LOG, logJson)
                    }
                } else {
                    v.errorMsg.text = "Server error"
                    v.serverProgress.visibility = View.GONE
                    Log.d(registerTag, "Failed to connect server, response unsuccessful")
                    logData["status"] = "fail"
                    val logJson = Gson().toJson(logData).toString()
                    FileManager().writeFile(mContext, API_LOG, logJson)
//                    Toast.makeText(mContext, "Cannot register to server", Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    private fun requestQrMehod(){
        val requestQrMethod = "Request QR Method"
        val token = "${mApp.tokenType} ${mApp.serverToken}"
        val _call = VendingInterface.getClient().qrMethodRequest(
            auth = token
        )
        _call.enqueue(object : retrofit2.Callback<qrMethodResponse> {
            override fun onFailure(call: Call<qrMethodResponse>, t: Throwable) {
                Log.d(requestQrMethod, "Failed to connect server")
            }

            override fun onResponse(call: Call<qrMethodResponse>, response: Response<qrMethodResponse>) {
                if (response.isSuccessful) {
                    if (response.body()!!.code == 200) {
//                        Log.d(requestQrMethod, "Response : ${response.body()}")
                        mApp.paymentMethod = response.body()!!.data
                        Log.d(requestQrMethod, "Request success")
                        Log.d(requestQrMethod, response.body()!!.toString())
                    } else {
                        Log.d(requestQrMethod, "Qr Method error")
                    }
                } else {
                    Log.d(requestQrMethod, "Qr Method error")
                }
            }
        })
    }

    private fun setupProductShow(v: View){
        v.errorFragment.visibility = View.INVISIBLE
        v.productView.visibility = View.VISIBLE
        //v.prodPro.visibility = View.INVISIBLE
        mAdapter.mDataList.clear()
        mAdapter.mDataList.addAll(productList.getProductList())
        mAdapter.notifyDataSetChanged()
    }

    private fun showBoardError(v: View){
        v.serverProgress.visibility = View.GONE
        v.errorFragment.visibility = View.VISIBLE
        v.productView.visibility = View.INVISIBLE
        v.errorMsg.text = getString(R.string.BoardErrorMsg)
    }

    private fun showConnectSv(v: View){
        v.serverProgress.visibility = View.VISIBLE
        v.errorFragment.visibility = View.VISIBLE
        v.productView.visibility = View.INVISIBLE
        v.errorMsg.text = getString(R.string.ConnectServerMsg)
    }

    private fun initTimer(v: View){
        val delayFunction = 5
        var inventPublishDelay = delayFunction
        syncTimer = object : CountDownTimer(cdSyncTimer * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if(mApp.dataChangedFlag){
                    mAdapter.notifyDataSetChanged()
                    mApp.dataChangedFlag = false
                }
                if(!VendingService.isReady()){
                    VendingService.open(mContext)

                }else{
                    if(!mApp.isServerRegistered && mApp.infoGet){
                        if(--inventPublishDelay == 0){
                            inventPublishDelay = delayFunction
                            registerServer(v)
                        }
                    }else{
//                        val productCheck = productList.getProductList().isEmpty()
//                        val mqttStatus = MqttService.getMqttStatus()
//                        if(mqttStatus && productCheck){
//                            if(--inventPublishDelay == 0){
//                                inventPublishDelay = delayFunction
//                                MqttService.publishRequestProduct()
//                                MqttService.publishGetAds()
//                            }
//                        }
                        if(productList.checkMenuUpdate()){
                            setupProductShow(v)
                            productList.clearMenuUpdate()
                        }
                    }
                }
                if(!MqttService.getServiceStatus() && mApp.pageId == PAGE_MAIN){
                    mCallback!!.closeService()
                }
                if(mApp.getRestart && mApp.pageId == PAGE_MAIN){
//                    mCallback!!.restartApp()
                    mCallback!!.rebootDevice()
                }
            }

            override fun onFinish() {
                if(mApp.isServerRegistered){
                    MqttService.publishCheckIn()
                }else{
                    if(!mApp.isServerRegistered && mApp.infoGet){
                        registerServer(v)
                    }
                }
                start()
            }
        }.start()
    }

    private fun delayRequestInventory(){
        val mqttStatus = MqttService.getMqttStatus()
        if(mqttStatus){
           MqttService.publishRequestProduct()
//           MqttService.publishGetAds()
        }else{
            waitMqttConnect()
        }
    }

    private fun waitMqttConnect(){
        Handler().postDelayed({
            MqttService.publishCheckIn()
            delayRequestInventory()
        }, 5000)
    }
}