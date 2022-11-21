package com.attafitamim.sensor.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.attafitamim.sensor.widgets.card.SensibleImageCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sensibleCard = findViewById<SensibleImageCardView>(R.id.imgCard)
        sensibleCard.setOnClickListener {
            sensibleCard.setImageResource(android.R.drawable.ic_btn_speak_now)
        }
    }
}