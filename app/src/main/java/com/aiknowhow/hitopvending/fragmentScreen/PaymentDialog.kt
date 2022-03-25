package com.aiknowhow.hitopvending.fragmentScreen

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.view.*
import android.widget.Toast
import android.media.MediaPlayer
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiknowhow.hitopvending.*
import com.aiknowhow.hitopvending.data.*
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.API_LOG
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.LAST_BUY_LOG
import com.aiknowhow.hitopvending.data.GlobalVariable.Companion.PAGE_MAIN
import com.aiknowhow.hitopvending.serverInterface.FileManager
import com.aiknowhow.hitopvending.serverInterface.VendingInterface
import com.aiknowhow.hitopvending.serverInterface.toMap
import com.bumptech.glide.Glide
import com.google.gson.Gson
//import com.google.zxing.BarcodeFormat
//import com.google.zxing.EncodeHintType
//import com.google.zxing.WriterException
//import com.google.zxing.qrcode.QRCodeWriter
//import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.android.synthetic.main.custom_qr_method.view.*
import kotlinx.android.synthetic.main.payment_dialog.view.*
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
//import java.lang.Exception

class PaymentDialog : DialogFragment() {

    private val tagPayment = "PaymentDialog"
    private val tagQR = "QrRequest"
    private var mCallback: Callback? = null
    lateinit var mContext: Context

    private lateinit var mAdapter: CustomAdapter
    private lateinit var backTimer: CountDownTimer
    private var cdBack: Long = 180

    private lateinit var qrTimer: CountDownTimer
    private var qrTimeout: Long = 360

    lateinit var dialogView:View

    private var payComplete = false
    private var qrRef = 0
    private var qrInit = false
    private var qrSelect = false
    private var cashSelect = false

    private var coinReceived = 0
    private var billReceived = 0
    private var cashReceived = 0
    private var cashChanged = 0
    private var paymentType = ""

    private var saleFinished = false
    private var sendDeliveryComplete = false

    private var closeDialogFinish = false

    private val cashType = "Cash"

    private var enableBack = true
    private var paymentSelect = true
    private var delay = 50
    private var receivedSuccess = false

    private var cannotConnectServer = 0

    class CustomAdapter(
        val mDataList: ArrayList<qrMethodData>,
        private val mContext: Context?,
        val itemClickListener: (qrMethodData) -> Unit
    ) : RecyclerView.Adapter<ViewHolder>(){

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.custom_qr_method,
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
            val itemData = mDataList[position]
            val itStatus = itemData.active == 1
            val itType = itemData.name
            Log.d(mDataList[position].logo_image,"")
            Glide.with(mContext!!).load(itemData.logo_image).into(
                holder.qrButtonImage
            )
            if(itStatus){
                val cm = ColorMatrix()
                cm.setSaturation(1f)
                holder.qrButtonImage.colorFilter = ColorMatrixColorFilter(cm)
                if(itType == "Card") {
                    holder.qrButtonImage.setBackgroundResource(R.drawable.bg_credit)
                }
                holder.qrButtonImage.setOnClickListener {
                    itemClickListener(mDataList[position])
                }
            }else{
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                holder.qrButtonImage.colorFilter = ColorMatrixColorFilter(cm)
                holder.qrButtonImage.setOnClickListener {
//                    itemClickListener(mDataList[position])
                }
            }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val qrButtonImage = itemView.qrMethodButton
    }



    private val itemOnClick: (qrMethodData) -> Unit = { data ->
        val soundPayQR = MediaPlayer.create(context, R.raw.pay_qrcode)
        soundPayQR.start()
        setQrShow(dialogView, data.logo_image)
        txRequest(data.name)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.payment_dialog, container)

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

    override fun onStart() {
        super.onStart()

        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog!!.window!!.setLayout(width, height)
        dialog!!.window!!.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )


