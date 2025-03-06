package io.getimpulse.example

import android.content.Intent
import android.widget.Button

class MainActivity : BaseActivity(R.layout.activity_main) {

    private val button by lazy { findViewById<Button>(R.id.show) }

    override fun setupListeners() {
        button.setOnClickListener {
            val intent = Intent(this, VideosActivity::class.java)
            startActivity(intent)
        }
    }
}