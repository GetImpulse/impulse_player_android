package io.getimpulse.example.feature.base

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity(
    @LayoutRes private val layoutId: Int
) : AppCompatActivity() {

    open fun setupView() {}
    open fun setupViewModel() {}
    open fun setupListeners() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)

        setupView()
        setupViewModel()
        setupListeners()
    }
}