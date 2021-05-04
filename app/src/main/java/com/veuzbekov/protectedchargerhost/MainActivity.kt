package com.veuzbekov.protectedchargerhost

import android.bluetooth.BluetoothAdapter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainActivity : AppCompatActivity() {
    private var communicationManager: CommunicationManager ?= null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val receivedTextView = findViewById<TextView>(R.id.received_text_view)
        communicationManager = CommunicationManager(BluetoothAdapter.getDefaultAdapter())
        with (communicationManager!!){
            init()
            receiveData()
            /*getOutputFlow().onEach {
                receivedTextView.text = it
            }.launchIn(CoroutineScope(Dispatchers.Main))*/
        }
    }
}