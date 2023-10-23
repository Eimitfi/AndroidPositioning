package com.example.mobiletotem

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.mobiletotem.utility.AssociationUtility
import com.example.mobiletotem.utility.VoiceSpeaker
import kotlin.system.exitProcess


class MainActivity : ComponentActivity() {
    private val filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path+ "/data.csv"
    private lateinit var dm:DevicePolicyManager
    private lateinit var wm:WifiManager
    private lateinit var wl:WifiManager.WifiLock
    private lateinit var cm:ConnectivityManager
    private lateinit var admRecv: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            Log.d(
                "DEBUG",
                "Thread: ${t.name} exception: ${e.printStackTrace()}"
            )
        }
        dm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        wm = getSystemService(WIFI_SERVICE) as WifiManager
        wl = wm.createWifiLock(WifiManager.WIFI_MODE_FULL,"TOTEM::activityLock")

        admRecv = ComponentName(baseContext,MyDeviceAdminReceiver::class.java)
        if(!dm.isAdminActive(admRecv)){
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admRecv)
            startActivityForResult(intent, 0);
        }

        if(!wm.isWifiEnabled || wm.wifiState == WifiManager.WIFI_STATE_DISABLING){
            wm.isWifiEnabled = true
        }
        if(!wl.isHeld){
            wl.acquire()
        }
        cm.activeNetwork ?: {
            Log.e("WIFI","Nessuna connessione trovata")
            exitProcess(1)
        }

        VoiceSpeaker.init(baseContext)
        //pronuncia parola per iniziare associazione, shake per confermare
        //VoiceListener.init(baseContext)

        setContentView(R.layout.activity_main)
        val buttonBlocca = findViewById<Button>(R.id.button_blocca)
        buttonBlocca.setOnClickListener {
            dm.lockNow()
        }
        val buttonTermina = findViewById<Button>(R.id.button_termina)
        buttonTermina.setOnClickListener {
            wl.release()
            ForegroundWifiCarer.cancel(this)
            VoiceSpeaker.deallocate()
            dm.lockNow()
            finish()
        }
        ForegroundWifiCarer.init(this)
        //AssociationUtility.init(this)
        dm.lockNow()

        /*
                VoiceListener.init(baseContext)
                OutputManager.init(baseContext)
                Handler(Looper.myLooper()!!).postDelayed({
                    OutputManager.instance.speakOut("Parla")
                    sleep(2000)
                    VoiceListener.start()
                    //metti un bel timerino zio senno...
                },10000)
        */

        //AssociationUtility.init(baseContext)

        //main()
        /*thread(start = true, isDaemon = true, name = "positioning routine") {
            val positioningRepeatmm = 10000L
            val e = EDAS(this.filePath,applicationContext)
            var md:Point3D? = null
            while (true){
                if(md != null){
                    e.setLastMD(md.x,md.y)
                }
                val mes = e.getFilteredAnchors()
                if(mes.isNotEmpty()){
                    md = Multilateration().getPosition(mes)
                    println("last md: $md")
                }
                Thread.sleep(positioningRepeatmm)
            }
        }*/
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onResume(){
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
    }
    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if(wl.isHeld) {
            wl.release()
        }
        ForegroundWifiCarer.cancel(this)
        VoiceSpeaker.deallocate()
    }
}




