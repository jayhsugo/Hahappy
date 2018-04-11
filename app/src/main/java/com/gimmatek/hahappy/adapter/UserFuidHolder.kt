package com.gimmatek.hahappy.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.gimmatek.hahappy.R

class UserFuidHolder (inflater: LayoutInflater, parent: ViewGroup)
    : RecyclerView.ViewHolder(inflater.inflate(R.layout.card_fuid_item, parent, false)) {
    val uiName: TextView = itemView.findViewById(R.id.tv_name)
}