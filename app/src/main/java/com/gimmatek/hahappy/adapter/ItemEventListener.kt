package com.gimmatek.hahappy.adapter

import com.gimmatek.hahappy.model.Schedule

interface ItemEventListener {
    fun onItemSelect(cardNo: Int, scheduleId: String)
}