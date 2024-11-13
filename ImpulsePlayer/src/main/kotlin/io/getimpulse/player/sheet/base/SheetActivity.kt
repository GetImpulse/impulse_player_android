package io.getimpulse.player.sheet.base

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.annotation.LayoutRes
import io.getimpulse.player.core.Logging
import io.getimpulse.player.R
import io.getimpulse.player.base.BaseActivity
import io.getimpulse.player.base.BaseSheet

internal abstract class SheetActivity(
    @LayoutRes private val sheetLayoutId: Int,
) : BaseActivity(R.layout.activity_sheet), BaseSheet.Listener {

    protected var sheet: BaseSheet? = null

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
        Logging.d("Finish")
        sheet = null
        handleClose()
    }
}