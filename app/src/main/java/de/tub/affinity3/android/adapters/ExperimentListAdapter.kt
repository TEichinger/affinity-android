package de.tub.affinity3.android.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.tub.affinity3.android.R
import de.tub.affinity3.android.classes.data.Experiment
import kotlinx.android.synthetic.main.item_experiment.view.*

class ExperimentListAdapter(
    private val deleteClickListener: (Experiment) -> Unit,
    private val exportClickListener: (Experiment) -> Unit
) :
    ListAdapter<Experiment, ExperimentListAdapter.ExperimentViewHolder>(ExperimentDiffCallback()) {
    class ExperimentDiffCallback : DiffUtil.ItemCallback<Experiment>() {
        override fun areItemsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
            return oldItem.logEntries == newItem.logEntries &&
                    oldItem.name == newItem.name
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ExperimentViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ExperimentViewHolder(inflater.inflate(R.layout.item_experiment, parent, false))
    }

    override fun onBindViewHolder(holder: ExperimentViewHolder, position: Int) {
        holder.bind(getItem(position), deleteClickListener, exportClickListener)
    }

    class ExperimentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(
            experiment: Experiment,
            deleteClickListener: (Experiment) -> Unit,
            exportClickListener: (Experiment) -> Unit
        ) {
            itemView.name.text = experiment.name
            itemView.entries.text = "${experiment.logEntries} Logs"
            itemView.download_button.setOnClickListener { exportClickListener(experiment) }
            itemView.delete_button.setOnClickListener { deleteClickListener(experiment) }
        }
    }
}
