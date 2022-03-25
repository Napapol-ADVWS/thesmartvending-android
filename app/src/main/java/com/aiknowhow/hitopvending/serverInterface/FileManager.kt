package com.aiknowhow.hitopvending.serverInterface

import android.content.Context
import android.util.Log
import java.io.File


class FileManager {

    fun writeFile(mContext:Context,fileName: String, data: String){
        val filePath = mContext.filesDir.path.toString() + String
        var fileObject = File(filePath)
        // create a new file
        val isNewFileCreated :Boolean = fileObject.createNewFile()
        if(isNewFileCreated){
            println("$fileName is created successfully.")
        } else{
            println("$fileName already exists.")
        }

        fileObject.writeText(data)
    }

    fun readFile(mContext: Context,fileName: String):String{
        val filePath = mContext.filesDir.path.toString() + String
        var fileObject = File(filePath)

        val isNewFileCreated :Boolean = fileObject.createNewFile()
        if(isNewFileCreated){
            println("$fileName is created successfully.")
            return ""
        } else{
            println("$fileName already exists.")
            Log.d("FileRead", fileObject.readText())
            return fileObject.readText()
        }
    }
}