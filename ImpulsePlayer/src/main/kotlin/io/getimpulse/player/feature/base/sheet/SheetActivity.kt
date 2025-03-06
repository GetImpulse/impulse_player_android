package io.getimpulse.player.feature.base.sheet

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.annotation.LayoutRes
import io.getimpulse.player.util.Logging
import io.getimpulse.player.R
import io.getimpulse.player.feature.base.BaseActivity
import io.getimpulse.player.feature.base.BaseSheet

internal abstract class SheetActivity(
    @LayoutRes private val sheetLayoutId: Int,
) : BaseActivity(R.layout.activity_sheet), BaseSheet.Listener {

    protected var sheet: BaseSheet? = null
    private var closed = false

    abstract fun handleClose()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        super.onCreate(savedInstanceState)
    }

    override fun setupView() {
        onBackPressedDispatcher.addCallback {
            handleClose()
        }

        sheet = BaseSheet(sheetLayoutId, this).apply {
            show(supportFragmentManager, "sheet")
        }
    }

    override fun onClose() {
        if (closed) return
        closed = true
        Logging.d("onClose")
        sheet = null
        handleClose()
    }
}