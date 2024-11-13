package io.getimpulse.player.sheet

import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.R
import io.getimpulse.player.extension.setFont
import io.getimpulse.player.model.VideoQuality
import io.getimpulse.player.sheet.base.SheetActivity
import io.getimpulse.player.sheet.base.SheetAdapter
import kotlinx.coroutines.launch

internal class VideoQualitySheet : SheetActivity(R.layout.sheet_quality), SheetAdapter.Listener {

    companion object {
        private const val ExtraContractKey = "contract_key"

        fun createIntent(
            context: Context,
            contractKey: String,
        ): Intent {
            return Intent(context, VideoQualitySheet::class.java).apply {
                putExtra(ExtraContractKey, contractKey)
            }
        }
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Navigation.getContract<Contract.VideoQualityContract>(key)
    }
    private val title by lazy {
        requireNotNull(sheet).requireView().findViewById<TextView>(R.id.title)
    }
    private val options by lazy {
        requireNotNull(sheet).requireView().findViewById<RecyclerView>(R.id.options)
    }
    private val adapter by lazy { SheetAdapter(this) }
    private var selected: VideoQuality? = null

    override fun setupSheet() {
        val contract = contract ?: return
        selected = contract.selected?.let { contract.options[it] }
        renderOptions()
        options.adapter = adapter
    }

    override fun setupSheetListeners() {
        lifecycleScope.launch {
            ImpulsePlayer.getAppearance().collect {
                title.setFont(it.h3)
            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onSelected(key: Int) {
        val contract = contract ?: return
        selected = contract.options.first { it.hashCode() == key }
        renderOptions()
        handleClose()
    }

    private fun renderOptions() {
        val contract = contract ?: return
        adapter.render(
            contract.options.map {
                SheetAdapter.Row(
                    it.hashCode(),
                    when (it) {
                        VideoQuality.Automatic -> {
                            getString(R.string.quality_automatic)
                        }

                        is VideoQuality.Detected -> {
                            getString(R.string.quality_x_p, it.height)
                        }
                    },
                    it == selected,
                )
            }
        )
    }

    override fun handleClose() {
        val contract = contract ?: return
        contract.onResult(selected)
        Navigation.finish(this)
    }
}
