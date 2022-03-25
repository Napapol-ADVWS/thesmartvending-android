package com.aiknowhow.hitopvending.vendingService

import java.util.*

class ResultCountUtils {
    private val resultMap: MutableMap<String, Int?> =
        HashMap()

    /**
     * statistics
     *
     * @param code
     */
    fun countResult(code: String) {
        if (resultMap.containsKey(code)) {
            resultMap[code] = resultMap[code]!! + 1
        } else {
            resultMap[code] = 1
        }
    }

    fun getString(): String {
        return resultMap.toString()
    }
}
