package com.aiknowhow.hitopvending.serverInterface

import android.content.Context
import com.danikula.videocache.HttpProxyCacheServer


class VideoCache {
    private var proxy: HttpProxyCacheServer? = null
    private fun newProxy(context: Context): HttpProxyCacheServer {
        return HttpProxyCacheServer.Builder(context).maxCacheSize(1024 * 1024 * 1024).build()
    }

    fun getProxy(context: Context): HttpProxyCacheServer {
        return if (proxy == null) newProxy(context).also { proxy = it } else proxy!!
    }

    fun getProxyUrl(url:String):String {
        return proxy!!.getProxyUrl(url)
    }
}