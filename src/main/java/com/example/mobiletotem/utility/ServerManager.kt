package com.example.mobiletotem.utility

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.mobiletotem.ForegroundWifiCarer
import io.javalin.Javalin
import io.javalin.http.HttpCode

class ServerManager {
    private lateinit var http: Javalin
    companion object{
        private val instance = ServerManager()
        fun init(context: Context){
            instance.http = Javalin.create()
            instance.http.post("/api/deassociate"){
                    ctx ->
                VoiceSpeaker.speakOut("Associazione sciolta. riporre l'oggetto al suo posto. buona giornata")
                Handler(Looper.getMainLooper()).postDelayed({
                    instance.http.stop()
                    ForegroundWifiCarer.cancel(context)
                    AssociationUtility.init(context)
                },100)
                ctx.status(HttpCode.OK)
            }
            instance.http.start(8080)
        }
    }
}