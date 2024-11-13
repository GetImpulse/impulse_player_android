package io.getimpulse.player.base

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.getimpulse.player.core.Logging
import io.getimpulse.player.extension.dpToPx

internal class BaseSheet(
    @LayoutRes private val layoutId: Int,
    private val listener: Listener,
) : BottomSheetDialogFragment() {

    interface Listener {
        fun setupSheet()
        fun setupSheetListeners()
        fun onClose()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.maxWidth = 500.dpToPx() // ViewGroup.LayoutParams.MATCH_PARENT
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bottomSheet = view.parent as View
//        bottomSheet.setBackgroundTintMode(PorterDuff.Mode.CLEAR)
//        bottomSheet.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT))
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        listener.setupSheet()
        listener.setupSheetListeners()
    }

    override fun onCancel(dialog: DialogInterface) {
        Logging.d("Cancel")
    }

    override fun onDismiss(dialog: DialogInterface) {
        Logging.d("Dismiss")
        listener.onClose()
    }
}