package com.example.mobiletotem.utility
/*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import io.javalin.Javalin


import kotlin.math.pow

private class AccelerometerManager(private val sensorManager: SensorManager,private val sensibility: Int,private var reaction: () -> Unit) : SensorEventListener{
    private var accelerometer: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
    private var mGravity: FloatArray = FloatArray(3)
    private var mAccel = 0f
    private var mAccelCurrent = SensorManager.GRAVITY_EARTH
    private var mAccelLast = SensorManager.GRAVITY_EARTH

    fun setReaction(react: () -> Unit){
        reaction = react
    }

    fun setListener(){
        sensorManager.registerListener(this,accelerometer,100000000)
    }
    fun unsetListener(){
        sensorManager.unregisterListener(this)
    }
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone()
            // Shake detection
            val x = mGravity[0]
            val y = mGravity[1]
            val z = mGravity[2]
            mAccelLast = mAccelCurrent
            mAccelCurrent = (x * x + y * y + z * z).pow(0.5F)
            val delta = mAccelCurrent - mAccelLast
            mAccel = mAccel * 0.9f + delta

            if (mAccel > sensibility) {
                reaction()
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}



class InputManager private constructor() {
    private lateinit var inCh:AccelerometerManager
    lateinit var httpIn:Javalin
    private var state = -1
    companion object{
        var instance = InputManager()
        fun init(sensorM:SensorManager, sensibility: Int) {
            //un listener per il trigger all'input da schermo
            instance.inCh = AccelerometerManager(sensorM,sensibility){println("shake")}
            instance.inCh.setReaction { si() }
            instance.inCh.setListener()
        }

        private fun si(){
            instance.inCh.unsetListener()
            OutputManager.instance.speakOut("Associazione richiesta. scuotere per confermare")
            while(OutputManager.instance.isBusy()){}
            Handler(Looper.getMainLooper()).postDelayed({
                if(instance.state > -1){
                    return@postDelayed
                }
                instance.inCh.unsetListener()
                OutputManager.instance.speakOut("Associazione annullata")
                while(OutputManager.instance.isBusy()){}
                instance.inCh.setReaction { si() }
                instance.inCh.setListener()
            },10000)
            instance.inCh.setReaction { S1() }
            instance.inCh.setListener()
        }
        private fun S1(){
            instance.inCh.unsetListener()
            instance.state = 0
            OutputManager.instance.speakOut("Associazione confermata. Lista degli ospiti, se non sei tu scuoti")
            DataManager.instance.askUpdate()
            //c'è veramente bisogno di aspettare dopo ogni parlata? quelli con timer poco dopo sono piu critici
            while(OutputManager.instance.isBusy()){}
            instance.inCh.setReaction { S2() }
            S2()
        }
        private fun S2(){
            instance.inCh.unsetListener()
            val oldS = instance.state
            instance.state = instance.state + 1
            val guests = DataManager.instance.getGuests()
            if(DataManager.instance.getGuests().subList(oldS,guests.size).isEmpty()){
                OutputManager.instance.speakOut("Lista degli ospiti terminata. Annullo")
                while (OutputManager.instance.isBusy()){}
                instance.state = -1
                instance.inCh.setReaction { si() }
                instance.inCh.setListener()
                return
            }

            DataManager.instance.setAssociated(DataManager.instance.getGuests()[oldS])
            OutputManager.instance.speakOut(DataManager.instance.getAssociated()!!.name + " " +DataManager.instance.getAssociated()!!.surname)
            while(OutputManager.instance.isBusy()){}
            Handler(Looper.getMainLooper()).postDelayed({
                if(instance.state != oldS+1){
                    return@postDelayed
                }
                instance.inCh.unsetListener()
                OutputManager.instance.speakOut("Associato a " + DataManager.instance.getAssociated()!!.surname)
                while(OutputManager.instance.isBusy()){}
                instance.inCh.setReaction { si() }
                instance.state = -1
                ss()
            },6000)
            instance.inCh.setListener()
        }
        private fun ss(){
            instance.httpIn = Javalin.create()
            instance.httpIn.get("/api/deassociate"){
                ctx ->
                OutputManager.instance.speakOut("Associazione sciolta. riporre l'oggetto al suo posto. buona giornata")
                Handler(Looper.getMainLooper()).postDelayed({
                    instance.httpIn.stop()
                    while(OutputManager.instance.isBusy()){}
                    instance.inCh.setListener()
                },100)
                ctx.result("y")
            }
            instance.httpIn.start(8080)
            //attiva nuovo thread per posizionamento
        }
    }
}*/