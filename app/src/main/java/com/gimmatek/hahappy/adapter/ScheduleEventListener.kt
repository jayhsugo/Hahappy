package com.gimmatek.hahappy.adapter

import com.gimmatek.hahappy.model.Schedule

interface ScheduleEventListener {
    fun onScheduleSelect(scheduleId: String)
}