package com.example.studentcalendar.horariosemanal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentcalendar.R

class HourAdapter(private val hours: List<String>) : RecyclerView.Adapter<HourAdapter.HourViewHolder>() {

    class HourViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHour: TextView = view.findViewById(R.id.tvHour)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hour_label, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        holder.tvHour.text = hours[position]
    }
    override fun getItemCount(): Int = hours.size
}