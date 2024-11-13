package io.getimpulse.player.sheet

import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.core.Formatter
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.R
import io.getimpulse.player.extension.setFont
import io.getimpulse.player.model.Speed
import io.getimpulse.player.sheet.base.SheetActivity
import io.getimpulse.player.sheet.base.SheetAdapter
import kotlinx.coroutines.launch

internal class SpeedSheet : SheetActivity(R.layout.sheet_speed), SheetAdapter.Listener {

    companion object {
        private const val ExtraContractKey = "contract_key"

        fun createIntent(
            context: Context,
            contractKey: String,
        ): Intent {
            return Intent(context, SpeedSheet::class.java).apply {
                putExtra(ExtraContractKey, contractKey)
            }
        }
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Navigation.getContract<Contract.SpeedContract>(key)
    }
    private val title by lazy {
        requireNotNull(sheet).requireView().findViewById<TextView>(R.id.title)
    }
    private val options by lazy {
        requireNotNull(sheet).requireView().findViewById<RecyclerView>(R.id.options)
    }
    private val adapter by lazy { SheetAdapter(this) }
    private val speeds by lazy { Speed.all() }
    private var selected: Speed? = null

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
        selected = Speed.from(key)
        renderOptions()
        handleClose()
    }

    private fun renderOptions() {
        adapter.render(
            speeds.map {
                SheetAdapter.Row(
                    it.key,
                    Formatter.speed(it),
                    it == selected,
                )
            },
        )
    }

    override fun handleClose() {
        val contract = contract ?: return
        contract.onResult(selected)
        Navigation.finish(this)
    }
}
