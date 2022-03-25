package com.aiknowhow.hitopvending.serverInterface

import com.aiknowhow.hitopvending.data.*
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

interface VendingInterface {

    @Headers("Content-Type: application/json")
    @POST("api/v1/kiosk/register/")
    fun register(
        @Body body: RequestBody
    ): Call<registerResponse>

    @POST("api/v1/payment/method/")
    fun qrMethodRequest(
        @Header("Authorization") auth: String
        // ขอ QR Payment Method ทั้งหมดที่ตู้รับ
    ): Call<qrMethodResponse>

    @Headers("Content-Type: application/json")
    @POST("api/v1/kiosk/transaction/")
    fun productSale(
        @Header("Authorization") auth: String,
        @Body body: RequestBody
        // ส่งรายละเอียดสินค้า (เลขที่ช่อง และช่องการชำระเงินที่เลือก)
        // ได้ txid
    ): Call<transactionResponse>

    @Headers("Content-Type: application/json")
    @GET("api/v1/kiosk/transaction/{tranID}")
    fun checkQRPay(
//        @GET("api/v1/kiosk/transaction/") tranID: String,
        @Path("tranID") tranID: String,
        @Header("Authorization") auth: String
        // ตรวจสอบการจ่าย QR ตอนหมดเวลาจ่ายหรือยกเลิกหน้าตู้
        // ได้ paymentType, transactionID, status
    ): Call<checkQRPayResponse>

    @Headers("Content-Type: application/json")
    @POST("api/v1/kiosk/transactionsuccess/")
    fun saleFinished(
        // ยืนยันการขาย สำเร็จ/ไม่สำเร็จ
        @Header("Authorization") auth: String,
        @Body body: RequestBody
    ): Call<transactionCompleteResponse>


    companion object Factory {

        const val BASE_URL = "https://api.advancevending.net/"

        private var retrofit: Retrofit? = null

        fun getClient(): VendingInterface {

            if (retrofit == null ) {
                retrofit = Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }

            return retrofit!!.create(
                VendingInterface::class.java)

        }
    }
}