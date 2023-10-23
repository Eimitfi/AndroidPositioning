package com.example.mobiletotem.utility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class VoiceListener {
    private lateinit var listener: SpeechRecognizer
    private lateinit var speechRecognizerIntent:Intent
    companion object{
        private val instance = VoiceListener()
        fun init(context: Context){
            instance.listener = SpeechRecognizer.createSpeechRecognizer(context)
            instance.speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            instance.speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            //metti locale dafault pure nell outputman
            instance.speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            instance.listener.setRecognitionListener(object : RecognitionListener{
                override fun onReadyForSpeech(params: Bundle?) {
                    return
                }

                override fun onBeginningOfSpeech() {
                    return
                }

                override fun onRmsChanged(rmsdB: Float) {
                    return
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    return
                }

                override fun onEndOfSpeech() {
                    return
                }

                override fun onError(error: Int) {
                    println("$error")
                }

                override fun onResults(results: Bundle?) {
                    //e se null?
                    val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    println(data)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    return
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    return
                }
            }
            )
        }

        fun start(){
            instance.listener.startListening(instance.speechRecognizerIntent)
        }
        fun stop(){
            instance.listener.stopListening()
        }
        fun destroy(){
            instance.listener.destroy()
        }
        fun cancel(){
            instance.listener.cancel()
        }
    }
}