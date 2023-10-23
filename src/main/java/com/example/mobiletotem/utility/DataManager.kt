package com.example.mobiletotem.utility

import com.example.mobiletotem.datatypes.Registration

class DataManager {
    private val ID:Int = 1
    private lateinit var associated:Registration
    companion object{
        private val instance = DataManager()
        fun init(associate:Registration) {
            instance.associated = associate
        }
        fun stop(){

        }
        fun getID():Int{
            return instance.ID
        }
    }

    fun getAssociated():Registration{
        return associated
    }

}