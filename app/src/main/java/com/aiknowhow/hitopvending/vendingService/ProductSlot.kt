package com.aiknowhow.hitopvending.vendingService

import android.util.Log
import com.aiknowhow.hitopvending.data.menuData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.Exception

class ProductSlot {
    private val TAG = "ProductClass"
    private var productList = ArrayList<menuData>()

    private var menuUpdate = false

    fun setupProductMenu(data: String){
        productList.clear()
        val gson = Gson()
        val type = object: TypeToken<Map<String, Any>>(){}.type
        var rowMap: Map<String, Any> = gson.fromJson(data, type)
        rowMap.forEach {mapRow ->
            val row = mapRow.key.filter { it.isDigit() }.toInt()
            val _col = gson.toJson(mapRow.value)
            Log.d(TAG, _col)
            val typeCol = object: TypeToken<Map<String, menuData>>(){}.type
            var colMap: Map<String, menuData> = gson.fromJson(_col,typeCol)
            colMap.forEach {mapCol ->
                val col = mapCol.key.filter { it.isDigit() }.toInt()
                val collectData = mapCol.value
//                collectData.row = row
//                collectData.col = col
//                collectData.amt = 1
                productList.add(collectData)
                Log.d(TAG, collectData.toString())
            }
        }
    }

    fun setupMenu(listMenu: ArrayList<menuData>){
        productList = listMenu.filterNotNull() as ArrayList<menuData>
        menuUpdate = true
    }

    fun checkMenuUpdate():Boolean{
        return  menuUpdate
    }

    fun clearMenuUpdate(){
        menuUpdate = false
    }

    fun getProductList():ArrayList<menuData> {
        return  productList
    }

    fun decreaseAmount(menu: menuData){
        val selectIndex = productList.indexOf(menu)
        if(selectIndex != -1){
            productList[selectIndex].remain--
        }
    }
}