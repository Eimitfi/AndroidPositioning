package com.example.mobiletotem.utility

import android.net.wifi.WifiConfiguration

class ConfigurationManager {
    private lateinit var netConfig:WifiConfiguration
    companion object{
        private val instance = ConfigurationManager()
        fun init(netConfig:WifiConfiguration){
            instance.netConfig = netConfig
        }
        fun getNetConfig(): WifiConfiguration {
            return instance.netConfig
        }
    }
}