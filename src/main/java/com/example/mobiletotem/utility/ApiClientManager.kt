package com.example.mobiletotem.utility

import android.net.ConnectivityManager
import android.util.Log
import java.io.IOException

import java.net.HttpURLConnection
import java.net.URL


class ApiClientManager {
    private val tag = "APICLIENTMANAGER"
    private val totemURL = "http://10.100.0.37:8080/"
    private val positionRoute = "newReception/smartwatchAPI/postap"

    private fun getConn(route:String): HttpURLConnection {
        return URL(instance.totemURL+route).openConnection() as HttpURLConnection
    }

    fun sendPosition(id: Int, bssid: String, rssi: Int){
        val conn = getConn(instance.positionRoute)
        conn.requestMethod = "POST"
        conn.setRequestProperty("Cache-Control","no-cache")
        conn.setRequestProperty("Connection","close")
        conn.setRequestProperty("Content-Type","application/json")
        conn.doOutput = true
        conn.connectTimeout = 0
        conn.readTimeout = 0
        val json = "{\"id\":$id,\"bssid\":\"$bssid\",\"rssi\":$rssi}"
        try {
            conn.connect()
            conn.outputStream.use { os ->
                val input: ByteArray = json.toByteArray()
                os.write(input, 0, input.size)
            }
            conn.responseCode
            conn.disconnect()
        }catch(e: IOException){
            Log.d(tag, e.printStackTrace().toString())
            return
        }
    }

    companion object{
        private val instance = ApiClientManager()
        fun sendPosition(id:Int,bssid:String,rssi:Int){
            return instance.sendPosition(id,bssid,rssi)
        }
    }

}