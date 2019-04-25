package io.wassa.pixlyticssdksampleapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_menu.*

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        itemManagementButton.setOnClickListener { startActivity(Intent(this, ItemEditingActivity::class.java)) }

        recognitionOnlineButton.setOnClickListener { startActivity(Intent(this, OnlineRecognitionActivity::class.java)) }

        recognitionOfflineButton.setOnClickListener { startActivity(Intent(this, OfflineRecognitionActivity::class.java)) }
    }
}