        MdbService.resetMoney()
//        mCallback = try {
//            context as Callback
//        } catch (e: ClassCastException) {
//            throw ClassCastException(context.toString().toString() + " must implement Callback")
//        }
    }

    private fun showToast(msg:String){
        val myToast = Toast.makeText(mContext,msg,Toast.LENGTH_LONG)
//        myToast.setGravity(Gravity.LEFT,200,200)
        myToast.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        backTimer.cancel()
        checkCancelQr()
        MdbService.cancelSale()
        mApp.dataChangedFlag = true
        mApp.pageId = PAGE_MAIN
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val soundSelect = MediaPlayer.create(context, R.raw.select_payment)
        soundSelect.start()
        dialog?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(R.color.bg_transparent)
        setStyle(STYLE_NO_INPUT, android.R.style.Theme)
        setupProductInfoView(view)

        dialogView = view
        setupQrMethodButton(view)
        view.paymentBack.setOnClickListener{
            val refundProcess = MdbService.getRefundProcess()
            if(enableBack && !refundProcess){
                enableBack = false
                Handler().postDelayed({enableBack = true}, 2000)
                val mediaPlayer = MediaPlayer.create(context, R.raw.button_push)
                mediaPlayer.start()
                if(qrSelect || cashSelect){
                    view.paymentBack.isEnabled = false
//                    if(MdbService.getMDBStatus()){
//                        cashChanged = MdbService.getUserMoney()
//                        if(cashChanged > 0){
//                            MdbService.cancelSale()
//                            showReturn(view, cashChanged)
//                        }
//                    }
                    waitRefundtoSendFinished("", "", false)
    //                backToPaymentSelect(view)
                }else{
                    dismiss()
                }
            }
        }
        if(!MdbService.getMDBStatus() || !MdbService.getCashStatus()){
            view.selectCash.isEnabled = false
            view.selectCash.background = resources.getDrawable(R.drawable.button_money_disable)
            //view.payWarning.visibility = View.VISIBLE
            val offPayment = MediaPlayer.create(context, R.raw.off_payment_)
            offPayment.start()
            //view.selectCash.setTextColor(Color.parseColor("#CCCCCC"))
        }
        view.selectCash.setOnClickListener {
            val refundProcess = MdbService.getRefundProcess()
            if(!refundProcess){
                val mediaPlayer = MediaPlayer.create(context, R.raw.button_push)
                mediaPlayer.start()
                if(MdbService.getMDBStatus()){
                    val midiapay = MediaPlayer.create(context, R.raw.pay_money)
                    midiapay.start()
                    setCashShow(view)
                    txRequest(cashType)
                }
            }
        }
        initTimer(view)
    }

    private fun setupQrMethodButton(view: View){
        mAdapter =
            CustomAdapter(
                ArrayList(),
                requireContext(),
                itemClickListener = itemOnClick
            )
        view.qrScroll.adapter = mAdapter
        val layoutManager = GridLayoutManager(activity, 2)
        view.qrScroll.layoutManager = layoutManager
        mAdapter.mDataList.clear()
        mAdapter.mDataList.addAll(mApp.paymentMethod)
        mAdapter.notifyDataSetChanged()
    }

    private fun backToPaymentSelect(view: View){
        checkCancelQr()
        hideQrView(view)
        hideCashView(view)
        showPaymentSelect(view)
        view.paymentBack.isEnabled = true
        qrSelect=false
        cashSelect=false
        backTimer.cancel()
        backTimer.start()
    }

    private fun setQrShow(view: View, qrLogoImage:String){
        qrSelect=true
        Glide.with(context!!).load(qrLogoImage).into(view.qrHead)
        view.qrHead.visibility = View.VISIBLE
        view.qrShow.visibility = View.VISIBLE
        hidePaymentSelect(view)
    }

    private fun setCashShow(view: View){
        cashSelect=true
        view.cashShow.visibility = View.VISIBLE
        view.cashRefundLine.visibility = View.INVISIBLE
        view.refundMsg.visibility = View.INVISIBLE
        view.cashPrice.text = "${mApp.menuSelected.payPrice}"
        view.cashReceived.text = "0"
        backTimer.cancel()
        backTimer.start()
        hidePaymentSelect(view)
    }

    private fun showRefund(view: View, refundAmount: Int){
        view.cashRefundLine.visibility = View.VISIBLE
        view.refundMsg.visibility = View.VISIBLE
        view.cashRefund.text = refundAmount.toString()
        view.caseRefundLabel.text = "เงินทอน"
        view.refundMsg.text = "เครื่องกำลังทอนเงิน"
    }

    private fun showReturn(view: View, refundAmount: Int){
        hideQrView(view)
        view.cashShow.visibility = View.VISIBLE
        view.cashPrice.text = "${mApp.menuSelected.payPrice}"
        view.cashReceived.text = refundAmount.toString()
        view.cashRefundLine.visibility = View.VISIBLE
        view.refundMsg.visibility = View.VISIBLE
        view.cashRefund.text = refundAmount.toString()
        view.caseRefundLabel.text = "คืนเงิน"
        view.refundMsg.text = "เครื่องกำลังคืนเงิน"
    }

    private fun setupProductInfoView(view: View){
        Glide.with(context!!).load(mApp.menuSelected.productImage).into(
            view.dialogProductImg
        )
        view.dialogProductName.text = mApp.menuSelected.productName
        view.dialogProductPrice.text = "${mApp.menuSelected.payPrice}฿"
        view.dialogProductDetail.text = mApp.menuSelected.description
    }

    private fun checkCancelQr(){
        if(qrInit){
            qrTimer.cancel()
            qrInit = false
        }
    }

    private fun hidePaymentSelect(view: View){
        view.paymentSelect.visibility = View.INVISIBLE
    }

    private fun showPaymentSelect(view: View){
        view.paymentSelect.visibility = View.VISIBLE
    }

    private fun hideCashView(view: View){
        cashSelect=false
        MdbService.stopReceived()
        view.cashShow.visibility = View.INVISIBLE
    }

    private fun hideQrView(view: View){
        view.qrHead.visibility = View.INVISIBLE
        view.qrShow.visibility = View.INVISIBLE
    }

    private fun disableBack(view: View){
        view.paymentBack.visibility = View.INVISIBLE
    }

    private fun showThankImg(view: View){
        hideQrView(view)
        val mediaPlayer = MediaPlayer.create(context, R.raw.thankyou2)
        mediaPlayer.start()
        view.showImgThank.visibility = View.VISIBLE
//        view.qrImage.setImageResource(R.drawable.thankyou)
    }

    private fun sendDeliveryProduct(v: View){
        Log.d(tagPayment, "Send delivery to ${mApp.menuSelected.slot.row}${mApp.menuSelected.slot.col}")
        if(VendingService.sendDelivery(
                mApp.menuSelected.slot.row,
                mApp.menuSelected.slot.col
            )){
            ////// Send cmd to vending board
            disableBack(v)
            showThankImg(v)
            sendDeliveryComplete = true
        }else{
            /////// cmd send incomplete
                                dismiss()
            Toast.makeText(
                context,
                "send cmd to control error",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initTimer(view: View) {
        var flagCompleteVending = false
//        var refundCancel = false
        backTimer = object : CountDownTimer(cdBack * 1000, 100) {
            override fun onTick(millisUntilFinished: Long) {
//                Log.d(tagPayment, "timer: ${millisUntilFinished / 1000} left")
//                val refundProcess = MdbService.getRefundProcess()
//                if(paymentSelect && !refundProcess ) {
//                    val userMoney = MdbService.getUserMoney()
//                    if(userMoney > 0){
//                        if(!refundCancel){
//                            refundCancel = true
//                            MdbService.cancelSale()
//                        }
//                    }else{
//                        refundCancel = false
//                    }
//                    return
//                }
                if(cashSelect && !payComplete){
                    cashProcess(view)
                }
                if(cashSelect){
                    val timeOut = millisUntilFinished/1000
                    view.cashTimeout.text=timeOut.toString()
                }

                if(payComplete && !flagCompleteVending){
                    if(!sendDeliveryComplete){
                        if(!cashSelect && !qrSelect){
                            payComplete = false
                        }else{
                            if(--delay == 0){
                                delay = 50
                                sendDeliveryProduct(view)
                            }
                        }
                    }else{
                        if(!VendingService.isWorking()){
                            val cb = VendingService.getError()
                            if(--delay == 0 && mApp.txid.isNotEmpty()){
                                delay=50
                                val isWarning = VendingService.checkProductWarning(cb.cbCode)
                                if(cb.isSuccess || isWarning){
                                    if(MdbService.getMDBStatus() && cashSelect){
                                        cashReceived = MdbService.getUserMoney()
                                        coinReceived = MdbService.getCoinReceived().toInt()
                                        billReceived = MdbService.getBillReceived().toInt()
                                        MdbService.deductedUserMoney(mApp.menuSelected.payPrice.toDouble())
                                        cashChanged = MdbService.getUserMoney()
                                        MdbService.finishSale()
                                    }
                                    if(cashChanged > 0){
                                        showRefund(view, cashChanged)
                                    }
                                    if(cb.isSuccess){
                                        waitRefundtoSendFinished(cb.cbCode.toString(), "success", true)
                                    }else{
                                        waitRefundtoSendFinished(cb.cbCode.toString(), "warning case", true)
                                    }
                                    productList.decreaseAmount(mApp.menuSelected)
                                }else{
                                    val isErrorCloseSale = VendingService.checkProductErrorCloseSale(cb.cbCode)
                                    if (isErrorCloseSale) {
                                        VendingService.setSlotStatus(
                                            mApp.menuSelected.slot.row,
                                            mApp.menuSelected.slot.col,
                                            false
                                        )
                                    }
                                    if(MdbService.getMDBStatus()){
                                        if(cashSelect){
                                            cashChanged = MdbService.getUserMoney()
                                            if(cashChanged > 0){
                                                showReturn(view, cashChanged)
                                            }
                                            MdbService.finishSale()
                                        }else{
                                            cashChanged = mApp.menuSelected.payPrice
                                            MdbService.refundMoney(cashChanged)
                                        }
                                    }

                                    waitRefundtoSendFinished(cb.cbCode.toString(), cb.errerMsg, true)
                                    view.qrImage.visibility = View.GONE
                                    view.showImgThank.visibility = View.INVISIBLE
                                    view.vendingError.visibility = View.VISIBLE
                                    view.msgComplete.text = "เกิดข้อผิดพลาดกับอุปกรณ์"
                                    view.msgComplete.visibility = View.VISIBLE
                                }
                                flagCompleteVending=true
                                Log.v("TESTRUN", "status = ${cb.isSuccess}, Error = ${cb.errerMsg}")
                            }
                        }
                    }
                }else if(saleFinished){
                    if(--delay==0){
                        dismiss()
                    }
                }
            }

            override fun onFinish() {
                if(cashSelect){
                    closeDialogFinish = true
                    saleFinished("", "", false)
                }else{
                    dismiss()
                }
            }
        }.start()
    }

    private fun initQrTimer(view: View){
        backTimer.cancel()
        view.qrTimeout.visibility = View.VISIBLE
        qrInit = true
        qrTimer = object : CountDownTimer(qrTimeout * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.d(tagQR, "timer: ${millisUntilFinished / 1000} left")
                val timeOut = millisUntilFinished/1000
                    view.qrTimeout.text=timeOut.toString()
                    val qrResult = MqttService.getPaymentResult()
                    if(qrResult != null){
                        val qrResponse = Gson().fromJson(qrResult.toString(), resultQr::class.java)
                        val qrRef = qrResponse.qr_ref
                        val qrStatus = qrResponse.status
                        if(qrRef == qrRef && qrStatus == "success"){
                            payComplete = true
                        }
                    }
                    if(payComplete){
                        cancel()
                        backTimer.start()
                    }
            }

            override fun onFinish() {
                closeDialogFinish = true
//                saleFinished("", "", false)
//                dismiss()
                checkQRPay()
            }
        }.start()
    }

    private fun checkRefund(code: String, msg: String, isFinished: Boolean){
        if(!MdbService.getRefundProcess()){
            saleFinished(code, msg, isFinished)
        }else{
            waitRefundtoSendFinished(code, msg, isFinished)
        }
    }

    private fun waitRefundtoSendFinished(code: String, msg: String, isFinished:Boolean){
        Handler().postDelayed({
            checkRefund(code, msg, isFinished)
        }, 1000)
    }

    private fun txRequest(type:String){
        val tagTx = "TransactionRequest"

        val slot = java.util.HashMap<String, Int>()
        slot["row"] = mApp.menuSelected.slot.row
        slot["col"] = mApp.menuSelected.slot.col

        val payment = java.util.HashMap<String, Any>()
        paymentType = type
        payment["type"] = type
        payment["price"] = mApp.menuSelected.payPrice

        val bodyJson = java.util.HashMap<String, Any>()
//        bodyJson["KioskID"] = mApp.kioskID.toInt()
        bodyJson["slot"] = slot
        bodyJson["payment"] = payment

        val jsonBody = Gson().toJson(bodyJson).toString()
        Log.v(tagTx, jsonBody)

        val bodyReq = RequestBody.create( MediaType.parse("application/json"),Gson().toJson(bodyJson).toString())
        val token = "${mApp.tokenType} ${mApp.serverToken}"
        val sendHTTP = VendingInterface.getClient().productSale(
            body = bodyReq,
            auth = token
        )
        val logData = HashMap<String, Any>()
        logData["api"] = "transaction"
        logData["body"] = bodyJson
        sendHTTP.enqueue(object : retrofit2.Callback<transactionResponse> {
            override fun onFailure(call: Call<transactionResponse>?, t: Throwable) {
                Log.d(tagTx, "Cannot connect to server")
                logData["status"] = "fail"
                showToast("Cannot connect to server")
                val logJson = Gson().toJson(logData).toString()
                FileManager().writeFile(mContext, API_LOG, logJson)
                backToPaymentSelect(dialogView)
                cannotConnectServer++
                if (cannotConnectServer >= 3) {
                    mApp.getRestart = true
                }
            }

            override fun onResponse(call: Call<transactionResponse>?, response: Response<transactionResponse>?) {
                mApp.isOnline = true
                cannotConnectServer = 0
                if (response!!.isSuccessful) {
                    if (response.body()!!.code == 200) {
                        val txRes = response.body()!!
                        Log.d(tagTx, "Request success")
//                        Log.d(tagTx, response.body()!!)
                        mApp.txid = txRes.transactionID.toString()
                        paymentSelect = false
                        if(cashSelect){
                            MdbService.openReceived(mApp.menuSelected.payPrice.toDouble())
                        }else{
//                            qrGenerator(view)
                            val imageBytes = Base64.decode(txRes.qr.imageWithBase64, Base64.DEFAULT)
                            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            dialogView.qrImage.setImageBitmap(decodedImage)
                            dialogView.qrProgress.visibility = View.INVISIBLE
                            dialogView.qrImage.visibility = View.VISIBLE
                            qrTimeout = txRes.qr.expireTimeSeconds.toLong()
                            qrRef = txRes.qr.ref
                            initQrTimer(dialogView)
                        }
                        logData["status"] = "success"
                        val logJson = Gson().toJson(logData).toString()
                        FileManager().writeFile(mContext, API_LOG, logJson)
                        Log.d(tagTx, "transId = ${mApp.txid}")
                    } else {
                        Log.d(tagTx, "Response failed")
                        logData["status"] = "fail"
                        val logJson = Gson().toJson(logData).toString()
                        FileManager().writeFile(mContext, API_LOG, logJson)
                        backToPaymentSelect(dialogView)
                    }
                } else {
                    Log.d(tagTx, "Response failed")
                    logData["status"] = "fail"
                    showToast("Cannot connect to server")
                    val logJson = Gson().toJson(logData).toString()
                    FileManager().writeFile(mContext, API_LOG, logJson)
                    backToPaymentSelect(dialogView)
                }
            }
        })
    }

    private fun checkQRPay() {
        val tagTx = "checkQRPay"

        Log.d(tagTx, "checkQRPay...")

//        val bodyJson = java.util.HashMap<String, Any>()
//        val jsonBody = Gson().toJson(bodyJson).toString()

//        val bodyReq = RequestBody.create( MediaType.parse("application/json"),jsonBody)
        val token = "${mApp.tokenType} ${mApp.serverToken}"
        val tranID = mApp.txid

        val sendHTTP = VendingInterface.getClient().checkQRPay(
//            body = bodyReq,
            auth = token,
            tranID = tranID
        )

        val logData = HashMap<String, Any>()
        logData["api"] = "inquire_transaction"
//        logData["body"] = bodyJson
        logData["tranID"] = tranID
        sendHTTP.enqueue(object : retrofit2.Callback<checkQRPayResponse> {

            override fun onFailure(call: Call<checkQRPayResponse>?, t: Throwable) {
                Log.d(tagTx, "Connect server failed")
                logData["status"] = "fail"
                val logJson = Gson().toJson(logData).toString()
                FileManager().writeFile(mContext, API_LOG, logJson)

                if(closeDialogFinish){
                    dismiss()
                }else{
                    backToPaymentSelect(dialogView)
                }
            }

            override fun onResponse(call: Call<checkQRPayResponse>?, response: Response<checkQRPayResponse>?) {
                mApp.isOnline = true
                if(response!!.isSuccessful){
                    val bodyRes = response.body()!!
                    Log.d(tagTx, "body response: $bodyRes")
                    if (bodyRes.code == 200) {
                        Log.d(tagTx, "Finished checkQRPay")
                        logData["status"] = "success"
                        val logJson = Gson().toJson(logData).toString()
                        FileManager().writeFile(mContext, API_LOG, logJson)

//                        val logCheckQRPayData = HashMap<String, Any>()
//                        logCheckQRPayData["body"] = bodyJson
//                        val logCheckQRPayJson = Gson().toJson(logCheckQRPayData).toString()
//                        FileManager().writeFile(mContext, LAST_BUY_LOG, logCheckQRPayJson)

                        // verify data
                        if ((bodyRes.paymentType == paymentType) && (bodyRes.transactionID == mApp.txid.toInt())) {
                            // valid data
                            when (bodyRes.status) {
                                "success" -> {
                                    payComplete = true
                                    backTimer.start()
                                    return
                                }
                                "fail" -> {

                                }
                                "cancel" -> {

                                }
                                else -> {

                                }
                            }
                        } else {
                            // invalid data
                        }

                        if(closeDialogFinish){
                            dismiss()
                        }else{
                            backToPaymentSelect(dialogView)
                        }
                    }else{
                        Log.d(tagTx, "Error response")
                        logData["status"] = "fail"
                        val logJson = Gson().toJson(logData).toString()
                        FileManager().writeFile(mContext, API_LOG, logJson)

//                        val logBuyData = HashMap<String, Any>()
//                        logBuyData["body"] = bodyJson
//                        val logBuyJson = Gson().toJson(logBuyData).toString()
//                        FileManager().writeFile(mContext, LAST_BUY_LOG, logBuyJson)

                        if(closeDialogFinish){
                            dismiss()
                        }else{
                            backToPaymentSelect(dialogView)
                        }
                    }
                }else{
                    Log.d(tagTx, "Error response")
                    logData["status"] = "fail"
                    val logJson = Gson().toJson(logData).toString()
                    FileManager().writeFile(mContext, API_LOG, logJson)

//                    val logCheckQRPayData = HashMap<String, Any>()
//                    logCheckQRPayData["body"] = bodyJson
//                    val logCheckQRPayJson = Gson().toJson(logCheckQRPayData).toString()
//                    FileManager().writeFile(mContext, LAST_BUY_LOG, logCheckQRPayJson)

                    if(closeDialogFinish){
                        dismiss()
                    }else{
                        backToPaymentSelect(dialogView)
                    }
                }
            }
        })
    }

    private fun saleFinished(statusCode: String, msg: String, isFinished: Boolean){
        val tagTx = "TransactionSuccess"
        var tubeInfo = ""
        var refundStatus = false
        var actionStatus = ""
        if(MdbService.getMDBStatus()){
            tubeInfo = MdbService.getCashInfo()
            refundStatus = MdbService.getRefundStatus()
        }
        val bodyJson = java.util.HashMap<String, Any>()
        val payment = java.util.HashMap<String, Any>()
        val amount = java.util.HashMap<String, Any>()

        amount["total"] = cashReceived
        amount["coin"] = MdbService.getCoinReceived()
        amount["bill"] = MdbService.getBillReceived()

        payment["amount"] = amount

        if(refundStatus){
            payment["changeMoney"] = cashChanged
        }else{
            payment["changeMoney"] = 0
        }

        if(cashSelect){
            payment["type"] = cashType
        }else{
            payment["type"] = paymentType
        }

        val jsonObj = JSONObject(tubeInfo)
        val mapTube = jsonObj.toMap()
        payment["coinStack"] = mapTube

        Log.d("STATUS CODE::::", statusCode)
        if(isFinished){
            val status = java.util.HashMap<String, Any>()
            var isWarning = false
            status["code"] = statusCode
            status["msg"] = msg
            bodyJson["kioskStatus"] = status

            if (statusCode.isNotEmpty()) {
                isWarning = VendingService.checkProductWarning(statusCode.toInt())
            }
            actionStatus = if(isWarning) { // Successful or Warning Order
                "complete"
            } else {    // Error and Refund
                "error"
            }
        }else{  // Cancel Order
            actionStatus = "cancel"
            if (qrSelect) {  // Check QR Payment
                checkQRPay()
                return
            }
        }

//        bodyJson["KioskID"] = mApp.kioskID.toInt()
        bodyJson["transactionID"] = mApp.txid.toInt()
        bodyJson["payment"] = payment
        bodyJson["action"] = actionStatus

        val jsonBody = Gson().toJson(bodyJson).toString()
        Log.v(tagTx, jsonBody)

        val bodyReq = RequestBody.create( MediaType.parse("application/json"),jsonBody)
        val token = "${mApp.tokenType} ${mApp.serverToken}"
        val sendHTTP = VendingInterface.getClient().saleFinished(
            body = bodyReq,
            auth = token
        )
        val logData = HashMap<String, Any>()
        logData["api"] = "transaction_finish"
        logData["body"] = bodyJson
        sendHTTP.enqueue(object : retrofit2.Callback<transactionCompleteResponse> {
            override fun onFailure(call: Call<transactionCompleteResponse>?, t: Throwable) {
                Log.d(tagTx, "Connect server failed")
                logData["status"] = "fail"
                val logJson = Gson().toJson(logData).toString()
                FileManager().writeFile(mContext, API_LOG, logJson)
                if(isFinished){
                    val logBuyData = HashMap<String, Any>()
                    logBuyData["body"] = bodyJson
                    val logBuyJson = Gson().toJson(logBuyData).toString()
                    FileManager().writeFile(mContext, LAST_BUY_LOG, logBuyJson)
                    saleFinished = true
                }else{
                    if(closeDialogFinish){
                        dismiss()
                    }else{
                        backToPaymentSelect(dialogView)
                    }
                }
            }

            override fun onResponse(call: Call<transactionCompleteResponse>?, response: Response<transactionCompleteResponse>?) {
                mApp.isOnline = true
                if(response!!.isSuccessful){
                    if (response.body()!!.code == 200) {
                        Log.d(tagTx, "Finished sale")
                        logData["status"] = "success"
                        val logJson = Gson().toJson(logData).toString()
                        FileManager().writeFile(mContext, API_LOG, logJson)
                        if(isFinished){
                            val logBuyData = HashMap<String, Any>()
                            logBuyData["body"] = bodyJson
                            val logBuyJson = Gson().toJson(logBuyData).toString()
                            FileManager().writeFile(mContext, LAST_BUY_LOG, logBuyJson)
                            saleFinished = true
                        }else{
                            if(closeDialogFinish){
                                dismiss()
                            }else{
                                backToPaymentSelect(dialogView)
                            }
                        }
                    }else{
                        Log.d(tagTx, "Error response")
                        logData["status"] = "fail"
                        val logJson = Gson().toJson(logData).toString()
                        FileManager().writeFile(mContext, API_LOG, logJson)
                        if(isFinished){
                            val logBuyData = HashMap<String, Any>()
                            logBuyData["body"] = bodyJson
                            val logBuyJson = Gson().toJson(logBuyData).toString()
                            FileManager().writeFile(mContext, LAST_BUY_LOG, logBuyJson)
                            saleFinished = true
                        }else{
                            if(closeDialogFinish){
                                dismiss()
                            }else{
                                backToPaymentSelect(dialogView)
                            }
                        }
                    }
                }else{
                    Log.d(tagTx, "Error response")
                    logData["status"] = "fail"
                    val logJson = Gson().toJson(logData).toString()
                    FileManager().writeFile(mContext, API_LOG, logJson)
                    if(isFinished){
                        val logBuyData = HashMap<String, Any>()
                        logBuyData["body"] = bodyJson
                        val logBuyJson = Gson().toJson(logBuyData).toString()
                        FileManager().writeFile(mContext, LAST_BUY_LOG, logBuyJson)
                        saleFinished = true
                    }else{
                        if(closeDialogFinish){
                            dismiss()
                        }else{
                            backToPaymentSelect(dialogView)
                        }
                    }
                }
            }
        })
    }

    private fun cashProcess(view: View){
        val getCash = MdbService.getUserMoney()
        if(cashReceived != getCash){
            cashReceived = getCash
//            if(cashReceived >= mApp.menuSelected.payPrice){
//                enableBack=false
//            }
            backTimer.cancel()
            backTimer.start()
        }
        view.cashReceived.text = cashReceived.toString()
        if(MdbService.checkFinished() && !receivedSuccess){
//            enableBack=false
            view.paymentBack.isEnabled = false
            receivedSuccess = true
            Handler().postDelayed({
                payComplete = true
                Log.d(tagPayment, "Cash pay completed")
            }, 5000 )
        }
    }
}