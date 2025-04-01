package io.getimpulse.player.feature

import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.base.sheet.SheetActivity
import io.getimpulse.player.feature.base.sheet.SheetAdapter
import io.getimpulse.player.model.VideoQuality
import io.getimpulse.player.util.extension.setFont
import kotlinx.coroutines.launch

internal class VideoQualitySheet : SheetActivity(R.layout.sheet_quality), SheetAdapter.Listener {

    companion object {
        private const val ExtraContractKey = "contract_key"

        fun createIntent(
            context: Context,
            contract: Contracts.SelectVideoQuality,
        ): Intent {
            return Intent(context, VideoQualitySheet::class.java).apply {
                putExtra(ExtraContractKey, contract.key)
            }
        }
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Contracts.get<Contracts.SelectVideoQuality>(key)
    }
    private val title by lazy {
        requireNotNull(sheet).requireView().findViewById<TextView>(R.id.title)
    }
    private val options by lazy {
        requireNotNull(sheet).requireView().findViewById<RecyclerView>(R.id.options)
    }
    private val adapter by lazy { SheetAdapter(this, showSelected = true) }

    private fun getSession() = contract?.let { SessionManager.require(it.videoKey) }
    private fun requireSession() = requireNotNull(getSession())

    override fun setupSheet() {
        val contract = contract ?: return
        val selected = contract.options.first { it.key == contract.selectedKey }
        renderOptions(selected)
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
        val selected = contract.options.first { it.hashCode() == key }
        renderOptions(selected)
        requireSession().onSetVideoQuality(selected)
        handleClose()
    }

    private fun renderOptions(selected: VideoQuality) {
        val contract = contract ?: return
        adapter.render(
            contract.options.map {
                SheetAdapter.Row(
                    it.hashCode(),
                    null,
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
        Navigation.finish(this)
    }
}
