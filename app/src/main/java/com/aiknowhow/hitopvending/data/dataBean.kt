package com.aiknowhow.hitopvending.data

import com.google.gson.annotations.SerializedName

data class registerResponse (
    @SerializedName("code") val code : Int,
    @SerializedName("data") val data : dataRegister,
    @SerializedName("message") val message: String
        )

data class dataRegister(
    @SerializedName("token") val token : String,
    @SerializedName("publish") val publish : ArrayList<String>,
    @SerializedName("subscribe") val subscribe : ArrayList<String>
)

data class qrMethodResponse(
    @SerializedName("code")  val code: Int,
    @SerializedName("data") val data : ArrayList<qrMethodData>,
)

data class qrMethodResponseMqtt(
    var cmd:String = "",
    var method:ArrayList<qrMethodData>,
)

data class qrMethodData(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("active") val active : Int,
    @SerializedName("logo_image") val logo_image: String
)

data class transactionResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("transaction") val transaction: String,
    @SerializedName("transactionID") val transactionID: Int,
    @SerializedName("qr") val qr: qrResponseData
)

data class checkQRPayResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("paymentType") val paymentType: String,
    @SerializedName("transactionID") val transactionID: Int,
    @SerializedName("status") val status: String
)

data class qrResponseData(
    @SerializedName("ref") val ref: Int,
    @SerializedName("price") val price: Int,
    @SerializedName("expireTimeSeconds") val expireTimeSeconds: Int,
    @SerializedName("imageWithBase64") val imageWithBase64: String
)

data class transactionCompleteResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("transaction") val transaction: String,
    @SerializedName("transactionID") val transactionID: Int
)

data class resultQr(
    var qr_ref:Int,
    var status:String,
    var price:Int
)

data class mqttCmdSetupMenu(
    var kioskID:Int,
    var cmd:String = "",
    var inventory:ArrayList<menuData>,
)

data class menuData(
    var slotID:Int,
    var slot:slotData,
    var productName: String,
    var productImage: String,
    var SKU: String,
    var remain: Int,
    var price:priceData,
    var description: String,
    var payPrice: Int,
    var expireMsg: String
)

data class slotData(
    var row:Int,
    var col:Int
)

data class priceData(
    var normal:Int,
    var sale:Int
)

data class adsSetup(
    var kioskID: Int,
    var cmd: String,
    var config: adsConfigData,
)

data class adsConfigData(
    var type: String,
    var state: String,
    var ads: ArrayList<adsVideoData>,
    var notice: ArrayList<noticeData>
)

data class adsVideoData(
    var id: Int,
    var name: String,
    var url: String,
    var duration: Int
)

data class noticeData(
    var id: Int,
    var name: String,
    var msg: String
)

data class mediaAds ( val url: String,
                      val mimetype: String,
                      val filename: String,
                      val filesize: String,
                      val createdate: String)

data class adsResponse (    val statuscode: String,
                            val statusdetail: String ,
                            val topic: Int,
                            val title: String,
                            val expire: String,
                            val media: ArrayList<mediaAds>)

