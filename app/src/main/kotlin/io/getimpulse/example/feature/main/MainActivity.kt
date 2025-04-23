package io.getimpulse.example.feature.main

import android.content.Intent
import android.widget.Button
import io.getimpulse.example.feature.base.BaseActivity
import io.getimpulse.example.R
import io.getimpulse.example.feature.videos.VideosActivity

class MainActivity : BaseActivity(R.layout.activity_main) {

    private val button by lazy { findViewById<Button>(R.id.show) }

    override fun setupListeners() {
        button.setOnClickListener {
            val intent = Intent(this, VideosActivity::class.java)
            startActivity(intent)
        }
    }
}