package com.example.mobiletotem

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.mobiletotem.utility.ApiClientManager
import com.example.mobiletotem.utility.DataManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KFunction


class ForegroundWifiCarer : Service(){
    private val tag = "FOREGROUNDMANAGER"
    private lateinit var wifiPositioning:BroadcastReceiver
    private lateinit var wifiUpper:BroadcastReceiver
    private lateinit var wm:WifiManager
    private lateinit var cm:ConnectivityManager
    private lateinit var wl:WifiLock
    private lateinit var pm:PowerManager
    private lateinit var wakl: PowerManager.WakeLock
    private val exceptionHandler =
        CoroutineExceptionHandler { _, e ->
            Log.d(
                tag,
                "exception: ${e.stackTrace}${System.lineSeparator()}cause: ${e.cause}"
            )
        }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        pm = getSystemService(POWER_SERVICE) as PowerManager
        wm = (getSystemService(WIFI_SERVICE) as WifiManager)
        cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        wl = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "TOTEM::ForegroundWifiLock")
        wakl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TOTEM::ForegroundWakeLock")

        wifiUpper = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) = goAsync {
                cm.activeNetwork?: {
                    scope.launch {
                        wifiUpDaemon()
                    }
                }
            }
        }

        wifiPositioning = object : BroadcastReceiver(){
            override fun onReceive(context: Context?, intent: Intent?) = goAsync {
                cm.activeNetwork?.run {
                    val info = wm.connectionInfo
                    if(info.bssid == null)
                        return@goAsync
                    Log.d("WIFIPOSITION","Send ${info.bssid} ${info.rssi}")
                    ApiClientManager.sendPosition(DataManager.getID(), info.bssid,info.rssi)
                }
            }
        }
        val CHANNEL_ID = "my_channel_01"
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MOBILE TOTEM CHANNEL",
            NotificationManager.IMPORTANCE_HIGH
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Localizzazione")
            .setContentText("")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true).build()
        startForeground(1, notification)
    }

    private suspend fun wifiUpDaemon(){
        val nreq = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // network is available for use
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                cm.bindProcessToNetwork(network)
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                if(!wm.isWifiEnabled || wm.wifiState == WifiManager.WIFI_STATE_DISABLING){
                    wm.isWifiEnabled = true
                }
                if(!wl.isHeld){
                    wl.acquire()
                }
                cm.unregisterNetworkCallback(this)
            }
        }
        try{
            if(!wakl.isHeld){
                wakl.acquire(24*60*60*1000L)
            }
            while(true){
                if(!wm.isWifiEnabled  || wm.wifiState == WifiManager.WIFI_STATE_DISABLING){
                    wm.isWifiEnabled = true
                }
                if(!wl.isHeld){
                    wl.acquire()
                }
                cm.requestNetwork(nreq, networkCallback)
                delay(500)
                if(cm.activeNetwork != null){
                    val info = wm.connectionInfo
                    if(info == null){
                    	//each time unregister to not leak 
                        cm.unregisterNetworkCallback(networkCallback)
                        continue
                    }
                    //until you don't find connection again
                    ApiClientManager.sendPosition(DataManager.getID(), info.bssid,info.rssi)
                    currentCoroutineContext().cancel()
                }else{
                    cm.unregisterNetworkCallback(networkCallback)
                }
                //try each second
                delay(1000)
            }
        }finally{
            if(wakl.isHeld){
                wakl.release()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if(intent?.getStringExtra("stop") != null){
            this.scope.coroutineContext.cancelChildren()
            unregisterReceiver(wifiPositioning)
            unregisterReceiver(wifiUpper)
            if(wl.isHeld){
                wl.release()
            }
            if(wakl.isHeld){
                wakl.release()
            }
            stopForeground(true)
            stopSelf()
        }

        val wpos = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        wpos.addAction(WifiManager.RSSI_CHANGED_ACTION)
        registerReceiver(wifiUpper,IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION))
        registerReceiver(wifiPositioning,wpos)
        val info = wm.connectionInfo
        if(info == null){
            //è appena stato associato tramite apihttp...
        }else{
            scope.launch {
                ApiClientManager.sendPosition(DataManager.getID(), info.bssid,info.rssi)
                currentCoroutineContext().cancel()
            }
        }
        return START_STICKY
    }

    override fun stopService(name: Intent?): Boolean {
        this.scope.coroutineContext.cancelChildren()
        unregisterReceiver(wifiPositioning)
        unregisterReceiver(wifiUpper)
        if(wl.isHeld){
            wl.release()
        }
        if(wakl.isHeld){
            wakl.release()
        }
        return super.stopService(name)
    }

    companion object{
        private val instance = ForegroundWifiCarer()
        //startForegroundService = (Intent)->(ComponentName?)
        fun init(context: Context){
            context.startForegroundService(Intent(context,ForegroundWifiCarer::class.java))
        }

        fun cancel(context:Context){
            val i = Intent(context,ForegroundWifiCarer::class.java)
            i.putExtra("stop","yes")
            context.startService(Intent(context,ForegroundWifiCarer::class.java))
        }
    }
}

internal fun BroadcastReceiver.goAsync(
    coroutineContext: CoroutineContext = Dispatchers.IO,
    block: suspend CoroutineScope.() -> Unit,
) {
    val coroutineScope = CoroutineScope(SupervisorJob() + coroutineContext)
    val pendingResult = goAsync()

    coroutineScope.launch {
        try {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e("DEBUG", "Wifi positioning BroadcastReceiver execution failed", t)
            } finally {
                // Nothing can be in the `finally` block after this, as this throws a
                // `CancellationException`
                coroutineScope.cancel()
            }
        } finally {
            // This must be the last call, as the process may be killed after calling this.
            pendingResult.finish()
        }
    }
}
