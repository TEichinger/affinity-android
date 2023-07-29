package de.tub.affinity3.android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.User
import kotlinx.android.synthetic.main.item_nearby_device.view.textDescription
import kotlinx.android.synthetic.main.item_nearby_device.view.textName

class NearbyDevicesAdapter(
    private var users: List<User>
) : RecyclerView.Adapter<NearbyDevicesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.view.textName.text = user.name
        holder.view.textDescription.text = user.deviceName

        with(holder.view) {
            tag = user
        }
    }

    fun replaceData(newData: List<User>) {
        users = newData
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = users.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)
}
