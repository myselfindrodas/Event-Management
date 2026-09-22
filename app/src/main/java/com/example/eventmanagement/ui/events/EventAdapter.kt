package com.example.eventmanagement.ui.events

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.eventmanagement.R
import com.example.eventmanagement.data.model.Event
import com.example.eventmanagement.databinding.ItemEventBinding
import com.example.eventmanagement.util.DateUtils
import java.util.Date

class EventAdapter(
    private val onClick: (Event) -> Unit,
    private val onDelete: (Event) -> Unit
) : ListAdapter<Event, EventAdapter.EventViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            val context = binding.root.context
            val date = event.dateTime.toDate()
            val upcoming = !date.before(Date())

            binding.tvDay.text = DateUtils.dayOfMonth(date)
            binding.tvMonth.text = DateUtils.monthShort(date)
            binding.tvTitle.text = event.title

            binding.tvDescription.text = event.description
            binding.tvDescription.isVisible = event.description.isNotBlank()

            binding.tvDateTime.text = DateUtils.format(event.dateTime)

            binding.tvLocation.text = event.location
            binding.tvLocation.isVisible = event.location.isNotBlank()

            binding.tvStatus.text = context.getString(
                if (upcoming) R.string.filter_upcoming else R.string.filter_past
            ).uppercase()
            tintStatusPill(upcoming)
            tintDateBlock(upcoming)

            binding.root.setOnClickListener { onClick(event) }
            binding.btnEdit.setOnClickListener { onClick(event) }
            binding.btnDelete.setOnClickListener { onDelete(event) }
        }

        private fun tintStatusPill(upcoming: Boolean) {
            val context = binding.root.context
            val background = if (upcoming) R.color.success_container else R.color.warning_container
            val foreground = if (upcoming) R.color.success else R.color.warning

            binding.tvStatus.background?.mutate()?.setTint(
                ContextCompat.getColor(context, background)
            )
            binding.tvStatus.setTextColor(ContextCompat.getColor(context, foreground))
        }

        private fun tintDateBlock(upcoming: Boolean) {
            val context = binding.root.context
            val block = binding.dateBlock.background?.mutate() as? GradientDrawable ?: return

            if (upcoming) {
                block.colors = intArrayOf(
                    ContextCompat.getColor(context, R.color.gradient_brand_start),
                    ContextCompat.getColor(context, R.color.gradient_brand_mid)
                )
                binding.tvDay.setTextColor(ContextCompat.getColor(context, R.color.on_brand))
                binding.tvMonth.setTextColor(ContextCompat.getColor(context, R.color.on_brand_muted))
            } else {
                val muted = ContextCompat.getColor(context, R.color.surface_container_high)
                block.colors = intArrayOf(muted, muted)
                binding.tvDay.setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
                binding.tvMonth.setTextColor(ContextCompat.getColor(context, R.color.outline))
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Event>() {
            override fun areItemsTheSame(oldItem: Event, newItem: Event): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Event, newItem: Event): Boolean =
                oldItem == newItem
        }
    }
}
