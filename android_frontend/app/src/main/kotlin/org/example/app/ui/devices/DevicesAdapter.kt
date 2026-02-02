package org.example.app.ui.devices

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.example.app.R
import org.example.app.model.DeviceSummary

class DevicesAdapter(
    private val onConnect: (DeviceSummary) -> Unit
) : RecyclerView.Adapter<DevicesAdapter.VH>() {

    private var items: List<DeviceSummary> = emptyList()

    fun submit(newItems: List<DeviceSummary>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return VH(v, onConnect)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class VH(itemView: View, private val onConnect: (DeviceSummary) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.deviceName)
        private val address: TextView = itemView.findViewById(R.id.deviceAddress)
        private val type: TextView = itemView.findViewById(R.id.deviceType)
        private val btn: Button = itemView.findViewById(R.id.btnConnect)

        fun bind(item: DeviceSummary) {
            name.text = item.name ?: "(Unnamed device)"
            address.text = item.address
            type.text = if (item.isBle) "BLE" else "Classic"
            btn.setOnClickListener { onConnect(item) }
        }
    }
}
