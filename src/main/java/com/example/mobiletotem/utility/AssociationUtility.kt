package com.example.mobiletotem.utility

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import com.example.mobiletotem.ForegroundWifiCarer
import com.example.mobiletotem.datatypes.Registration
import io.javalin.Javalin
import io.javalin.http.HttpCode

class AssociationUtility {
    //togliere route associate javalin e mettere deassociate, senza server restart
    private lateinit var http: Javalin
    companion object{
        private val instance = AssociationUtility()
        fun init(context: Context){
            instance.http = Javalin.create()
            instance.http.post("/associate"){
                    ctx ->
                val r = ctx.bodyAsClass<Registration>()
                VoiceSpeaker.speakOut("Associato a " + r.name)
                DataManager.init(r)
                Handler(Looper.getMainLooper()).postDelayed({
                    instance.http.stop()
                    ServerManager.init(context)
                    ForegroundWifiCarer.init(context)
                },100)
                ctx.status(HttpCode.OK)
            }
            instance.http.start(8080)
        }
    }
}