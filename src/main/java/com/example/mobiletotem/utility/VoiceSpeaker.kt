package com.example.mobiletotem.utility

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceSpeaker private constructor(){
    private lateinit var speaker:TextToSpeech
    companion object{
         private val instance = VoiceSpeaker()
        fun init(context: Context){
            instance.speaker = TextToSpeech(context){
                status ->
                if(status != TextToSpeech.SUCCESS){
                    throw Exception("Could not initialize textToSpeech.")
                }
                instance.speaker.language = Locale.getDefault()
                speakOut("Avvio")
            }
        }
        fun isBusy(): Boolean {
            return instance.speaker.isSpeaking
        }
        fun speakOut(text:String){
            instance.speaker.speak(text,TextToSpeech.QUEUE_ADD,null,"")
        }
        fun deallocate(){
            instance.speaker.stop()
            instance.speaker.shutdown()
        }
    }
}
