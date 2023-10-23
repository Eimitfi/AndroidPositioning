package com.example.mobiletotem.utility

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Thread.sleep
import java.util.Locale
import kotlin.math.pow

class WifiService : Service(){


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    private fun calcDistance(freq:Double,level:Int):Double{
        val exp = (27.55 - (20 * kotlin.math.log10(freq)) + kotlin.math.abs(level)) / 20.0
        return 10.0.pow(exp)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val wm = getSystemService(WIFI_SERVICE) as WifiManager
        var AP = ""
        var distance = 0.0
        val n = NotificationCompat.Builder(this, "SERVICE CHANNEL").setContentTitle("LOCALIZZATORE")
            .build()
        ServiceCompat.startForeground(this, 100, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        while (true) {
            val info = wm.connectionInfo
            if (info.bssid != null) {
                if (AP != info.bssid) {
                    if (info.bssid.uppercase(Locale.getDefault()) == "30:CB:C7:9F:37:B1") {
                        VoiceSpeaker.speakOut("Sono in mensa")
                    } else if (info.bssid.uppercase(Locale.getDefault()) == "30:CB:C7:9F:0F:D1") {
                        VoiceSpeaker.speakOut("Sono in stabilimento e1 entrata accanto alla mensa")
                    } else if (info.bssid.uppercase(Locale.getDefault()) == "BC:A9:93:8E:B3:51") {
                        VoiceSpeaker.speakOut("Sono in ufficio sopra le scale")
                    } else if (info.bssid.uppercase(Locale.getDefault()) == "30:CB:C7:9E:92:71") {
                        VoiceSpeaker.speakOut("Sono in ufficio vicino al bagno")
                    } else {
                        VoiceSpeaker.speakOut("Sono dove non conosco l'access point")
                    }
                }
                AP = info.bssid
                distance = calcDistance(info.frequency.toDouble(), info.rssi)
            }
            ApiClientManager.sendPosition(DataManager.getID(), AP, info.rssi)
            sleep(1000)
        }
    }

    override fun stopService(name: Intent?): Boolean {
        super.stopService(name)
        stopSelf()
        return true
    }
}

class PositioningManager private constructor() {
    private var period:Long = 0
    private val exceptionHandler =
        CoroutineExceptionHandler { _, e ->
            Log.d(
                "DEBUG",
                "exception: ${e.stackTrace}"
            )
            Log.d(
                "DEBUG",
                "cause: ${e.cause}"
            )
        }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    private lateinit var wm:WifiManager
    private lateinit var AP:String
    private var distance:Double = 0.0
    private lateinit var stopS:(Intent)->(Boolean)
    private lateinit var intent: Intent
    companion object{
        private var instance = PositioningManager()
        fun init(period:Long,context:Context){
            instance.wm = context.getSystemService(ComponentActivity.WIFI_SERVICE) as WifiManager
            instance.period = period
            instance.intent = Intent(context,WifiService::class.java)
            instance.stopS = context::stopService
            val info = instance.wm.connectionInfo!!
            instance.AP = info.bssid
            instance.distance = instance.calcDistance(info.frequency.toDouble(),info.rssi)

            instance.scope.launch{
                try{
                    context.startForegroundService(instance.intent)
                }finally{context.stopService(instance.intent)}
            }

        }
        fun stop(){
            instance.scope.coroutineContext.cancelChildren()
        }
    }

    private fun calcDistance(freq:Double,level:Int):Double{
        val exp = (27.55 - (20 * kotlin.math.log10(freq)) + kotlin.math.abs(level)) / 20.0
        return 10.0.pow(exp)
    }
    private suspend fun send(){
        //prova a lanciare exception e vedi che succede
        while (true) {
            val info = wm.connectionInfo
            if (info.bssid != null) {
                if (this::AP.isInitialized && AP != info.bssid) {
                    if (info.bssid.uppercase(Locale.getDefault()) == "30:CB:C7:9F:37:B1") {
                        VoiceSpeaker.speakOut("Sono in mensa")
                    } else if (info.bssid.uppercase(Locale.getDefault()) == "30:CB:C7:9F:0F:D1") {
                        VoiceSpeaker.speakOut("Sono in stabilimento e1 entrata accanto alla mensa")
                    } else if (info.bssid.uppercase(Locale.getDefault()) == "BC:A9:93:8E:B3:51") {
                        VoiceSpeaker.speakOut("Sono in ufficio sopra le scale")
                    } else if (info.bssid.uppercase(Locale.getDefault()) == "30:CB:C7:9E:92:71") {
                        VoiceSpeaker.speakOut("Sono in ufficio vicino al bagno")
                    } else {
                        VoiceSpeaker.speakOut("Sono dove non conosco l'access point")
                    }
                }
                AP = info.bssid
                distance = calcDistance(info.frequency.toDouble(), info.rssi)
            }
            ApiClientManager.sendPosition(DataManager.getID(), instance.AP, info.rssi)
            delay(period)
        }
    }
}