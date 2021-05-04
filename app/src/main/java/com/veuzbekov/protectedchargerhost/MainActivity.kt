package com.veuzbekov.protectedchargerhost

import android.bluetooth.BluetoothAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    private var communicationManager: CommunicationManager ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        communicationManager = CommunicationManager(BluetoothAdapter.getDefaultAdapter())
        communicationManager!!.init()
        communicationManager!!.receiveData()
    }
}