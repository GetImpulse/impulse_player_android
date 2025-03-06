package io.getimpulse.player.feature.base

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity

internal abstract class BaseActivity(
    @LayoutRes private val layoutId: Int,
) : AppCompatActivity() {

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        setupView()
        setupViewListeners()
    }

    open fun setupView() {}
    open fun setupViewListeners() {}
}