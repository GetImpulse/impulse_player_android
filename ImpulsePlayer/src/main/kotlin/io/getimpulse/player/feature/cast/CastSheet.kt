package io.getimpulse.player.feature.cast

import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import io.getimpulse.player.ImpulsePlayer
import io.getimpulse.player.R
import io.getimpulse.player.core.Contracts
import io.getimpulse.player.core.Navigation
import io.getimpulse.player.core.SessionManager
import io.getimpulse.player.feature.base.sheet.SheetActivity
import io.getimpulse.player.feature.base.sheet.SheetAdapter
import io.getimpulse.player.util.Logging
import io.getimpulse.player.util.extension.setFont
import kotlinx.coroutines.launch

internal class CastSheet : SheetActivity(R.layout.sheet_cast), SheetAdapter.Listener {

    companion object {
        private const val ExtraContractKey = "contract_key"

        fun createIntent(
            context: Context,
            contract: Contracts.SelectCast,
        ): Intent {
            return Intent(context, CastSheet::class.java).apply {
                putExtra(ExtraContractKey, contract.key)
            }
        }
    }

    private val contract by lazy {
        val key = intent.getStringExtra(ExtraContractKey) ?: return@lazy null
        Contracts.get<Contracts.SelectCast>(key)
    }
    private val title by lazy {
        requireNotNull(sheet).requireView().findViewById<TextView>(R.id.title)
    }
    private val options by lazy {
        requireNotNull(sheet).requireView().findViewById<RecyclerView>(R.id.options)
    }
    private val loader by lazy {
        requireNotNull(sheet).requireView().findViewById<RecyclerView>(R.id.loader)
    }
    private val adapter by lazy { SheetAdapter(this, showSelected = false) }

    private fun getSession() = contract?.let { SessionManager.require(it.videoKey) }
    private fun requireSession() = requireNotNull(getSession())

    override fun setupSheet() {
        options.adapter = adapter
        CastManager.attach(this)
    }

    override fun setupSheetListeners() {
        lifecycleScope.launch {
            ImpulsePlayer.getAppearance().collect {
                title.setFont(it.h3)
            }
            adapter.notifyDataSetChanged()
        }
        lifecycleScope.launch {
            CastManager.getRoutes().collect { routes ->
                renderOptions(routes)
            }
        }
    }

    override fun onSelected(key: Int) {
        val devices = CastManager.getRoutes().value
        val selected = devices.first { it.id.hashCode() == key }
        renderOptions(devices)
        CastManager.select(
            selected,
            requireSession().getVideo().value,
        )
        requireSession().onPause()
        onClose()
    }

    private fun renderOptions(devices: List<CastDisplay.Route>) {
        adapter.render(
            devices.map {
                SheetAdapter.Row(
                    it.id.hashCode(),
                    getIcon(it),
                    getName(it),
                    CastManager.isSelected(it.id),
                )
            })
    }

    @DrawableRes
    private fun getIcon(route: CastDisplay.Route): Int {
        return if (CastManager.isDefault(route.id)) {
            R.drawable.ic_cast_phone
        } else {
            when (route.model) {
                "Chromecast" -> R.drawable.ic_cast_chromecast_1
                "Chromecast Ultra" -> R.drawable.ic_cast_chromecast_2
                "Google TV Streamer" -> R.drawable.ic_cast_tv
                else -> R.drawable.ic_cast_tv
            }
        }
    }

    private fun getName(route: CastDisplay.Route): String {
        return if (CastManager.isDefault(route.id)) {
            getString(R.string.cast_this_device)
        } else {
            route.name
        }
    }

    override fun handleClose() {
        Logging.d("HandleClose")
        CastManager.detach()
        Navigation.finish(this)
    }
}
