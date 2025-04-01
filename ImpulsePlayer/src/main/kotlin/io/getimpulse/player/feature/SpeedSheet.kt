package io.getimpulse.player.feature

import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.core.Formatter
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.base.sheet.SheetActivity
import io.getimpulse.player.feature.base.sheet.SheetAdapter
import io.getimpulse.player.model.Speed
import io.getimpulse.player.util.extension.setFont
import kotlinx.coroutines.launch

internal class SpeedSheet : SheetActivity(R.layout.sheet_speed), SheetAdapter.Listener {

    companion object {
        private const val ExtraContractKey = "contract_key"

        fun createIntent(
            context: Context,
            contract: Contracts.SelectSpeed,
        ): Intent {
            return Intent(context, SpeedSheet::class.java).apply {
                putExtra(ExtraContractKey, contract.key)
            }
        }
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Contracts.get<Contracts.SelectSpeed>(key)
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
        val selected = contract.options.first { it.key == key }
        renderOptions(selected)
        requireSession().onSetSpeed(selected)
        handleClose()
    }

    private fun renderOptions(selected: Speed?) {
        val contract = contract ?: return
        adapter.render(
            contract.options.map {
                SheetAdapter.Row(
                    it.key,
                    null,
                    Formatter.speed(it),
                    it == selected,
                )
            },
        )
    }

    override fun handleClose() {
        Navigation.finish(this)
    }
}
