package com.gimmatek.hahappy.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.gimmatek.hahappy.R

class RecordHolder (inflater: LayoutInflater, parent: ViewGroup)
    : RecyclerView.ViewHolder(inflater.inflate(R.layout.card_record_item, parent, false)) {
    val uiCardNo: TextView = itemView.findViewById(R.id.tv_cardNo)
    val uiName: TextView = itemView.findViewById(R.id.tv_name)
    val uiTotal: TextView = itemView.findViewById(R.id.tv_total)
    val uiRest: TextView = itemView.findViewById(R.id.tv_rest)
}