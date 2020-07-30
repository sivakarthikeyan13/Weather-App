package com.siva1312.weatherapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class HourRecyclerAdapter(
    val itemList: ArrayList<HourlyPrediction>
) : RecyclerView.Adapter<HourRecyclerAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val txtTemp: TextView = view.findViewById(R.id.txtTemp)
        val imgIcon: ImageView = view.findViewById(R.id.imgIcon)
        val llRecyclerView: LinearLayout = view.findViewById(R.id.llRecyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_single_hour, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val predict = itemList[position]
        holder.txtTime.text = predict.time
        holder.txtTemp.text = predict.temp
        val imageUri = "http://openweathermap.org/img/wn/${predict.icon}@4x.png"
        Picasso.get().load(imageUri).error(R.drawable.ic_sunrise)
            .into(holder.imgIcon)

    }
}